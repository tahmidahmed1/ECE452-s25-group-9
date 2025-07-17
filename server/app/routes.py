from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, WebSocket, WebSocketDisconnect
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta
import logging
from typing import List, Dict
from fastapi.responses import JSONResponse
from sqlalchemy import or_
import math
from sqlalchemy import func

from .database import get_db
from .models import User, UserType, Sex, Event, Message, Badge, user_badges, user_subscriptions
from .schemas import (
    UserCreate, User as UserSchema, Token, OnboardingStepOne, 
    OnboardingComplete, ProfilePictureUploadResponse, EventSchema,
    UserUpdate, EventCreate,
    MessageCreate, MessageOut,
    ProfileBannerUploadResponse,
    LeaderboardResponse, LeaderboardEntry,
    Badge as BadgeSchema, UserBadge, BadgeCheckResponse, BadgeAchievement,
    SubscriptionCreate, SubscriptionResponse, SubscriptionStatus,
    UserSubscriptionsResponse, OrganizerWithSubscriptionStatus,
)
from .auth import (
    authenticate_user, create_access_token, get_password_hash, 
    get_current_active_user, ACCESS_TOKEN_EXPIRE_MINUTES
)
from .storage import storage_service

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter()

# -------------------- Banner Upload --------------------

@router.post("/upload-profile-banner", response_model=ProfileBannerUploadResponse)
async def upload_profile_banner(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    logger.info(
        f"Profile banner upload request from user: {current_user.username}, filename: {file.filename}, content_type: {file.content_type}")

    # Delete old banner if exists
    if current_user.banner_url:
        storage_service.delete_profile_picture(current_user.banner_url)

    # Upload new banner
    banner_url = await storage_service.upload_profile_banner(file, current_user.id)

    # Persist
    current_user.banner_url = banner_url
    db.commit()
    db.refresh(current_user)

    logger.info(f"Profile banner uploaded successfully for user: {current_user.username}")
    return {
        "banner_url": banner_url,
        "message": "Profile banner uploaded successfully",
    }

# Connection manager
class ConnectionManager:
    def __init__(self):
        self.active: Dict[int, List[WebSocket]] = {}

    async def connect(self, room: int, websocket: WebSocket):
        await websocket.accept()
        self.active.setdefault(room, []).append(websocket)

    def disconnect(self, room: int, websocket: WebSocket):
        self.active.get(room, []).remove(websocket)

    async def broadcast(self, room: int, message: dict):
        for ws in list(self.active.get(room, [])):
            await ws.send_json(message)

manager = ConnectionManager()

@router.post("/register", response_model=Token)
def register_user(user: UserCreate, db: Session = Depends(get_db)):
    logger.info(f"Registration request received for username: {user.username}, email: {user.email}")
    
    # Check if user already exists
    dup_user = db.query(User).filter(or_(User.username == user.username, User.email == user.email)).first()
    if dup_user:
        message = "Username already registered" if dup_user.username == user.username else "Email already registered"
        logger.warning(f"Registration failed: {message}")
        return JSONResponse(status_code=409, content={"success": False, "message": message})
    
    # Create new user (onboarding_completed defaults to False)
    hashed_password = get_password_hash(user.password)
    db_user = User(
        username=user.username,
        email=user.email,
        hashed_password=hashed_password
    )
    
    # Check if this is a dev user and auto-complete onboarding
    if user.username.startswith("dev_"):
        logger.info(f"Dev user detected: {user.username}, auto-completing onboarding")
        
        # Extract user type from username (e.g., "dev_volunteer_123456")
        parts = user.username.split("_")
        if len(parts) >= 2:
            user_type_str = parts[1].upper()
            try:
                if user_type_str == "VOLUNTEER":
                    db_user.user_type = UserType.VOLUNTEER
                    db_user.full_name = f"Dev Volunteer {parts[2] if len(parts) > 2 else 'User'}"
                    db_user.phone = f"+1-555-DEV-{parts[2] if len(parts) > 2 else '123'}"
                    db_user.sex = Sex.PREFER_NOT_TO_SAY
                    db_user.description = "Auto-generated dev volunteer account"
                    db_user.skills = ["testing", "development", "community"]
                    db_user.age = 25
                    db_user.emergency_contact_name = "Dev Emergency Contact"
                    db_user.emergency_contact_phone = "+1-555-911-DEV"
                    db_user.location_area = "DevVille"
                    db_user.has_drivers_license = True
                    db_user.disabilities = None
                    # Add some random karma points for dev users
                    import random
                    db_user.karma_points = random.randint(50, 1500)
                elif user_type_str == "ORGANIZER":
                    db_user.user_type = UserType.ORGANIZER
                    db_user.full_name = f"Dev Organizer {parts[2] if len(parts) > 2 else 'User'}"
                    db_user.phone = f"+1-555-DEV-{parts[2] if len(parts) > 2 else '123'}"
                    db_user.organization_name = f"Dev Organization {parts[2] if len(parts) > 2 else '123'}"
                    db_user.banner_url = "https://placehold.co/600x200?text=Dev+Banner"

                    
                # Mark onboarding as completed for dev users
                db_user.onboarding_completed = True
                logger.info(f"Dev user {user.username} onboarding auto-completed with type: {user_type_str}")
                
            except Exception as e:
                logger.warning(f"Failed to auto-complete onboarding for dev user {user.username}: {e}")
    
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    logger.info(f"User created successfully: {user.username}")
    
    # Generate token for the new user
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": db_user.username}, expires_delta=access_token_expires
    )
    logger.info(f"Token generated for user: {user.username}")
    return {"access_token": access_token, "token_type": "bearer"}

@router.post("/token", response_model=Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    logger.info(f"Login request received for username: {form_data.username}")
    
    user = authenticate_user(db, form_data.username, form_data.password)
    if not user:
        logger.warning(f"Login failed for username: {form_data.username}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    logger.info(f"Login successful for username: {form_data.username}")
    return {"access_token": access_token, "token_type": "bearer"}

@router.get("/users/me", response_model=UserSchema)
def read_users_me(current_user: User = Depends(get_current_active_user)):
    logger.info(f"User info request for: {current_user.username}")
    return current_user

# Profile picture upload endpoint
@router.post("/upload-profile-picture", response_model=ProfilePictureUploadResponse)
async def upload_profile_picture(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    # Delete old profile picture if exists
    if current_user.profile_picture_url:
        storage_service.delete_profile_picture(current_user.profile_picture_url)
    
    # Upload new profile picture
    profile_picture_url = await storage_service.upload_profile_picture(file, current_user.id)
    
    # Update user's profile picture URL in database
    current_user.profile_picture_url = profile_picture_url
    db.commit()
    db.refresh(current_user)
    
    return {
        "profile_picture_url": profile_picture_url,
        "message": "Profile picture uploaded successfully"
    }

# Onboarding endpoints
@router.post("/onboarding/step-one")
def complete_onboarding_step_one(
    step_data: OnboardingStepOne, 
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    logger.info(f"Onboarding step 1 for user {current_user.username}: {step_data.user_type}")
    
    # Normalize to database enum label (uppercase)
    try:
        current_user.user_type = UserType[step_data.user_type.value.upper()]
    except KeyError:
        # Fallback to direct assignment (in case enum labels already match)
        current_user.user_type = step_data.user_type
    
    db.commit()
    db.refresh(current_user)
    
    return {"message": "Step one completed", "user_type": step_data.user_type}

@router.post("/onboarding/complete")
def complete_onboarding(
    onboarding_data: OnboardingComplete,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    logger.info(f"Completing onboarding for user {current_user.username}")
    
    # Update user profile
    # Only set full_name if provided or user is not ORGANIZER
    if onboarding_data.full_name and current_user.user_type != UserType.ORGANIZER:
        current_user.full_name = onboarding_data.full_name
    current_user.phone = onboarding_data.phone
    current_user.onboarding_completed = True
    
    if onboarding_data.organization_name:
        current_user.organization_name = onboarding_data.organization_name
    
    db.commit()
    db.refresh(current_user)
    
    logger.info(f"Onboarding completed for user {current_user.username}")
    return {"message": "Onboarding completed successfully"}

@router.post("/onboarding/volunteer-complete")
def complete_volunteer_onboarding(
    onboarding_data: OnboardingComplete,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    logger.info(f"Completing volunteer onboarding for user {current_user.username}")
    
    # Update user profile with all volunteer fields
    current_user.full_name = onboarding_data.full_name
    current_user.phone = onboarding_data.phone
    current_user.onboarding_completed = True
    
    # Volunteer-specific fields
    if onboarding_data.sex:
        try:
            current_user.sex = Sex[onboarding_data.sex.value.upper()]
        except KeyError:
            current_user.sex = onboarding_data.sex  # fallback
    if onboarding_data.description:
        current_user.description = onboarding_data.description
    if onboarding_data.skills:
        current_user.skills = onboarding_data.skills
    if onboarding_data.age:
        current_user.age = onboarding_data.age
    if onboarding_data.emergency_contact_name:
        current_user.emergency_contact_name = onboarding_data.emergency_contact_name
    if onboarding_data.emergency_contact_phone:
        current_user.emergency_contact_phone = onboarding_data.emergency_contact_phone
    if onboarding_data.location_area:
        current_user.location_area = onboarding_data.location_area
    if onboarding_data.has_drivers_license is not None:
        current_user.has_drivers_license = onboarding_data.has_drivers_license
    if onboarding_data.disabilities:
        current_user.disabilities = onboarding_data.disabilities
    
    db.commit()
    db.refresh(current_user)
    
    logger.info(f"Volunteer onboarding completed for user {current_user.username}")
    return {"message": "Volunteer onboarding completed successfully"}

# ------------------ Event Endpoints ------------------

# Helper: calculate distance between two lat/lon points (Haversine)
def _haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6371.0  # Earth radius km
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)

    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

@router.get("/events", response_model=List[EventSchema])
def list_events_nearby(
    lat: float | None = None,
    lon: float | None = None,
    radius_km: float = 50.0,
    category: str | None = None,
    only_available: bool = False,
    almost_full: bool = False,
    min_karma_points: int = 1,
    max_karma_points: int = 200,
    date_filter: str | None = None,
    db: Session = Depends(get_db),
):
    """Return all events, filtered by various criteria."""
    query = db.query(Event)
    
    # Category filter
    if category:
        try:
            from .models import OpportunityCategory
            category_enum = OpportunityCategory[category.upper()]
            query = query.filter(Event.category == category_enum)
        except KeyError:
            pass  # Invalid category, ignore filter
    
    # Availability filters
    if only_available:
        query = query.filter(Event.current_volunteers < Event.max_volunteers)
    elif almost_full:
        # Almost full: 80% or more capacity but not full
        query = query.filter(
            Event.current_volunteers >= (Event.max_volunteers * 0.8),
            Event.current_volunteers < Event.max_volunteers
        )
    
    # Karma points filter
    query = query.filter(
        Event.karma_points >= min_karma_points,
        Event.karma_points <= max_karma_points
    )
    
    # Date filter (basic implementation)
    if date_filter:
        from datetime import datetime, timedelta
        today = datetime.now().date()
        
        if date_filter == "today":
            query = query.filter(Event.date == today.strftime("%Y-%m-%d"))
        elif date_filter == "this_week":
            start_week = today - timedelta(days=today.weekday())
            end_week = start_week + timedelta(days=6)
            query = query.filter(
                Event.date >= start_week.strftime("%Y-%m-%d"),
                Event.date <= end_week.strftime("%Y-%m-%d")
            )
        elif date_filter == "this_month":
            start_month = today.replace(day=1)
            if today.month == 12:
                end_month = today.replace(year=today.year + 1, month=1, day=1) - timedelta(days=1)
            else:
                end_month = today.replace(month=today.month + 1, day=1) - timedelta(days=1)
            query = query.filter(
                Event.date >= start_month.strftime("%Y-%m-%d"),
                Event.date <= end_month.strftime("%Y-%m-%d")
            )
    
    events = query.all()
    
    # Location filter (applied after database query)
    if lat is not None and lon is not None:
        events = [
            e for e in events if e.latitude is not None and e.longitude is not None and _haversine_km(lat, lon, e.latitude, e.longitude) <= radius_km
        ]
    
    return events

@router.post("/events", response_model=EventSchema)
def create_event(event: EventCreate, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    if current_user.user_type != UserType.ORGANIZER:
        raise HTTPException(status_code=403, detail="Only organizers can create events")
    event_data = event.dict()
    if event_data.get("current_volunteers") is None:
        event_data["current_volunteers"] = 0
    new_event = Event(**event_data, organizer_id=current_user.id)
    db.add(new_event)
    db.commit()
    db.refresh(new_event)
    return new_event

@router.get("/events/{event_id}", response_model=EventSchema)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    return event

@router.patch("/events/{event_id}", response_model=EventSchema)
async def update_event(event_id: int, payload: EventCreate, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")
    for k, v in payload.dict(exclude_unset=True).items():
        setattr(event, k, v)
    db.commit()
    db.refresh(event)
    return event

@router.delete("/events/{event_id}")
async def delete_event(event_id: int, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")
    db.delete(event)
    db.commit()
    return {"detail": "deleted"}

@router.get("/organizers/{organizer_id}/events", response_model=List[EventSchema])
def list_events_by_organizer(organizer_id: int, db: Session = Depends(get_db)):
    return db.query(Event).filter(Event.organizer_id == organizer_id).all()

@router.websocket("/ws/chat/{room_id}")
async def chat_endpoint(websocket: WebSocket, room_id: int, db: Session = Depends(get_db)):
    await manager.connect(room_id, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            # data expected: {"sender_id":.., "receiver_id":.., "content":..}
            try:
                payload = MessageCreate(**data)
            except Exception as e:
                await websocket.send_json({"error": str(e)})
                continue
            msg = Message(**payload.dict())
            db.add(msg)
            db.commit()
            db.refresh(msg)
            await manager.broadcast(room_id, MessageOut.from_orm(msg).dict())
    except WebSocketDisconnect:
        manager.disconnect(room_id, websocket)

# ------------------ Profile Edit ------------------

@router.put("/users/me", response_model=UserSchema)
def update_user_profile(
    updates: UserUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Update fields of the currently authenticated user. Only provided fields are updated."""

    logger.info(f"Profile update requested by {current_user.username}")

    update_dict = updates.dict(exclude_unset=True)

    for field, value in update_dict.items():
        # Convert Enum values to correct DB enums if necessary
        if field == "sex" and value is not None:
            try:
                value = Sex[value.upper()]
            except KeyError:
                pass


        setattr(current_user, field, value)

    db.commit()
    db.refresh(current_user)

    logger.info(f"Profile updated successfully for {current_user.username}")
    return current_user 

# Event image upload

@router.post("/events/{event_id}/upload-image", response_model=EventSchema)
async def upload_event_image(
    event_id: int,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Upload an image for an event and return the updated event."""
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    # Upload to object storage
    url = await storage_service.upload_event_image(file, event_id)

    # Save URL
    event.image_url = url
    db.commit()
    db.refresh(event)

    return event 

# ------------------ Organizer search ------------------

@router.get("/organizers", response_model=list[UserSchema])
def list_organizers(q: str | None = None, db: Session = Depends(get_db)):
    """Return organizers optionally filtered by query string in username/full_name/organization."""
    query = db.query(User).filter(User.user_type == UserType.ORGANIZER, User.onboarding_completed == True)
    if q:
        ilike = f"%{q.lower()}%"
        query = query.filter(
            (User.username.ilike(ilike)) | (User.full_name.ilike(ilike)) | (User.organization_name.ilike(ilike))
        )
    return query.all()

# ------------------ Subscription Endpoints ------------------

@router.post("/subscriptions", response_model=SubscriptionResponse)
def subscribe_to_organizer(
    subscription: SubscriptionCreate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Subscribe to an organizer"""
    # Check if organizer exists and is actually an organizer
    organizer = db.query(User).filter(
        User.id == subscription.organizer_id,
        User.user_type == UserType.ORGANIZER,
        User.onboarding_completed == True
    ).first()
    
    if not organizer:
        raise HTTPException(status_code=404, detail="Organizer not found")
    
    # Check if user is trying to subscribe to themselves
    if current_user.id == subscription.organizer_id:
        raise HTTPException(status_code=400, detail="Cannot subscribe to yourself")
    
    # Check if already subscribed
    existing_subscription = db.query(user_subscriptions).filter(
        user_subscriptions.c.subscriber_id == current_user.id,
        user_subscriptions.c.organizer_id == subscription.organizer_id
    ).first()
    
    if existing_subscription:
        return SubscriptionResponse(
            success=True,
            message="Already subscribed to this organizer",
            is_subscribed=True
        )
    
    # Create subscription
    db.execute(user_subscriptions.insert().values(
        subscriber_id=current_user.id,
        organizer_id=subscription.organizer_id
    ))
    db.commit()
    
    return SubscriptionResponse(
        success=True,
        message=f"Successfully subscribed to {organizer.organization_name or organizer.full_name}",
        is_subscribed=True
    )

@router.delete("/subscriptions/{organizer_id}", response_model=SubscriptionResponse)
def unsubscribe_from_organizer(
    organizer_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Unsubscribe from an organizer"""
    # Check if subscription exists
    existing_subscription = db.query(user_subscriptions).filter(
        user_subscriptions.c.subscriber_id == current_user.id,
        user_subscriptions.c.organizer_id == organizer_id
    ).first()
    
    if not existing_subscription:
        return SubscriptionResponse(
            success=True,
            message="Not subscribed to this organizer",
            is_subscribed=False
        )
    
    # Remove subscription
    db.execute(user_subscriptions.delete().where(
        user_subscriptions.c.subscriber_id == current_user.id,
        user_subscriptions.c.organizer_id == organizer_id
    ))
    db.commit()
    
    return SubscriptionResponse(
        success=True,
        message="Successfully unsubscribed",
        is_subscribed=False
    )

@router.get("/subscriptions", response_model=UserSubscriptionsResponse)
def get_user_subscriptions(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get all organizers the current user is subscribed to"""
    subscriptions = db.query(User).join(
        user_subscriptions,
        User.id == user_subscriptions.c.organizer_id
    ).filter(
        user_subscriptions.c.subscriber_id == current_user.id,
        User.user_type == UserType.ORGANIZER
    ).all()
    
    return UserSubscriptionsResponse(subscriptions=subscriptions)

@router.get("/subscriptions/status/{organizer_id}", response_model=SubscriptionStatus)
def get_subscription_status(
    organizer_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Check if current user is subscribed to a specific organizer"""
    subscription = db.query(user_subscriptions).filter(
        user_subscriptions.c.subscriber_id == current_user.id,
        user_subscriptions.c.organizer_id == organizer_id
    ).first()
    
    return SubscriptionStatus(
        organizer_id=organizer_id,
        is_subscribed=subscription is not None,
        subscribed_at=subscription.subscribed_at if subscription else None
    )

@router.get("/organizers/with-subscription-status", response_model=list[OrganizerWithSubscriptionStatus])
def list_organizers_with_subscription_status(
    q: str | None = None,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Return organizers with subscription status for the current user"""
    query = db.query(User).filter(User.user_type == UserType.ORGANIZER, User.onboarding_completed == True)
    if q:
        ilike = f"%{q.lower()}%"
        query = query.filter(
            (User.username.ilike(ilike)) | (User.full_name.ilike(ilike)) | (User.organization_name.ilike(ilike))
        )
    
    organizers = query.all()
    
    # Get subscription status for each organizer
    result = []
    for organizer in organizers:
        # Check if current user is subscribed to this organizer
        subscription = db.query(user_subscriptions).filter(
            user_subscriptions.c.subscriber_id == current_user.id,
            user_subscriptions.c.organizer_id == organizer.id
        ).first()
        
        # Count total subscribers for this organizer
        subscriber_count = db.query(user_subscriptions).filter(
            user_subscriptions.c.organizer_id == organizer.id
        ).count()
        
        # Create response with subscription status
        organizer_data = OrganizerWithSubscriptionStatus.from_orm(organizer)
        organizer_data.is_subscribed = subscription is not None
        organizer_data.subscriber_count = subscriber_count
        result.append(organizer_data)
    
    return result

# ------------------ Messaging Endpoints ------------------

@router.get("/conversations", response_model=list[dict])
def get_conversations(current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    """Get all conversations for the current user"""
    # Get all unique users that the current user has exchanged messages with
    sent_to = db.query(Message.receiver_id).filter(Message.sender_id == current_user.id).distinct().subquery()
    received_from = db.query(Message.sender_id).filter(Message.receiver_id == current_user.id).distinct().subquery()
    
    # Get users from both sent and received messages
    conversation_user_ids = db.query(User.id).filter(
        or_(User.id.in_(sent_to), User.id.in_(received_from))
    ).all()
    
    conversations = []
    for user_id_tuple in conversation_user_ids:
        other_user_id = user_id_tuple[0]
        if other_user_id == current_user.id:
            continue
            
        # Get the other user
        other_user = db.query(User).filter(User.id == other_user_id).first()
        if not other_user:
            continue
            
        # Get the last message in this conversation
        last_message = db.query(Message).filter(
            ((Message.sender_id == current_user.id) & (Message.receiver_id == other_user_id)) |
            ((Message.sender_id == other_user_id) & (Message.receiver_id == current_user.id))
        ).order_by(Message.created_at.desc()).first()
        
        if last_message:
            conversations.append({
                "id": str(other_user_id),
                "title": other_user.organization_name or other_user.full_name or other_user.username,
                "subtitle": f"Chat with {other_user.username}",
                "lastMessage": last_message.content,
                "timestamp": last_message.created_at.strftime("%H:%M"),
                "unreadCount": 0,  # TODO: Implement unread count
                "isStarred": False,  # TODO: Implement starred conversations
                "participantCount": 2,
                "otherUserId": other_user_id
            })
    
    # Sort by last message timestamp
    conversations.sort(key=lambda x: x["timestamp"], reverse=True)
    return conversations

@router.get("/messages/{other_user_id}", response_model=list[MessageOut])
def get_messages_with_user(other_user_id: int, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    msgs = db.query(Message).filter(
        ((Message.sender_id == current_user.id) & (Message.receiver_id == other_user_id)) |
        ((Message.sender_id == other_user_id) & (Message.receiver_id == current_user.id))
    ).order_by(Message.created_at).all()
    return msgs

@router.post("/messages", response_model=MessageOut)
def send_message(payload: MessageCreate, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    # Create message with current user as sender
    msg = Message(
        sender_id=current_user.id,
        receiver_id=payload.receiver_id,
        content=payload.content
    )
    db.add(msg)
    db.commit()
    db.refresh(msg)
    return msg

# ------------------ Leaderboard Endpoints ------------------

@router.get("/leaderboard", response_model=LeaderboardResponse)
def get_leaderboard(
    page: int = 1,
    page_size: int = 20,
    db: Session = Depends(get_db),
):
    """Get paginated leaderboard sorted by karma points"""
    # Validate pagination parameters
    if page < 1:
        page = 1
    if page_size < 1 or page_size > 100:
        page_size = 20
    
    # Get total count of volunteers (only volunteers have karma points that matter)
    total_query = db.query(User).filter(
        User.user_type == UserType.VOLUNTEER,
        User.onboarding_completed == True
    )
    total_entries = total_query.count()
    
    # Calculate pagination
    total_pages = (total_entries + page_size - 1) // page_size
    offset = (page - 1) * page_size
    
    # Get paginated results ordered by karma points descending
    users = total_query.order_by(User.karma_points.desc()).offset(offset).limit(page_size).all()
    
    # Create leaderboard entries with ranks
    entries = []
    for index, user in enumerate(users):
        rank = offset + index + 1
        entry = LeaderboardEntry(
            id=user.id,
            username=user.username,
            full_name=user.full_name,
            karma_points=user.karma_points,
            profile_picture_url=user.profile_picture_url,
            user_type=user.user_type,
            rank=rank
        )
        entries.append(entry)
    
    return LeaderboardResponse(
        entries=entries,
        page=page,
        page_size=page_size,
        total_pages=total_pages,
        total_entries=total_entries,
        has_next=page < total_pages,
        has_previous=page > 1
    )


# ------------------ Badge Endpoints ------------------

@router.get("/badges", response_model=List[BadgeSchema])
def get_all_badges(db: Session = Depends(get_db)):
    """Get all available badges"""
    badges = db.query(Badge).filter(Badge.is_active == True).order_by(Badge.required_karma_points).all()
    return badges

@router.get("/users/me/badges", response_model=List[UserBadge])
def get_user_badges(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get all badges earned by the current user"""
    user_badges_data = db.query(user_badges).filter(user_badges.c.user_id == current_user.id).all()
    
    result = []
    for user_badge in user_badges_data:
        badge = db.query(Badge).filter(Badge.id == user_badge.badge_id).first()
        if badge:
            result.append(UserBadge(badge=badge, earned_at=user_badge.earned_at))
    
    return result

@router.post("/users/me/check-badges", response_model=BadgeCheckResponse)
def check_badge_achievements(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Check for newly earned badges based on current karma points"""
    
    # Get all badges the user can potentially earn
    available_badges = db.query(Badge).filter(
        Badge.is_active == True,
        Badge.required_karma_points <= current_user.karma_points
    ).all()
    
    # Get badges the user has already earned
    earned_badge_ids = [row.badge_id for row in db.query(user_badges.c.badge_id).filter(
        user_badges.c.user_id == current_user.id
    ).all()]
    
    # Find newly earned badges
    newly_earned = []
    for badge in available_badges:
        if badge.id not in earned_badge_ids:
            # Award the badge to the user
            db.execute(user_badges.insert().values(
                user_id=current_user.id,
                badge_id=badge.id
            ))
            
            newly_earned.append(BadgeAchievement(
                badge_id=badge.id,
                badge_name=badge.name,
                description=badge.description or "",
                icon_name=badge.icon_name,
                color=badge.color,
                earned_at=func.now()
            ))
    
    db.commit()
    
    # Find next badge to work towards
    next_badge = db.query(Badge).filter(
        Badge.is_active == True,
        Badge.required_karma_points > current_user.karma_points
    ).order_by(Badge.required_karma_points).first()
    
    # Count total badges earned
    total_badges_earned = len(earned_badge_ids) + len(newly_earned)
    
    return BadgeCheckResponse(
        newly_earned_badges=newly_earned,
        total_badges_earned=total_badges_earned,
        next_badge=next_badge
    ) 
