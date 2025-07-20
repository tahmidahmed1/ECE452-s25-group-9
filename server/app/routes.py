from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, WebSocket, WebSocketDisconnect
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta, datetime
import logging
from typing import List, Dict
from fastapi.responses import JSONResponse
from sqlalchemy import or_
import math
from sqlalchemy import func

from .database import get_db
from .models import User, UserType, Sex, Event, Message, Badge, user_badges, user_subscriptions, InAppNotification
from sqlalchemy.orm import selectinload
from .schemas import (
    UserCreate, User as UserSchema, Token, OnboardingStepOne, OnboardingStepTwoOrganizer,
    OnboardingComplete, ProfilePictureUploadResponse, EventSchema,
    UserUpdate, EventCreate, EventImageOut,
    MessageCreate, MessageOut,
    ProfileBannerUploadResponse,
    LeaderboardResponse, LeaderboardEntry,
    Badge as BadgeSchema, UserBadge, BadgeCheckResponse, BadgeAchievement,
    SubscriptionCreate, SubscriptionResponse, SubscriptionStatus,
    UserSubscriptionsResponse, OrganizerWithSubscriptionStatus,
    NotificationTokenUpdate, NotificationPreferences, SubscriptionRequest,
    NotificationRequest, NotificationResponse,
    InAppNotificationCreate, InAppNotificationOut, InAppNotificationUpdate,
    InAppNotificationsResponse,
)
from .auth import (
    authenticate_user, create_access_token, get_password_hash, 
    get_current_active_user, ACCESS_TOKEN_EXPIRE_MINUTES
)
from .storage import storage_service
from .firebase_service import firebase_service

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

@router.post("/logout")
def logout(current_user: User = Depends(get_current_active_user)):
    """Logout endpoint - for JWT tokens this just logs the action since tokens are stateless"""
    logger.info(f"Logout request for user: {current_user.username}")
    # With JWT tokens, we can't invalidate them server-side without a blacklist
    # So we just log the logout action and return success
    # The client should delete the token from local storage
    return {"message": "Successfully logged out"}

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

@router.post("/remove-profile-picture")
async def remove_profile_picture(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    logger.info(f"Profile picture removal request from user: {current_user.username}")
    
    # Delete existing profile picture if exists
    if current_user.profile_picture_url:
        storage_service.delete_profile_picture(current_user.profile_picture_url)
        
        # Remove profile picture URL from database
        current_user.profile_picture_url = None
        db.commit()
        db.refresh(current_user)
        
        return {"message": "Profile picture removed successfully"}
    else:
        return {"message": "No profile picture to remove"}

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

@router.post("/complete-organizer-onboarding")
def complete_organizer_onboarding(
    onboarding_data: OnboardingStepTwoOrganizer,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    logger.info(f"Completing organizer onboarding for user {current_user.username}")
    
    # Update user profile with all organizer fields
    current_user.full_name = onboarding_data.full_name
    current_user.phone = onboarding_data.phone
    current_user.onboarding_completed = True
    
    # Organizer-specific fields
    current_user.organization_name = onboarding_data.organization_name
    current_user.organization_description = onboarding_data.organization_description
    current_user.organization_website = str(onboarding_data.organization_website) if onboarding_data.organization_website else None
    
    # Handle social media links
    if onboarding_data.organization_social_media:
        social_media_data = []
        for link in onboarding_data.organization_social_media:
            social_media_data.append({
                "platform": link.platform.value,
                "url": str(link.url)
            })
        current_user.organization_social_media = social_media_data
    
    # Handle organization images
    if onboarding_data.organization_images:
        current_user.organization_images = onboarding_data.organization_images
    
    db.commit()
    db.refresh(current_user)
    
    logger.info(f"Organizer onboarding completed for user {current_user.username}")
    return {"message": "Organizer onboarding completed successfully"}

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
    query = db.query(Event).options(selectinload(Event.images))
    
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
async def create_event(event: EventCreate, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    if current_user.user_type != UserType.ORGANIZER:
        raise HTTPException(status_code=403, detail="Only organizers can create events")
    from .models import OpportunityCategory

    event_data = event.dict()
    # Ensure category stored as proper Enum, not raw string
    raw_cat = event_data.pop("category", OpportunityCategory.OTHER)
    if isinstance(raw_cat, str):
        try:
            category_enum = OpportunityCategory[raw_cat.upper()]
        except KeyError:
            category_enum = OpportunityCategory.OTHER
    else:
        category_enum = raw_cat  # already enum

    if event_data.get("current_volunteers") is None:
        event_data["current_volunteers"] = 0

    new_event = Event(**event_data, category=category_enum, organizer_id=current_user.id)
    db.add(new_event)
    db.commit()
    db.refresh(new_event)
    
    # Send notification to subscribers
    try:
        await send_event_notification(
            organizer_id=current_user.id,
            event_title=new_event.title,
            organizer_name=current_user.full_name or current_user.username,
            event_id=new_event.id,
            db=db
        )
    except Exception as e:
        # Don't fail event creation if notification fails
        logger.error(f"Failed to send event notification: {e}")
    
    # Create in-app notifications for subscribers
    try:
        await create_event_in_app_notifications(
            organizer_id=current_user.id,
            event_title=new_event.title,
            organizer_name=current_user.full_name or current_user.username,
            event_id=new_event.id,
            db=db
        )
    except Exception as e:
        # Don't fail event creation if in-app notification fails
        logger.error(f"Failed to create in-app notifications: {e}")
    
    return new_event

@router.get("/events/{event_id}", response_model=EventSchema)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(Event).options(selectinload(Event.images)).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    
    # Debug logging
    logger.info(f"Returning event {event_id} with {len(event.images)} images")
    for i, img in enumerate(event.images):
        logger.info(f"Image {i}: id={img.id}, url={img.image_url}, is_main={img.is_main}")
    
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
    events = db.query(Event).options(selectinload(Event.images)).filter(Event.organizer_id == organizer_id).all()
    
    # Debug logging
    logger.info(f"Returning {len(events)} events for organizer {organizer_id}")
    for event in events:
        logger.info(f"Event {event.id} ({event.title}) has {len(event.images)} images")
        for i, img in enumerate(event.images):
            logger.info(f"  Image {i}: id={img.id}, url={img.image_url}, is_main={img.is_main}")
    
    return events

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

# Multiple event images endpoints

@router.post("/events/{event_id}/images/upload", response_model=EventImageOut)
async def upload_event_image_to_carousel(
    event_id: int,
    file: UploadFile = File(...),
    is_main: bool = Form(False),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Upload an image to event carousel (up to 10 images)."""
    from .models import Event, EventImage
    
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    # Check if event already has 10 images
    existing_images_count = db.query(EventImage).filter(EventImage.event_id == event_id).count()
    if existing_images_count >= 10:
        raise HTTPException(status_code=400, detail="Maximum 10 images allowed per event")

    # Upload to object storage
    url = await storage_service.upload_event_image(file, event_id)

    # Auto-set as main image if this is the first image for this event
    if existing_images_count == 0:
        is_main = True
        logger.info(f"Setting first image as main for event {event_id}")

    # If this is marked as main, unset other main images
    if is_main:
        db.query(EventImage).filter(
            EventImage.event_id == event_id,
            EventImage.is_main == True
        ).update({"is_main": False})
        event.image_url = url # Also update the main event image_url
        logger.info(f"Set image as main for event {event_id}: {url}")

    # Get next display order
    max_order = db.query(func.max(EventImage.display_order)).filter(
        EventImage.event_id == event_id
    ).scalar() or 0

    # Create new event image
    event_image = EventImage(
        event_id=event_id,
        image_url=url,
        is_main=is_main,
        display_order=max_order + 1
    )
    db.add(event_image)
    db.commit()
    db.refresh(event_image)

    return event_image

@router.get("/events/{event_id}/images", response_model=List[EventImageOut])
async def get_event_images(
    event_id: int,
    db: Session = Depends(get_db),
):
    """Get all images for an event, ordered by display_order."""
    from .models import EventImage
    
    images = db.query(EventImage).filter(
        EventImage.event_id == event_id
    ).order_by(EventImage.display_order).all()
    
    return images

@router.delete("/events/{event_id}/images/{image_id}")
async def delete_event_image(
    event_id: int,
    image_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Delete an event image."""
    from .models import Event, EventImage
    
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    event_image = db.get(EventImage, image_id)
    if not event_image or event_image.event_id != event_id:
        raise HTTPException(status_code=404, detail="Image not found")

    # Delete from object storage
    try:
        await storage_service.delete_file_from_url(event_image.image_url)
    except Exception:
        pass  # Continue even if storage deletion fails

    db.delete(event_image)
    db.commit()

    return {"message": "Image deleted successfully"}

@router.patch("/events/{event_id}/images/{image_id}/set-main")
async def set_main_event_image(
    event_id: int,
    image_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Set an image as the main image for the event."""
    from .models import Event, EventImage
    
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    event_image = db.get(EventImage, image_id)
    if not event_image or event_image.event_id != event_id:
        raise HTTPException(status_code=404, detail="Image not found")

    # Unset all other main images for this event
    db.query(EventImage).filter(
        EventImage.event_id == event_id,
        EventImage.is_main == True
    ).update({"is_main": False})

    # Set this image as main
    event_image.is_main = True
    event.image_url = event_image.image_url  # Also update the main event image_url
    db.commit()
    
    logger.info(f"Manually set image {image_id} as main for event {event_id}")
    return {"message": "Main image updated successfully"}

@router.patch("/events/{event_id}/images/reorder")
async def reorder_event_images(
    event_id: int,
    image_orders: List[dict],  # [{"image_id": 1, "display_order": 1}, ...]
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Reorder event images."""
    from .models import Event, EventImage
    
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    # Update display orders
    for item in image_orders:
        db.query(EventImage).filter(
            EventImage.id == item["image_id"],
            EventImage.event_id == event_id
        ).update({"display_order": item["display_order"]})

    db.commit()
    return {"message": "Images reordered successfully"}

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

# ----- Badge Seed Config -----
BADGE_CONFIG = [
    {
        "name": "Karma 200",
        "description": "Earn 200 total karma points",
        "required": 200,
        "icon": "Star",
        "color": "#FFD54F",
    },
    {
        "name": "Karma 400",
        "description": "Earn 400 total karma points",
        "required": 400,
        "icon": "WorkspacePremium",
        "color": "#FFC107",
    },
    {
        "name": "Karma 600",
        "description": "Earn 600 total karma points",
        "required": 600,
        "icon": "LocalFireDepartment",
        "color": "#FFB300",
    },
    {
        "name": "Karma 800",
        "description": "Earn 800 total karma points",
        "required": 800,
        "icon": "EmojiEvents",
        "color": "#FFA000",
    },
    {
        "name": "Karma 1000",
        "description": "Earn 1000 total karma points",
        "required": 1000,
        "icon": "WorkspacePremium",
        "color": "#FF8F00",
    },
]

@router.post("/users/me/check-badges", response_model=BadgeCheckResponse)
def check_badge_achievements(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Check for newly earned badges based on current karma points"""
    
    # Ensure badge definitions exist
    for cfg in BADGE_CONFIG:
        badge = db.query(Badge).filter(Badge.required_karma_points == cfg["required"]).first()
        if not badge:
            badge = Badge(
                name=cfg["name"],
                description=cfg["description"],
                required_karma_points=cfg["required"],
                icon_name=cfg["icon"],
                color=cfg["color"],
                is_active=True,
            )
            db.add(badge)
    db.commit()

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
                earned_at=datetime.utcnow()
            ))
    
    db.commit()
    
    # Find next badge to work towards
    next_badge = db.query(Badge).filter(
        Badge.is_active == True,
        Badge.required_karma_points > current_user.karma_points
    ).order_by(Badge.required_karma_points).first()
    
    # Count total badges earned
    total_badges_earned = len(earned_badge_ids) + len(newly_earned)
    
    db.refresh(current_user)
    return BadgeCheckResponse(
        newly_earned_badges=newly_earned,
        total_badges_earned=total_badges_earned,
        next_badge=next_badge
    )

# ==================== NOTIFICATION ROUTES ====================

@router.put("/notifications/token", response_model=NotificationResponse)
async def update_fcm_token(
    token_update: NotificationTokenUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update FCM token for the current user"""
    try:
        current_user.fcm_token = token_update.fcm_token
        db.commit()
        
        logger.info(f"Updated FCM token for user {current_user.id}")
        return NotificationResponse(success=True, message="FCM token updated successfully")
    
    except Exception as e:
        logger.error(f"Failed to update FCM token: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update FCM token"
        )

@router.put("/notifications/preferences", response_model=NotificationResponse)
async def update_notification_preferences(
    preferences: NotificationPreferences,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update notification preferences for the current user"""
    try:
        current_user.notifications_enabled = preferences.notifications_enabled
        db.commit()
        
        logger.info(f"Updated notification preferences for user {current_user.id}: {preferences.notifications_enabled}")
        return NotificationResponse(success=True, message="Notification preferences updated successfully")
    
    except Exception as e:
        logger.error(f"Failed to update notification preferences: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update notification preferences"
        )

@router.post("/notifications/subscribe", response_model=NotificationResponse)
async def subscribe_to_organizer_notifications(
    subscription: SubscriptionRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Subscribe to notifications from an organizer (same as regular subscription)"""
    try:
        # Verify organizer exists and is an organizer
        organizer = db.query(User).filter(
            User.id == subscription.organizer_id,
            User.user_type == UserType.ORGANIZER
        ).first()
        
        if not organizer:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Organizer not found"
            )
        
        # Check if already subscribed
        existing = db.execute(
            user_subscriptions.select().where(
                user_subscriptions.c.subscriber_id == current_user.id,
                user_subscriptions.c.organizer_id == subscription.organizer_id
            )
        ).first()
        
        if existing:
            return NotificationResponse(success=True, message="Already subscribed to this organizer")
        
        # Create subscription
        db.execute(user_subscriptions.insert().values(
            subscriber_id=current_user.id,
            organizer_id=subscription.organizer_id
        ))
        db.commit()
        
        logger.info(f"User {current_user.id} subscribed to organizer {subscription.organizer_id}")
        return NotificationResponse(success=True, message="Successfully subscribed to organizer notifications")
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to subscribe to organizer notifications: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to subscribe to organizer notifications"
        )

@router.post("/notifications/unsubscribe", response_model=NotificationResponse)
async def unsubscribe_from_organizer_notifications(
    subscription: SubscriptionRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Unsubscribe from notifications from an organizer"""
    try:
        # Remove subscription
        result = db.execute(
            user_subscriptions.delete().where(
                user_subscriptions.c.subscriber_id == current_user.id,
                user_subscriptions.c.organizer_id == subscription.organizer_id
            )
        )
        
        if result.rowcount == 0:
            return NotificationResponse(success=True, message="Not subscribed to this organizer")
        
        db.commit()
        
        logger.info(f"User {current_user.id} unsubscribed from organizer {subscription.organizer_id}")
        return NotificationResponse(success=True, message="Successfully unsubscribed from organizer notifications")
    
    except Exception as e:
        logger.error(f"Failed to unsubscribe from organizer notifications: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to unsubscribe from organizer notifications"
        )

@router.post("/notifications/send", response_model=NotificationResponse)
async def send_notification_to_subscribers(
    notification: NotificationRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Send notification to all subscribers of an organizer (admin/organizer only)"""
    try:
        # Verify current user is the organizer or admin
        if current_user.id != notification.organizer_id and current_user.user_type != UserType.ORGANIZER:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Only organizers can send notifications to their subscribers"
            )
        
        # Get all subscribers with FCM tokens and notifications enabled
        subscribers = db.query(User).join(
            user_subscriptions,
            User.id == user_subscriptions.c.subscriber_id
        ).filter(
            user_subscriptions.c.organizer_id == notification.organizer_id,
            User.fcm_token.isnot(None),
            User.notifications_enabled == True
        ).all()
        
        if not subscribers:
            return NotificationResponse(
                success=True,
                message="No subscribers with push notifications enabled found"
            )
        
        # Extract FCM tokens
        fcm_tokens = [user.fcm_token for user in subscribers if user.fcm_token]
        
        if not fcm_tokens:
            return NotificationResponse(
                success=True,
                message="No valid FCM tokens found for subscribers"
            )
        
        # Send notifications using Firebase
        results = firebase_service.send_notification_to_multiple_tokens(
            tokens=fcm_tokens,
            title=notification.title,
            body=notification.body,
            data=notification.data or {}
        )
        
        # Count successful sends
        successful_sends = sum(1 for success in results.values() if success)
        total_attempts = len(fcm_tokens)
        
        logger.info(f"Sent notifications: {successful_sends}/{total_attempts} successful")
        
        return NotificationResponse(
            success=True,
            message=f"Notifications sent successfully to {successful_sends} out of {total_attempts} subscribers"
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to send notification: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to send notification"
        )

async def send_event_notification(
    organizer_id: int,
    event_title: str,
    organizer_name: str,
    event_id: int,
    db: Session
):
    """Helper function to send notifications when a new event is created"""
    try:
        # Get all subscribers with FCM tokens and notifications enabled
        subscribers = db.query(User).join(
            user_subscriptions,
            User.id == user_subscriptions.c.subscriber_id
        ).filter(
            user_subscriptions.c.organizer_id == organizer_id,
            User.fcm_token.isnot(None),
            User.notifications_enabled == True
        ).all()
        
        if not subscribers:
            logger.info(f"No subscribers found for organizer {organizer_id}")
            return
        
        # Extract FCM tokens
        fcm_tokens = [user.fcm_token for user in subscribers if user.fcm_token]
        
        if not fcm_tokens:
            logger.info(f"No valid FCM tokens found for organizer {organizer_id} subscribers")
            return
        
        # Send notifications
        results = firebase_service.send_notification_to_multiple_tokens(
            tokens=fcm_tokens,
            title="New Volunteer Opportunity!",
            body=f"{organizer_name} posted: {event_title}",
            data={
                "type": "new_event",
                "eventId": str(event_id),
                "organizerId": str(organizer_id),
                "organizerName": organizer_name,
                "eventTitle": event_title
            }
        )
        
        # Count successful sends
        successful_sends = sum(1 for success in results.values() if success)
        total_attempts = len(fcm_tokens)
        
        logger.info(f"Event notification sent: {successful_sends}/{total_attempts} successful for event {event_id}")
        
    except Exception as e:
        logger.error(f"Failed to send event notification: {e}")

async def create_event_in_app_notifications(
    organizer_id: int,
    event_title: str,
    organizer_name: str,
    event_id: int,
    db: Session
):
    """Helper function to create in-app notifications when a new event is created"""
    try:
        # Get all subscribers for this organizer
        subscribers = db.query(User).join(
            user_subscriptions,
            User.id == user_subscriptions.c.subscriber_id
        ).filter(
            user_subscriptions.c.organizer_id == organizer_id
        ).all()
        
        if not subscribers:
            logger.info(f"No subscribers found for organizer {organizer_id}")
            return
        
        # Create in-app notifications for each subscriber
        notifications_created = 0
        for subscriber in subscribers:
            try:
                in_app_notification = InAppNotification(
                    user_id=subscriber.id,
                    title="New Volunteer Opportunity!",
                    message=f"{organizer_name} posted: {event_title}",
                    data={
                        "type": "new_event",
                        "eventId": str(event_id),
                        "organizerId": str(organizer_id),
                        "organizerName": organizer_name,
                        "eventTitle": event_title
                    }
                )
                db.add(in_app_notification)
                notifications_created += 1
            except Exception as e:
                logger.error(f"Failed to create in-app notification for user {subscriber.id}: {e}")
        
        db.commit()
        logger.info(f"Created {notifications_created} in-app notifications for event {event_id}")
        
    except Exception as e:
        logger.error(f"Failed to create in-app notifications for event: {e}")
        db.rollback()

# ==================== IN-APP NOTIFICATION ROUTES ====================

@router.get("/in-app-notifications", response_model=InAppNotificationsResponse)
def get_in_app_notifications(
    limit: int = 50,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get in-app notifications for the current user"""
    try:
        # Get notifications for the current user, ordered by creation date (newest first)
        notifications = db.query(InAppNotification).filter(
            InAppNotification.user_id == current_user.id
        ).order_by(InAppNotification.created_at.desc()).limit(limit).all()
        
        # Count unread notifications
        unread_count = db.query(InAppNotification).filter(
            InAppNotification.user_id == current_user.id,
            InAppNotification.is_read == False
        ).count()
        
        return InAppNotificationsResponse(
            notifications=notifications,
            unread_count=unread_count
        )
        
    except Exception as e:
        logger.error(f"Failed to get in-app notifications: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve notifications"
        )

@router.post("/in-app-notifications", response_model=InAppNotificationOut)
def create_in_app_notification(
    notification: InAppNotificationCreate,
    db: Session = Depends(get_db)
):
    """Create a new in-app notification (internal use)"""
    try:
        new_notification = InAppNotification(
            user_id=notification.user_id,
            title=notification.title,
            message=notification.message,
            data=notification.data
        )
        
        db.add(new_notification)
        db.commit()
        db.refresh(new_notification)
        
        logger.info(f"Created in-app notification for user {notification.user_id}")
        return new_notification
        
    except Exception as e:
        logger.error(f"Failed to create in-app notification: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create notification"
        )

@router.put("/in-app-notifications/{notification_id}", response_model=InAppNotificationOut)
def update_in_app_notification(
    notification_id: int,
    update_data: InAppNotificationUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Update an in-app notification (mark as read/unread)"""
    try:
        # Get the notification and verify it belongs to the current user
        notification = db.query(InAppNotification).filter(
            InAppNotification.id == notification_id,
            InAppNotification.user_id == current_user.id
        ).first()
        
        if not notification:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Notification not found"
            )
        
        # Update the notification
        if update_data.is_read is not None:
            notification.is_read = update_data.is_read
            if update_data.is_read:
                notification.read_at = func.now()
            else:
                notification.read_at = None
        
        db.commit()
        db.refresh(notification)
        
        logger.info(f"Updated notification {notification_id} for user {current_user.id}")
        return notification
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to update in-app notification: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update notification"
        )

@router.delete("/in-app-notifications/{notification_id}", response_model=dict)
def delete_in_app_notification(
    notification_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Delete an in-app notification"""
    try:
        # Get the notification and verify it belongs to the current user
        notification = db.query(InAppNotification).filter(
            InAppNotification.id == notification_id,
            InAppNotification.user_id == current_user.id
        ).first()
        
        if not notification:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Notification not found"
            )
        
        db.delete(notification)
        db.commit()
        
        logger.info(f"Deleted notification {notification_id} for user {current_user.id}")
        return {"success": True, "message": "Notification deleted successfully"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to delete in-app notification: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete notification"
        )

@router.put("/in-app-notifications/mark-all-read", response_model=dict)
def mark_all_notifications_read(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Mark all notifications as read for the current user"""
    try:
        # Update all unread notifications for the current user
        updated_count = db.query(InAppNotification).filter(
            InAppNotification.user_id == current_user.id,
            InAppNotification.is_read == False
        ).update({
            InAppNotification.is_read: True,
            InAppNotification.read_at: func.now()
        })
        
        db.commit()
        
        logger.info(f"Marked {updated_count} notifications as read for user {current_user.id}")
        return {
            "success": True, 
            "message": f"Marked {updated_count} notifications as read",
            "updated_count": updated_count
        }
        
    except Exception as e:
        logger.error(f"Failed to mark all notifications as read: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to mark notifications as read"
        )

@router.delete("/in-app-notifications", response_model=dict)
def clear_all_notifications(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Clear all notifications for the current user"""
    try:
        # Delete all notifications for the current user
        deleted_count = db.query(InAppNotification).filter(
            InAppNotification.user_id == current_user.id
        ).delete()
        
        db.commit()
        
        logger.info(f"Cleared {deleted_count} notifications for user {current_user.id}")
        return {
            "success": True, 
            "message": f"Cleared {deleted_count} notifications",
            "deleted_count": deleted_count
        }
        
    except Exception as e:
        logger.error(f"Failed to clear all notifications: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to clear notifications"
        )

@router.post("/dev/increase-karma", response_model=UserSchema)
def increase_karma_dev_only(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Development-only endpoint to increase karma points by 100 for testing purposes"""
    try:
        # Only allow for volunteer users
        if current_user.user_type != UserType.VOLUNTEER:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="This endpoint is only available for volunteer users"
            )
        
        # Increase karma points by 100
        current_user.karma_points += 100
        db.commit()

        # After karma updated, check for new badges automatically
        # Reuse the function directly
        check_badge_achievements(current_user=current_user, db=db)

        db.refresh(current_user)
        
        logger.info(f"[DEV] Increased karma points by 100 for user {current_user.username}. New total: {current_user.karma_points}")
        
        return current_user
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except Exception as e:
        logger.error(f"Failed to increase karma points: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to increase karma points"
        )
