from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, WebSocket, WebSocketDisconnect, Query
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta, datetime, timezone
import logging
from typing import List, Dict, Optional
from sqlalchemy import text, select
from fastapi.responses import JSONResponse, StreamingResponse
from sqlalchemy import or_
import math
from sqlalchemy import func
from openai import OpenAI
from dotenv import load_dotenv
from pathlib import Path
from io import BytesIO
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas
from reportlab.pdfbase.pdfmetrics import stringWidth

from .database import get_db
from .models import User, UserType, Sex, Event, Message, Badge, user_badges, user_subscriptions, volunteer_events, InAppNotification, LostFoundItem, LostFoundImage
from sqlalchemy.orm import selectinload
from . import schemas
from .schemas import (
    UserCreate, User as UserSchema, SessionResponse, OnboardingStepOne, OnboardingStepTwoOrganizer,
    OnboardingComplete, ProfilePictureUploadResponse, EventSchema,
    UserUpdate, EventCreate, EventImageOut,
    MessageCreate, MessageOut, ChatSummary,
    ProfileBannerUploadResponse,
    LeaderboardResponse, LeaderboardEntry,
    Badge as BadgeSchema, UserBadge, BadgeCheckResponse, BadgeAchievement,
    SubscriptionCreate, SubscriptionResponse, SubscriptionStatus,
    UserSubscriptionsResponse, OrganizerWithSubscriptionStatus,
    NotificationTokenUpdate, NotificationPreferences, SubscriptionRequest,
    NotificationRequest, NotificationResponse,
    InAppNotificationCreate, InAppNotificationOut, InAppNotificationUpdate,
    InAppNotificationsResponse,
    LostFoundItemCreate, LostFoundItemOut, LostFoundItemUpdate, LostFoundItemsResponse,
    EventVolunteer, VolunteerAttendanceRecord, AttendanceSubmission, AttendanceResponse,
    VolunteerHistoryEntry,
)
from .session_auth import (
    authenticate_user, get_password_hash, get_current_active_user, get_current_user,
    create_session, invalidate_session, invalidate_all_user_sessions, security
)
from fastapi.security import HTTPAuthorizationCredentials
from .storage import storage_service
from .firebase_service import firebase_service
import os

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter()

def populate_organizer_name(event: Event) -> None:
    """Populate the organizer_name field for an event based on organizer data"""
    if hasattr(event, 'organizer') and event.organizer:
        logger.info(f"🏢 POPULATE ORGANIZER - Event '{event.title}' (ID: {event.id})")
        logger.info(f"🏢 POPULATE ORGANIZER - Organizer: {event.organizer.username}")
        logger.info(f"🏢 POPULATE ORGANIZER - Organizer full_name: '{event.organizer.full_name}'")
        logger.info(f"🏢 POPULATE ORGANIZER - Has organization_name: {hasattr(event.organizer, 'organization_name')}")
        
        if hasattr(event.organizer, 'organization_name'):
            logger.info(f"🏢 POPULATE ORGANIZER - Organization name: '{event.organizer.organization_name}'")
        
        # Use organization name for organizers, or full name/username as fallback
        if hasattr(event.organizer, 'organization_name') and event.organizer.organization_name:
            event.organizer_name = event.organizer.organization_name
            logger.info(f"🏢 POPULATE ORGANIZER - Using organization_name: '{event.organizer_name}'")
        elif event.organizer.full_name:
            event.organizer_name = event.organizer.full_name
            logger.info(f"🏢 POPULATE ORGANIZER - Using full_name: '{event.organizer_name}'")
        else:
            event.organizer_name = event.organizer.username
            logger.info(f"🏢 POPULATE ORGANIZER - Using username: '{event.organizer_name}'")
    else:
        event.organizer_name = ""
        logger.warning(f"🏢 POPULATE ORGANIZER - No organizer found for event '{event.title}' (ID: {event.id})")

# Load environment variables from .env file located in the same directory (if present)
env_path = Path(__file__).parent / ".env"
if env_path.exists():
    load_dotenv(dotenv_path=env_path)

# Initialize OpenAI client (optional)
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if OPENAI_API_KEY:
    openai_client = OpenAI(api_key=OPENAI_API_KEY)
    logger.info("OpenAI client initialized – dynamic suggestions enabled")
else:
    openai_client = None
    logger.warning("OPENAI_API_KEY not set – suggestion endpoints will use static samples")

# Helper to call OpenAI Chat Completion
def _call_openai(prompt: str, model: str = "gpt-3.5-turbo", max_tokens: int = 200) -> str:
    """Call OpenAI ChatCompletion and return the assistant message content."""
    try:
        completion = openai_client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": "You are a helpful assistant for a volunteering platform."},
                {"role": "user", "content": prompt},
            ],
            temperature=0.7,
            max_tokens=max_tokens,
        )
        return completion.choices[0].message.content.strip()
    except Exception as e:
        logger.error(f"OpenAI API call failed: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate suggestion")

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

@router.post("/register")
def register_user(user: UserCreate, db: Session = Depends(get_db)):
    logger.info(f"📝 Registration request received for username: {user.username}, email: {user.email}")
    
    # Check if user already exists
    dup_user = db.query(User).filter(or_(User.username == user.username, User.email == user.email)).first()
    if dup_user:
        message = "Username already registered" if dup_user.username == user.username else "Email already registered"
        logger.warning(f"📝 Registration failed: {message}")
        logger.info(f"📝 Existing user found - ID: {dup_user.id}, Username: {dup_user.username}, Email: {dup_user.email}")
        return JSONResponse(status_code=409, content={"success": False, "message": message})
    
    # Create new user (onboarding_completed defaults to False)
    hashed_password = get_password_hash(user.password)
    db_user = User(
        username=user.username,
        email=user.email,
        hashed_password=hashed_password
    )
    
    # Check if this is a dev user and auto-complete onboarding
    if user.username.startswith("dev_") or user.username in ["volunteer", "organizer"]:
        logger.info(f"Dev user detected: {user.username}, auto-completing onboarding")
        
        # Extract user type from username
        if user.username == "volunteer":
            user_type_str = "VOLUNTEER"
        elif user.username == "organizer":
            user_type_str = "ORGANIZER"
        else:
            # Handle old format (e.g., "dev_volunteer_123456")
            parts = user.username.split("_")
            user_type_str = parts[1].upper() if len(parts) >= 2 else "VOLUNTEER"
        
        try:
            if user_type_str == "VOLUNTEER":
                db_user.user_type = UserType.VOLUNTEER
                if user.username == "volunteer":
                    db_user.full_name = "Dev Volunteer"
                    db_user.phone = "+1-555-DEV-VOL"
                else:
                    # Old format handling
                    parts = user.username.split("_")
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
                if user.username == "organizer":
                    db_user.full_name = "Dev Organizer"
                    db_user.phone = "+1-555-DEV-ORG"
                    db_user.organization_name = "Dev Organization"
                else:
                    # Old format handling
                    parts = user.username.split("_")
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
    
    logger.info(f"📝 User created successfully: {user.username}")
    logger.info(f"📝 New user details - ID: {db_user.id}, Username: {db_user.username}")
    logger.info(f"📝 New user - Email: {db_user.email}")
    logger.info(f"📝 New user - Onboarding completed: {db_user.onboarding_completed}")
    logger.info(f"📝 New user - User type: {db_user.user_type}")
    logger.info(f"📝 New user - Is active: {db_user.is_active}")
    
    # Create session for the new user
    session_id = create_session(db_user)
    logger.info(f"📝 Session created for user: {user.username}")
    logger.info(f"📝 Session ID: {session_id}")
    
    # Return session and user data
    logger.info(f"📝 Returning registration response with user data and session")
    logger.info(f"📝 Response will contain user ID: {db_user.id}, username: {db_user.username}")
    return {
        "session_id": session_id,
        "session_type": "session",
        "user": db_user
    }

@router.post("/login")
def login_for_session(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    logger.info(f"🔐 Login request received for username: {form_data.username}")
    
    user = authenticate_user(db, form_data.username, form_data.password)
    if not user:
        logger.warning(f"🔐 Login failed for username: {form_data.username}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    logger.info(f"🔐 User authenticated successfully - ID: {user.id}, Username: {user.username}")
    logger.info(f"🔐 User onboarding completed: {user.onboarding_completed}")
    logger.info(f"🔐 User type: {user.user_type}")
    
    # Invalidate any existing sessions for this user to ensure clean login
    invalidate_all_user_sessions(user.id)
    
    # Create new session
    session_id = create_session(user)
    logger.info(f"🔐 Login successful for username: {form_data.username}")
    logger.info(f"🔐 Session ID: {session_id}")
    
    # Return session and user data  
    logger.info(f"🔐 Returning login response with user data and session")
    logger.info(f"🔐 Response will contain user ID: {user.id}, username: {user.username}")
    return {
        "session_id": session_id,
        "session_type": "session",
        "user": user
    }

@router.get("/users/me", response_model=UserSchema)
def read_users_me(current_user: User = Depends(get_current_active_user)):
    logger.info(f"🔍 /users/me - User info request for: {current_user.username}")
    logger.info(f"🔍 /users/me - User ID: {current_user.id}")
    logger.info(f"🔍 /users/me - User onboarding completed: {current_user.onboarding_completed}")
    logger.info(f"🔍 /users/me - User type: {current_user.user_type}")
    logger.info(f"🔍 /users/me - User email: {current_user.email}")
    return current_user

@router.post("/logout")
def logout(current_user: User = Depends(get_current_active_user)):
    """Logout endpoint - invalidates the user's session"""
    logger.info(f"🚪 Logout request for user: {current_user.username} (ID: {current_user.id})")
    logger.info(f"🚪 Logout - User email: {current_user.email}")
    logger.info(f"🚪 Logout - User onboarding completed: {current_user.onboarding_completed}")
    
    # Invalidate all sessions for this user
    invalidate_all_user_sessions(current_user.id)
    
    logger.info("🚪 Logout completed - All user sessions invalidated")
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
    from datetime import datetime, timedelta
    
    query = db.query(Event).options(selectinload(Event.images), selectinload(Event.organizer))
    
    # DEFAULT: Only show future events (events that haven't started yet)
    # Filter by exact date and time comparison
    now = datetime.now()
    current_date = now.date().strftime("%Y-%m-%d")
    current_time = now.time().strftime("%H:%M")
    
    # Import SQLAlchemy or_ and and_ operators
    from sqlalchemy import or_, and_
    
    # Filter events: future dates OR today's events that haven't started yet
    query = query.filter(
        or_(
            Event.date > current_date,  # Future dates
            and_(
                Event.date == current_date,  # Today's events
                Event.start_time > current_time  # That haven't started yet
            )
        )
    )
    
    # Category filter
    if category:
        try:
            from .models import OpportunityCategory
            # Convert category value to enum by value (not by name)
            category_enum = OpportunityCategory(category)
            query = query.filter(Event.category == category_enum)
        except ValueError:
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
    
    # Date filter (additional filtering on top of future-only default)
    if date_filter:
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
    
    # Populate organizer names for all events
    for event in events:
        populate_organizer_name(event)
    
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
    
    # Populate organizer name for the newly created event
    new_event.organizer = current_user  # Set the organizer directly since we have it
    populate_organizer_name(new_event)
    
    return new_event

@router.get("/events/{event_id}", response_model=EventSchema)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(Event).options(selectinload(Event.images), selectinload(Event.organizer)).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    
    # Populate organizer name
    populate_organizer_name(event)
    
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
    
    # Store original values to check for important changes
    original_date = event.date
    original_start_time = event.start_time
    original_end_time = event.end_time
    original_location = event.location
    
    # Update event fields
    update_data = payload.dict(exclude_unset=True)
    for k, v in update_data.items():
        setattr(event, k, v)
    
    # Check if important fields changed (date, time, location)
    important_changes = []
    if update_data.get('date') and update_data['date'] != original_date:
        important_changes.append(f"Date changed to {update_data['date']}")
    if update_data.get('start_time') and update_data['start_time'] != original_start_time:
        important_changes.append(f"Start time changed to {update_data['start_time']}")
    if update_data.get('end_time') and update_data['end_time'] != original_end_time:
        important_changes.append(f"End time changed to {update_data['end_time']}")
    if update_data.get('location') and update_data['location'] != original_location:
        important_changes.append(f"Location changed to {update_data['location']}")
    
    db.commit()
    db.refresh(event)
    
    # Send notifications to joined volunteers if important fields changed
    if important_changes:
        try:
            await send_event_update_notification(
                event_id=event.id,
                event_title=event.title,
                organizer_name=current_user.full_name or current_user.username,
                changes=important_changes,
                db=db
            )
        except Exception as e:
            # Don't fail event update if notification fails
            logger.error(f"Failed to send event update notification: {e}")
        
        try:
            await create_event_update_in_app_notifications(
                event_id=event.id,
                event_title=event.title,
                organizer_name=current_user.full_name or current_user.username,
                changes=important_changes,
                db=db
            )
        except Exception as e:
            # Don't fail event update if in-app notification fails
            logger.error(f"Failed to create event update in-app notifications: {e}")
    
    # Ensure organizer is loaded and populate organizer name
    if not hasattr(event, 'organizer') or event.organizer is None:
        db.refresh(event, ['organizer'])
    populate_organizer_name(event)
    
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

@router.post("/events/{event_id}/join")
async def join_event(
    event_id: int, 
    current_user: User = Depends(get_current_active_user), 
    db: Session = Depends(get_db)
):
    """Join an event as a volunteer"""
    logger.info(f"🎯 JOIN EVENT - User {current_user.username} (ID: {current_user.id}) attempting to join event {event_id}")
    
    # Check if event exists (with organizer relationship loaded)
    from sqlalchemy.orm import selectinload
    event = db.query(Event).options(selectinload(Event.organizer)).filter(Event.id == event_id).first()
    if not event:
        logger.error(f"❌ JOIN EVENT - Event {event_id} not found")
        raise HTTPException(status_code=404, detail="Event not found")
    
    logger.info(f"📋 JOIN EVENT - Event details: '{event.title}' on {event.date} from {event.start_time} to {event.end_time}")
    logger.info(f"📋 JOIN EVENT - Event organizer: {event.organizer.username}, category: {event.category}")
    
    # Check if user is a volunteer
    if current_user.user_type != UserType.VOLUNTEER:
        logger.error(f"❌ JOIN EVENT - User {current_user.username} is not a volunteer (type: {current_user.user_type})")
        raise HTTPException(status_code=403, detail="Only volunteers can join events")
    
    # Check if event is full
    if event.max_volunteers and event.current_volunteers >= event.max_volunteers:
        logger.error(f"❌ JOIN EVENT - Event {event_id} is full ({event.current_volunteers}/{event.max_volunteers})")
        raise HTTPException(status_code=400, detail="Event is full")
    
    # Check if user is already joined
    existing_join = db.execute(
        select(volunteer_events).where(
            volunteer_events.c.volunteer_id == current_user.id,
            volunteer_events.c.event_id == event_id
        )
    ).first()
    
    if existing_join:
        logger.warning(f"⚠️ JOIN EVENT - User {current_user.username} already joined event {event_id}")
        raise HTTPException(status_code=400, detail="Already joined this event")
    
    # Check if event has started
    from datetime import datetime
    try:
        event_start_dt = datetime.strptime(f"{event.date} {event.start_time}", "%Y-%m-%d %H:%M")
        current_time = datetime.utcnow()
        logger.info(f"⏰ JOIN EVENT - Event start time: {event_start_dt}, Current time: {current_time}")
        if current_time >= event_start_dt:
            logger.error(f"❌ JOIN EVENT - Event {event_id} already started")
            raise HTTPException(status_code=400, detail="Event has already started")
    except Exception as e:
        logger.warning(f"⚠️ JOIN EVENT - Could not parse event start datetime for event {event_id}: {e}")
    
    # Join the event
    try:
        logger.info(f"📝 JOIN EVENT - Adding user {current_user.id} to volunteer_events table for event {event_id}")
        db.execute(
            volunteer_events.insert().values(
                volunteer_id=current_user.id,
                event_id=event_id,
                status='joined'
            )
        )
        
        # Update volunteer count
        old_count = event.current_volunteers
        event.current_volunteers += 1
        logger.info(f"📊 JOIN EVENT - Updated event volunteer count from {old_count} to {event.current_volunteers}")
        
        db.commit()
        logger.info(f"✅ JOIN EVENT - Database transaction committed successfully")
        
        logger.info(f"🎉 JOIN EVENT - User {current_user.username} successfully joined event {event_id} ('{event.title}')")
        logger.info(f"📊 JOIN EVENT - Event {event_id} now has {event.current_volunteers}/{event.max_volunteers or 'unlimited'} volunteers")
        
        return {"message": "Successfully joined event", "current_volunteers": event.current_volunteers}
        
    except Exception as e:
        logger.error(f"❌ JOIN EVENT - Failed to join event {event_id}: {e}")
        logger.error(f"❌ JOIN EVENT - Exception details: {str(e)}")
        logger.error(f"❌ JOIN EVENT - Exception type: {type(e).__name__}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Failed to join event")

@router.post("/events/{event_id}/leave")
async def leave_event(
    event_id: int, 
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Leave an event as a volunteer"""
    logger.info(f"User {current_user.username} (ID: {current_user.id}) attempting to leave event {event_id}")
    
    # Check if event exists
    event = db.get(Event, event_id)
    if not event:
        logger.error(f"Event {event_id} not found")
        raise HTTPException(status_code=404, detail="Event not found")
    
    # Check if user is joined
    existing_join = db.execute(
        select(volunteer_events).where(
            volunteer_events.c.volunteer_id == current_user.id,
            volunteer_events.c.event_id == event_id
        )
    ).first()
    
    if not existing_join:
        logger.warning(f"User {current_user.username} not joined to event {event_id}")
        raise HTTPException(status_code=400, detail="Not joined to this event")
    
    # Check if event has started
    from datetime import datetime
    try:
        event_start_dt = datetime.strptime(f"{event.date} {event.start_time}", "%Y-%m-%d %H:%M")
        if datetime.utcnow() >= event_start_dt:
            raise HTTPException(status_code=400, detail="Cannot leave event that has already started")
    except Exception:
        logger.warning("Could not parse event start datetime for event %s", event_id)
    
    # Leave the event
    try:
        db.execute(
            volunteer_events.delete().where(
                volunteer_events.c.volunteer_id == current_user.id,
                volunteer_events.c.event_id == event_id
            )
        )
        
        # Update volunteer count
        if event.current_volunteers > 0:
            event.current_volunteers -= 1
        
        db.commit()
        
        logger.info(f"User {current_user.username} successfully left event {event_id}")
        logger.info(f"Event {event_id} now has {event.current_volunteers}/{event.max_volunteers or 'unlimited'} volunteers")
        
        return {"message": "Successfully left event", "current_volunteers": event.current_volunteers}
        
    except Exception as e:
        logger.error(f"Failed to leave event {event_id}: {e}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Failed to leave event")

@router.get("/events/{event_id}/volunteers")
async def get_event_volunteers(
    event_id: int, 
    current_user: User = Depends(get_current_active_user), 
    db: Session = Depends(get_db)
):
    """Get list of volunteers who joined an event"""
    logger.info(f"Getting volunteers for event {event_id}")
    
    # Check if event exists
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    
    # Get volunteers
    volunteers = db.query(User).join(volunteer_events).filter(
        volunteer_events.c.event_id == event_id
    ).all()
    
    volunteer_list = []
    for volunteer in volunteers:
        volunteer_list.append({
            "id": volunteer.id,
            "username": volunteer.username,
            "full_name": volunteer.full_name,
            "profile_picture_url": volunteer.profile_picture_url
        })
    
    logger.info(f"Event {event_id} has {len(volunteer_list)} volunteers")
    return {"volunteers": volunteer_list, "total_count": len(volunteer_list)}

@router.get("/events/{event_id}/volunteers/attendance", response_model=List[EventVolunteer])
async def get_event_volunteers_for_attendance(
    event_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get list of volunteers for attendance tracking"""
    logger.info(f"Getting volunteers for attendance for event {event_id}")
    
    # Check if event exists and user is the organizer
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only event organizer can track attendance")
    
    # Get volunteers with join timestamps
    volunteers_query = db.query(User, volunteer_events.c.joined_at).join(
        volunteer_events, User.id == volunteer_events.c.volunteer_id
    ).filter(volunteer_events.c.event_id == event_id)
    
    volunteer_list = []
    for volunteer, joined_at in volunteers_query:
        volunteer_list.append(EventVolunteer(
            id=volunteer.id,
            user_id=volunteer.id,
            name=volunteer.full_name or volunteer.username,
            username=volunteer.username,
            email=volunteer.email,
            profile_picture_url=volunteer.profile_picture_url,
            joined_at=joined_at
        ))
    
    logger.info(f"Event {event_id} has {len(volunteer_list)} volunteers for attendance")
    return volunteer_list

# Helper functions for attendance notifications
async def send_karma_earned_notification(volunteer: User, event: Event, karma_earned: int, db: Session):
    """Send notification to volunteer about karma points earned"""
    try:
        # Create in-app notification
        notification_data = {
            "user_id": volunteer.id,
            "title": "Karma Points Earned!",
            "message": f"You earned {karma_earned} karma points for completing '{event.title}'",
            "data": {
                "type": "karma_earned",
                "event_id": str(event.id),
                "karma_points": str(karma_earned)
            }
        }
        
        in_app_notification = InAppNotification(**notification_data)
        db.add(in_app_notification)
        db.flush()
        
        # Send FCM notification if volunteer has notifications enabled
        if volunteer.fcm_token and volunteer.notifications_enabled:
            await firebase_service.send_notification(
                token=volunteer.fcm_token,
                title="Karma Points Earned!",
                body=f"You earned {karma_earned} karma points for completing '{event.title}'",
                data={
                    "type": "karma_earned",
                    "event_id": str(event.id),
                    "karma_points": str(karma_earned)
                }
            )
            
        logger.info(f"Karma earned notification sent to volunteer {volunteer.username}")
        
    except Exception as e:
        logger.error(f"Failed to send karma earned notification to {volunteer.username}: {e}")

async def send_volunteer_rejected_notification(volunteer: User, event: Event, rejection_reason: str, db: Session):
    """Send notification to volunteer about event rejection"""
    try:
        # Create in-app notification
        notification_data = {
            "user_id": volunteer.id,
            "title": "Event Participation Update",
            "message": f"Your participation in '{event.title}' was not approved. Reason: {rejection_reason}",
            "data": {
                "type": "volunteer_rejected",
                "event_id": str(event.id),
                "rejection_reason": rejection_reason
            }
        }
        
        in_app_notification = InAppNotification(**notification_data)
        db.add(in_app_notification)
        db.flush()
        
        # Send FCM notification if volunteer has notifications enabled
        if volunteer.fcm_token and volunteer.notifications_enabled:
            await firebase_service.send_notification(
                token=volunteer.fcm_token,
                title="Event Participation Update",
                body=f"Your participation in '{event.title}' was not approved",
                data={
                    "type": "volunteer_rejected",
                    "event_id": str(event.id),
                    "rejection_reason": rejection_reason
                }
            )
            
        logger.info(f"Volunteer rejection notification sent to {volunteer.username}")
        
    except Exception as e:
        logger.error(f"Failed to send volunteer rejection notification to {volunteer.username}: {e}")

@router.post("/events/{event_id}/attendance", response_model=AttendanceResponse)
async def submit_attendance(
    event_id: int,
    attendance_data: AttendanceSubmission,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Submit volunteer attendance for an event"""
    logger.info(f"Submitting attendance for event {event_id} by organizer {current_user.username}")
    
    # Check if event exists and user is the organizer
    event = db.get(Event, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only event organizer can submit attendance")
    
    # Verify event_id matches
    if attendance_data.event_id != event_id:
        raise HTTPException(status_code=400, detail="Event ID mismatch")
    
    karma_points_awarded = {}
    approved_count = 0
    
    for record in attendance_data.attendance_records:
        # Get volunteer user
        volunteer = db.get(User, record.volunteer_id)
        if not volunteer:
            logger.warning(f"Volunteer {record.volunteer_id} not found, skipping")
            continue
        
        if record.is_approved and record.hours_worked and record.hours_worked > 0:
            # Calculate karma points (event karma points per hour worked)
            karma_earned = int(event.karma_points * record.hours_worked)
            
            # Award karma points
            volunteer.karma_points = (volunteer.karma_points or 0) + karma_earned
            karma_points_awarded[volunteer.username] = karma_earned
            approved_count += 1
            
            # Persist attendance details in association table
            db.execute(
                volunteer_events.update().where(
                    (volunteer_events.c.volunteer_id == volunteer.id) &
                    (volunteer_events.c.event_id == event_id)
                ).values(
                    hours_worked=record.hours_worked,
                    is_approved=True,
                    rejection_reason=None,
                    karma_points_earned=karma_earned,
                    status='completed',
                    karma_awarded=True
                )
            )
            
            # Send notification to volunteer about karma earned
            await send_karma_earned_notification(volunteer, event, karma_earned, db)
            
            logger.info(f"Awarded {karma_earned} karma points to volunteer {volunteer.username}")
        
        elif not record.is_approved and record.rejection_reason:
            # Persist rejection details in association table
            db.execute(
                volunteer_events.update().where(
                    (volunteer_events.c.volunteer_id == volunteer.id) &
                    (volunteer_events.c.event_id == event_id)
                ).values(
                    hours_worked=None,
                    is_approved=False,
                    rejection_reason=record.rejection_reason,
                    karma_points_earned=0,
                    status='rejected',
                    karma_awarded=False
                )
            )
            
            # Send notification to volunteer about rejection
            await send_volunteer_rejected_notification(volunteer, event, record.rejection_reason, db)
            
            logger.info(f"Volunteer {volunteer.username} rejected for event {event.title}")
    
    db.commit()
    
    message = f"Attendance submitted successfully. {approved_count} volunteers approved."
    if karma_points_awarded:
        message += f" Total karma awarded: {sum(karma_points_awarded.values())} points."
    
    return AttendanceResponse(
        success=True,
        message=message,
        karma_points_awarded=karma_points_awarded
    )

@router.get("/users/me/volunteer-history", response_model=List[VolunteerHistoryEntry])
async def get_volunteer_history(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get volunteer history for the current user"""
    logger.info(f"Getting volunteer history for user {current_user.username}")
    
    if current_user.user_type != UserType.VOLUNTEER:
        raise HTTPException(status_code=403, detail="Only volunteers can access volunteer history")
    
    # Get volunteer attendance records with joined event info
    history_entries = []
    attendance_rows = db.query(
        Event,
        volunteer_events.c.hours_worked,
        volunteer_events.c.is_approved,
        volunteer_events.c.rejection_reason,
        volunteer_events.c.karma_points_earned
    ).join(
        volunteer_events, Event.id == volunteer_events.c.event_id
    ).options(selectinload(Event.images), selectinload(Event.organizer)).filter(
        volunteer_events.c.volunteer_id == current_user.id
    ).all()

    for event, hours_worked, is_approved, rejection_reason, karma_points_earned in attendance_rows:
        # Determine status based on is_approved value
        if is_approved is None:
            status = "pending"
            is_approved_bool = False
        elif is_approved is True:
            status = "approved"
            is_approved_bool = True
        else:  # is_approved is False
            status = "rejected"
            is_approved_bool = False
        
        # Get event image URLs
        event_image_urls = [img.image_url for img in event.images] if event.images else []
        
        # Get organizer name
        organizer_name = None
        if event.organizer:
            organizer_name = event.organizer.organization_name or event.organizer.full_name or event.organizer.username
            
        history_entries.append(VolunteerHistoryEntry(
            event_id=event.id,
            event_title=event.title,
            event_date=event.date,
            event_description=event.description,
            event_location=event.location,
            event_start_time=event.start_time,
            event_end_time=event.end_time,
            event_image_urls=event_image_urls,
            organizer_name=organizer_name,
            hours_worked=hours_worked,
            is_approved=is_approved_bool,
            rejection_reason=rejection_reason,
            karma_points_earned=karma_points_earned or 0,
            status=status
        ))

    logger.info(f"Found {len(history_entries)} volunteer history entries for user {current_user.username}")
    return history_entries

@router.get("/users/me/volunteer-history/pdf")
def download_volunteer_history_pdf(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Generate and return a PDF summary of the volunteer's history with proper formatting."""
    logger.info(f"Generating PDF for volunteer history for user {current_user.username}")
    
    if current_user.user_type != UserType.VOLUNTEER:
        raise HTTPException(status_code=403, detail="Only volunteers can access volunteer history PDF")
    
    # Get volunteer attendance records with full event info
    logger.info(f"📊 PDF: Querying events for user {current_user.id} ({current_user.username})")
    
    # First, let's see all volunteer_events entries for this user
    all_volunteer_events = db.execute(
        select(volunteer_events).where(volunteer_events.c.volunteer_id == current_user.id)
    ).fetchall()
    
    logger.info(f"📊 PDF: Found {len(all_volunteer_events)} total volunteer_events entries for user {current_user.id}")
    for entry in all_volunteer_events:
        logger.info(f"  📝 PDF: Event ID: {entry.event_id}, Status: {getattr(entry, 'status', 'N/A')}, Approved: {getattr(entry, 'is_approved', 'N/A')}")
    
    # Now get the detailed event data
    attendance_rows = db.query(
        Event,
        volunteer_events.c.hours_worked,
        volunteer_events.c.is_approved,
        volunteer_events.c.rejection_reason,
        volunteer_events.c.karma_points_earned
    ).join(
        volunteer_events, Event.id == volunteer_events.c.event_id
    ).options(selectinload(Event.images), selectinload(Event.organizer)).filter(
        volunteer_events.c.volunteer_id == current_user.id
        # Temporarily remove the is_approved filter to see all events
        # volunteer_events.c.is_approved == True  # Only include verified/approved events in PDF
    ).all()
    
    logger.info(f"📊 PDF: Found {len(attendance_rows)} events after joining with detailed info")
    for i, (event, hours_worked, is_approved, rejection_reason, karma_points_earned) in enumerate(attendance_rows):
        logger.info(f"  🗓️ PDF Event {i+1}: '{event.title}' (ID: {event.id})")
        logger.info(f"    📍 Location: {event.location}, Date: {event.date}")
        logger.info(f"    ✅ Approved: {is_approved}, Hours: {hours_worked}, Karma: {karma_points_earned}")
        logger.info(f"    📸 Images: {len(event.images) if event.images else 0}")
        logger.info(f"    👤 Organizer: {event.organizer.username if event.organizer else 'None'}")
    
    # Filter for approved events only after logging
    approved_rows = [
        (event, hours_worked, is_approved, rejection_reason, karma_points_earned)
        for event, hours_worked, is_approved, rejection_reason, karma_points_earned in attendance_rows
        if is_approved == True
    ]
    
    logger.info(f"📊 PDF: {len(approved_rows)} approved events will be included in PDF")
    attendance_rows = approved_rows

    def wrap_text(text, font, font_size, max_width):
        """Helper function to wrap text within a given width"""
        if not text:
            return []
        words = text.split()
        lines = []
        current_line = []
        for word in words:
            test_line = ' '.join(current_line + [word])
            if stringWidth(test_line, font, font_size) <= max_width:
                current_line.append(word)
            else:
                if current_line:
                    lines.append(' '.join(current_line))
                    current_line = [word]
                else:
                    lines.append(word)
        if current_line:
            lines.append(' '.join(current_line))
        return lines

    buffer = BytesIO()
    p = canvas.Canvas(buffer, pagesize=letter)
    width, height = letter
    y = height - 80  # Start high for logo
    
    logger.info("📊 PDF: Starting PDF generation")
    
    # App logo placeholder (top left)
    p.setFont("Helvetica-Bold", 16)
    p.drawString(50, y, "🏆 GoodDeedFeed")  # App logo placeholder
    
    # User profile picture placeholder (top right)
    if current_user.profile_picture_url:
        p.setFont("Helvetica", 10)
        p.drawString(width - 200, y, f"Profile: {current_user.profile_picture_url[:30]}...")
        logger.info(f"📊 PDF: Added user profile picture reference: {current_user.profile_picture_url}")
    else:
        p.setFont("Helvetica", 12)
        p.drawString(width - 150, y, "👤 Profile")
        logger.info("📊 PDF: No profile picture for user")
    
    y -= 40

    # Header with much larger font
    p.setFont("Helvetica-Bold", 24)
    p.drawString(50, y, "Volunteer History Report")
    y -= 50

    # Volunteer info with larger fonts and better spacing
    p.setFont("Helvetica-Bold", 18)
    p.drawString(50, y, f"Volunteer: {current_user.full_name or current_user.username}")
    y -= 30
    
    p.setFont("Helvetica", 16)
    if current_user.email:
        p.drawString(50, y, f"Email: {current_user.email}")
        y -= 25
    if current_user.karma_points:
        p.drawString(50, y, f"Total Karma Points: {current_user.karma_points}")
        y -= 25
    y -= 30  # Extra spacing before events

    if not attendance_rows:
        p.setFont("Helvetica", 18)
        p.drawString(50, y, "No volunteer events completed yet.")
        logger.info("📊 PDF: No events to display in PDF")
    else:
        logger.info(f"📊 PDF: Rendering {len(attendance_rows)} events in PDF")
        for i, (event, hours_worked, is_approved, rejection_reason, karma_points_earned) in enumerate(attendance_rows):
            logger.info(f"📊 PDF: Rendering event {i+1}/{len(attendance_rows)}: {event.title}")
            
            # Check if we need a new page - reserve more space for event details
            if y < 300:
                logger.info("📊 PDF: Starting new page")
                p.showPage()
                y = height - 50

            # Event header box
            p.setStrokeColorRGB(0.7, 0.7, 0.7)
            p.setFillColorRGB(0.95, 0.95, 0.95)
            p.rect(40, y-40, width-80, 35, fill=1, stroke=1)
            
            # Event title with larger font
            p.setFillColorRGB(0, 0, 0)  # Reset to black
            p.setFont("Helvetica-Bold", 20)
            p.drawString(50, y-25, f"Event #{i+1}: {event.title}")
            y -= 50

            # Event details with larger fonts and proper spacing
            p.setFont("Helvetica", 16)
            
            # Date and time on same line with proper spacing
            if event.date:
                date_text = f"📅 Date: {event.date}"
                if event.start_time and event.end_time:
                    date_text += f" | ⏰ Time: {event.start_time} - {event.end_time}"
                p.drawString(50, y, date_text)
                y -= 25

            # Location with proper spacing
            if event.location:
                p.drawString(50, y, f"📍 Location: {event.location}")
                y -= 25

            # Category
            if event.category:
                p.drawString(50, y, f"🏷️ Category: {event.category.value.replace('_', ' ').title()}")
                y -= 25

            # Status with proper spacing
            status_text = "✅ Status: Verified and Completed"
            if is_approved is None:
                status_text = "⏳ Status: Pending Approval"
            elif is_approved is False:
                status_text = "❌ Status: Not Approved"
            p.drawString(50, y, status_text)
            y -= 25

            # Hours worked
            if hours_worked is not None:
                p.drawString(50, y, f"⏱️ Hours Worked: {hours_worked}")
                y -= 25

            # Karma points earned
            if karma_points_earned:
                p.drawString(50, y, f"⭐ Karma Points Earned: {karma_points_earned}")
                y -= 25

            # Event description with text wrapping and better formatting
            if event.description:
                y -= 10
                p.setFont("Helvetica-Bold", 16)
                p.drawString(50, y, "📝 Description:")
                y -= 22
                p.setFont("Helvetica", 14)
                
                description_lines = wrap_text(event.description, "Helvetica", 14, width - 100)
                logger.info(f"📊 PDF: Event description wrapped into {len(description_lines)} lines")
                for line in description_lines:
                    if y < 80:
                        p.showPage()
                        y = height - 50
                    p.drawString(70, y, line)
                    y -= 20
                y -= 10

            # Event photos information with enhanced display
            if hasattr(event, 'images') and event.images:
                y -= 10
                p.setFont("Helvetica-Bold", 16)
                p.drawString(50, y, f"📸 Event Photos: {len(event.images)} image(s)")
                y -= 22
                
                logger.info(f"📊 PDF: Event has {len(event.images)} images")
                p.setFont("Helvetica", 12)
                for img_idx, img in enumerate(event.images):
                    if y < 80:
                        p.showPage()
                        y = height - 50
                    
                    # Show image URL and metadata
                    p.drawString(70, y, f"Photo {img_idx+1}: {img.image_url}")
                    y -= 15
                    
                    if hasattr(img, 'is_main') and img.is_main:
                        p.drawString(90, y, "⭐ Main Event Photo")
                        y -= 15
                    
                    # If we have more than 5 images, show abbreviated list
                    if img_idx >= 4 and len(event.images) > 5:
                        remaining = len(event.images) - 5
                        p.drawString(70, y, f"... and {remaining} more photos")
                        y -= 15
                        break
                y -= 10

            # Organizer info with enhanced details
            if hasattr(event, 'organizer') and event.organizer:
                y -= 10
                p.setFont("Helvetica-Bold", 14)
                p.drawString(50, y, "👤 Organized by:")
                y -= 18
                
                p.setFont("Helvetica", 12)
                org_name = event.organizer.organization_name or event.organizer.full_name or event.organizer.username
                p.drawString(70, y, f"Name: {org_name}")
                y -= 15
                
                if event.organizer.organization_description:
                    desc_lines = wrap_text(event.organizer.organization_description, "Helvetica", 12, width - 120)
                    for line in desc_lines[:2]:  # Show first 2 lines only
                        if y < 80:
                            p.showPage()
                            y = height - 50
                        p.drawString(70, y, f"About: {line}")
                        y -= 15
                        break  # Just show first line
                
                logger.info(f"📊 PDF: Added organizer info: {org_name}")

            # Event separator
            y -= 20
            p.setStrokeColorRGB(0.8, 0.8, 0.8)
            p.line(50, y, width-50, y)
            y -= 30  # Extra spacing between events
            
            logger.info(f"📊 PDF: Completed rendering event {i+1}: {event.title}")

    p.save()
    buffer.seek(0)
    
    logger.info(f"PDF generated successfully for user {current_user.username}")
    return StreamingResponse(
        buffer, 
        media_type="application/pdf", 
        headers={"Content-Disposition": "attachment; filename=volunteer_history.pdf"}
    )

@router.get("/users/me/joined-events", response_model=List[EventSchema])
def get_my_joined_events(
    current_user: User = Depends(get_current_active_user), 
    db: Session = Depends(get_db)
):
    """Get events that the current user has joined"""
    logger.info(f"📅 GET JOINED EVENTS - Getting joined events for user {current_user.username} (ID: {current_user.id})")
    
    if current_user.user_type != UserType.VOLUNTEER:
        logger.error(f"❌ GET JOINED EVENTS - User {current_user.username} is not a volunteer (type: {current_user.user_type})")
        raise HTTPException(status_code=403, detail="Only volunteers can have joined events")
    
    # First, let's check what entries exist in volunteer_events for this user
    volunteer_registrations = db.execute(
        select(volunteer_events).where(volunteer_events.c.volunteer_id == current_user.id)
    ).fetchall()
    
    logger.info(f"📊 GET JOINED EVENTS - Found {len(volunteer_registrations)} volunteer_events entries for user {current_user.id}")
    for reg in volunteer_registrations:
        logger.info(f"  📝 Event ID: {reg.event_id}, Status: {getattr(reg, 'status', 'N/A')}, Joined At: {getattr(reg, 'joined_at', 'N/A')}")
    
    # Get joined events
    events = db.query(Event).join(volunteer_events).filter(
        volunteer_events.c.volunteer_id == current_user.id
    ).options(selectinload(Event.images), selectinload(Event.organizer)).all()
    
    logger.info(f"📊 GET JOINED EVENTS - User {current_user.username} has joined {len(events)} events")
    
    # Log details about each joined event
    for event in events:
        logger.info(f"  🗓️ Event '{event.title}' (ID: {event.id}) on {event.date} from {event.start_time} to {event.end_time}")
        logger.info(f"    📍 Location: {event.location}, Category: {event.category}")
        logger.info(f"    👥 Volunteers: {event.current_volunteers}/{event.max_volunteers or 'unlimited'}")
    
    # Populate organizer names for all events
    for event in events:
        populate_organizer_name(event)
    
    logger.info(f"✅ GET JOINED EVENTS - Returning {len(events)} events for user {current_user.username}")
    return events

@router.get("/organizers/{organizer_id}/events", response_model=List[EventSchema])
def list_events_by_organizer(organizer_id: int, q: str | None = None, db: Session = Depends(get_db)):
    query = db.query(Event).options(selectinload(Event.images), selectinload(Event.organizer)).filter(Event.organizer_id == organizer_id)
    if q:
        like_pattern = f"%{q}%"
        query = query.filter(Event.title.ilike(like_pattern))

    events = query.all()
    
    # Populate organizer names for all events
    for event in events:
        populate_organizer_name(event)
    
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
    
    # Populate usernames for each message
    result = []
    for msg in msgs:
        sender = db.query(User).filter(User.id == msg.sender_id).first()
        receiver = db.query(User).filter(User.id == msg.receiver_id).first()
        
        msg_data = MessageOut(
            id=msg.id,
            sender_id=msg.sender_id,
            receiver_id=msg.receiver_id,
            content=msg.content,
            sender_username=sender.username if sender else "Unknown",
            receiver_username=receiver.username if receiver else "Unknown",
            created_at=msg.created_at,
            is_read=msg.is_read,
            is_important_sender=msg.is_important_sender,
            is_important_receiver=msg.is_important_receiver,
            is_deleted_sender=msg.is_deleted_sender,
            is_deleted_receiver=msg.is_deleted_receiver,
            reactions=[],
        )
        result.append(msg_data)
    
    return result

@router.post("/messages", response_model=MessageOut)
async def send_message(payload: MessageCreate, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    # Validate receiver exists to avoid FK errors
    receiver = db.get(User, payload.receiver_id)
    if not receiver:
        raise HTTPException(status_code=404, detail="Receiver user not found")
 
    # Create message with current user as sender
    msg = Message(
        sender_id=current_user.id,
        receiver_id=payload.receiver_id,
        content=payload.content
    )
    try:
        db.add(msg)
        db.commit()
    except Exception as e:
        db.rollback()
        # Log and re-raise as proper HTTPException to ensure JSON error response
        logger.error(f"Failed to create message: {e}")
        raise HTTPException(status_code=500, detail="Failed to create message")

    db.refresh(msg)
    
    # Send push notification to receiver if they have notifications enabled
    try:
        await send_message_notification(
            sender_name=current_user.full_name or current_user.username,
            sender_id=current_user.id,
            receiver_id=receiver.id,
            message_content=msg.content,
            db=db
        )
    except Exception as e:
        # Don't fail message sending if notification fails
        logger.error(f"Failed to send message notification: {e}")
    
    # Create in-app notification for receiver
    try:
        await create_message_in_app_notification(
            sender_name=current_user.full_name or current_user.username,
            sender_id=current_user.id,
            receiver_id=receiver.id,
            message_content=msg.content,
            message_id=msg.id,
            db=db
        )
    except Exception as e:
        # Don't fail message sending if in-app notification fails
        logger.error(f"Failed to create message in-app notification: {e}")
    
    return MessageOut(
        id=msg.id,
        sender_id=msg.sender_id,
        receiver_id=msg.receiver_id,
        content=msg.content,
        sender_username=current_user.username,
        receiver_username=receiver.username,
        created_at=msg.created_at,
        is_read=msg.is_read,
        is_important_sender=msg.is_important_sender,
        is_important_receiver=msg.is_important_receiver,
        is_deleted_sender=msg.is_deleted_sender,
        is_deleted_receiver=msg.is_deleted_receiver,
        reactions=[],
    )



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
        "name": "First Steps",
        "description": "Earn 200 karma points to take your first steps!",
        "required": 200,
        "icon": "Star",
        "color": "#FFD700",
    },
    {
        "name": "Rising Star",
        "description": "Earn 400 karma points as a rising star!",
        "required": 400,
        "icon": "WorkspacePremium",
        "color": "#FFC107",
    },
    {
        "name": "Community Helper",
        "description": "Earn 600 karma points helping the community!",
        "required": 600,
        "icon": "LocalFireDepartment",
        "color": "#FFB300",
    },
    {
        "name": "Karma Champion",
        "description": "Earn 800 karma points as a champion!",
        "required": 800,
        "icon": "EmojiEvents",
        "color": "#FFA000",
    },
    {
        "name": "Karma Legend",
        "description": "Earn 1000 karma points becoming a legend!",
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

async def send_event_update_notification(
    event_id: int,
    event_title: str,
    organizer_name: str,
    changes: List[str],
    db: Session
):
    """Helper function to send notifications when an event is updated"""
    try:
        # Get all volunteers who joined this event with FCM tokens and notifications enabled
        joined_volunteers = db.query(User).join(
            volunteer_events,
            User.id == volunteer_events.c.volunteer_id
        ).filter(
            volunteer_events.c.event_id == event_id,
            User.fcm_token.isnot(None),
            User.notifications_enabled == True
        ).all()
        
        if not joined_volunteers:
            logger.info(f"No joined volunteers with notifications found for event {event_id}")
            return
        
        # Extract FCM tokens
        fcm_tokens = [user.fcm_token for user in joined_volunteers if user.fcm_token]
        
        if not fcm_tokens:
            logger.info(f"No valid FCM tokens found for joined volunteers of event {event_id}")
            return
        
        # Create notification message
        change_summary = ", ".join(changes[:2])  # Show first 2 changes
        if len(changes) > 2:
            change_summary += f" and {len(changes) - 2} more"
        
        # Send notifications
        results = firebase_service.send_notification_to_multiple_tokens(
            tokens=fcm_tokens,
            title="Event Update",
            body=f"{event_title} has been updated: {change_summary}",
            data={
                "type": "event_update",
                "eventId": str(event_id),
                "organizerName": organizer_name,
                "eventTitle": event_title,
                "changes": "; ".join(changes)
            }
        )
        
        # Count successful sends
        successful_sends = sum(1 for success in results.values() if success)
        total_attempts = len(fcm_tokens)
        
        logger.info(f"Event update notification sent: {successful_sends}/{total_attempts} successful for event {event_id}")
        
    except Exception as e:
        logger.error(f"Failed to send event update notification: {e}")

async def create_event_update_in_app_notifications(
    event_id: int,
    event_title: str,
    organizer_name: str,
    changes: List[str],
    db: Session
):
    """Helper function to create in-app notifications when an event is updated"""
    try:
        # Get all volunteers who joined this event
        joined_volunteers = db.query(User).join(
            volunteer_events,
            User.id == volunteer_events.c.volunteer_id
        ).filter(
            volunteer_events.c.event_id == event_id
        ).all()
        
        if not joined_volunteers:
            logger.info(f"No joined volunteers found for event {event_id}")
            return
        
        # Create notification message
        change_summary = ", ".join(changes[:2])  # Show first 2 changes
        if len(changes) > 2:
            change_summary += f" and {len(changes) - 2} more"
        
        # Create in-app notifications for each joined volunteer
        notifications_created = 0
        for volunteer in joined_volunteers:
            try:
                in_app_notification = InAppNotification(
                    user_id=volunteer.id,
                    title="Event Update",
                    message=f"{event_title} has been updated: {change_summary}",
                    data={
                        "type": "event_update",
                        "eventId": str(event_id),
                        "organizerName": organizer_name,
                        "eventTitle": event_title,
                        "changes": "; ".join(changes)
                    }
                )
                db.add(in_app_notification)
                notifications_created += 1
            except Exception as e:
                logger.error(f"Failed to create event update in-app notification for user {volunteer.id}: {e}")
        
        db.commit()
        logger.info(f"Created {notifications_created} event update in-app notifications for event {event_id}")
        
    except Exception as e:
        logger.error(f"Failed to create event update in-app notifications: {e}")
        db.rollback()

async def send_message_notification(
    sender_name: str,
    sender_id: int,
    receiver_id: int,
    message_content: str,
    db: Session
):
    """Helper function to send notifications when a message is sent"""
    try:
        # Get receiver with FCM token and notification preferences
        receiver = db.query(User).filter(
            User.id == receiver_id,
            User.fcm_token.isnot(None),
            User.notifications_enabled == True
        ).first()
        
        if not receiver or not receiver.fcm_token:
            logger.info(f"Receiver {receiver_id} has no FCM token or notifications disabled")
            return
        
        # Truncate message content for notification preview
        preview = message_content[:50] + "..." if len(message_content) > 50 else message_content
        
        # Send notification
        success = firebase_service.send_notification_to_token(
            token=receiver.fcm_token,
            title=f"Message from {sender_name}",
            body=preview,
            data={
                "type": "new_message",
                "senderId": str(sender_id),
                "senderName": sender_name,
                "messagePreview": preview
            }
        )
        
        if success:
            logger.info(f"Message notification sent successfully to user {receiver_id}")
        else:
            logger.warning(f"Failed to send message notification to user {receiver_id}")
        
    except Exception as e:
        logger.error(f"Failed to send message notification: {e}")

async def create_message_in_app_notification(
    sender_name: str,
    sender_id: int,
    receiver_id: int,
    message_content: str,
    message_id: int,
    db: Session
):
    """Helper function to create in-app notifications when a message is sent"""
    try:
        # Truncate message content for notification preview
        preview = message_content[:100] + "..." if len(message_content) > 100 else message_content
        
        # Create in-app notification for receiver
        in_app_notification = InAppNotification(
            user_id=receiver_id,
            title=f"Message from {sender_name}",
            message=preview,
            data={
                "type": "new_message",
                "senderId": str(sender_id),
                "senderName": sender_name,
                "messageId": str(message_id),
                "messagePreview": preview
            }
        )
        
        db.add(in_app_notification)
        db.commit()
        
        logger.info(f"Created message in-app notification for user {receiver_id}")
        
    except Exception as e:
        logger.error(f"Failed to create message in-app notification: {e}")
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
        # Check badges (but don't return the result here since this is a dev endpoint)
        try:
            # Call the badge check function inline to ensure badges are awarded
            existing_badge_ids = db.execute(
                text("SELECT badge_id FROM user_badges WHERE user_id = :user_id"),
                {"user_id": current_user.id}
            ).fetchall()
            existing_ids = [row[0] for row in existing_badge_ids]
            
            # Get badges user can earn based on karma points
            available_badges = db.query(Badge).filter(
                Badge.is_active == True,
                Badge.required_karma_points <= current_user.karma_points,
                ~Badge.id.in_(existing_ids) if existing_ids else True
            ).all()
            
            # Award new badges
            for badge in available_badges:
                db.execute(
                    text("INSERT INTO user_badges (user_id, badge_id, earned_at) VALUES (:user_id, :badge_id, NOW())"),
                    {"user_id": current_user.id, "badge_id": badge.id}
                )
                logger.info(f"User {current_user.username} earned badge: {badge.name}")
        except Exception as e:
            logger.warning(f"Badge check failed after karma increase: {e}")

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


# ===== BADGE ENDPOINTS =====

@router.get("/badges", response_model=List[BadgeSchema])
async def get_all_badges(db: Session = Depends(get_db)):
    """Get all available badges"""
    try:
        badges = db.query(Badge).filter(Badge.is_active == True).all()
        return badges
    except Exception as e:
        logger.error(f"Failed to get badges: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get badges"
        )

# ===== LOST & FOUND ENDPOINTS =====

@router.post("/lost-found", response_model=schemas.LostFoundItemOut)
async def create_lost_found_item(
    item_data: schemas.LostFoundItemCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new lost or found item"""
    try:
        from datetime import datetime, timedelta
        
        logger.info(f"Creating lost/found item - User: {current_user.username} (ID: {current_user.id})")
        logger.info(f"Item data - Title: {item_data.title}, Type: {item_data.item_type}, Location: {item_data.location}")
        logger.info(f"Item data - Expiry days: {item_data.expiry_days}, Tags: {item_data.tags}")
        
        # Calculate expiry date
        expires_at = datetime.utcnow() + timedelta(days=item_data.expiry_days)
        logger.info(f"Calculated expiry date: {expires_at}")
        
        # Create the lost/found item
        db_item = LostFoundItem(
            user_id=current_user.id,
            title=item_data.title,
            description=item_data.description,
            location=item_data.location,
            item_type=item_data.item_type.value,
            reward=item_data.reward,
            tags=item_data.tags,
            expiry_days=item_data.expiry_days,
            expires_at=expires_at
        )
        
        logger.info(f"Created LostFoundItem object with item_type: {db_item.item_type}")
        
        db.add(db_item)
        logger.info("Added item to database session")
        
        db.commit()
        logger.info("Committed transaction to database")
        
        db.refresh(db_item)
        logger.info(f"Successfully created lost/found item with ID: {db_item.id}")
        logger.info(f"Item details - is_active: {db_item.is_active}, created_at: {db_item.created_at}, expires_at: {db_item.expires_at}")
        
        # Convert to response format
        response = convert_lost_found_item_to_out(db_item, current_user)
        logger.info(f"Converted to response format, returning item with ID: {response.id}")
        return response
        
    except Exception as e:
        logger.error(f"Failed to create lost/found item: {e}")
        logger.error(f"Exception type: {type(e).__name__}")
        import traceback
        logger.error(f"Full traceback: {traceback.format_exc()}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create lost/found item"
        )


@router.get("/lost-found", response_model=schemas.LostFoundItemsResponse)
async def get_lost_found_items(
    item_type: Optional[str] = None,
    limit: int = Query(50, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
):
    """List lost & found items. Authentication optional."""
    # Try to identify user, but ignore authentication errors
    current_user: Optional[User] = None
    if credentials:
        try:
            current_user = await get_current_user(credentials, db)
        except Exception:
            current_user = None
    
    try:
        from datetime import datetime
        
        current_time = datetime.utcnow()
        logger.info(f"Getting lost/found items - User: {current_user.username if current_user else 'Unauthenticated'} (ID: {current_user.id if current_user else 'N/A'})")
        logger.info(f"Query parameters - item_type: {item_type}, limit: {limit}, offset: {offset}")
        logger.info(f"Current UTC time for expiry check: {current_time}")
        
        # First, let's see all items in the database
        all_items_query = db.query(LostFoundItem)
        all_items_count = all_items_query.count()
        logger.info(f"Total items in LostFoundItem table: {all_items_count}")
        
        if all_items_count > 0:
            # Log some details about all items
            all_items = all_items_query.all()
            for item in all_items:
                logger.info(f"Item ID: {item.id}, is_active: {item.is_active}, expires_at: {item.expires_at}, created_at: {item.created_at}, item_type: {item.item_type}")
        
        query = db.query(LostFoundItem).filter(
            LostFoundItem.is_active == True,
            LostFoundItem.expires_at > current_time
        )
        
        active_unexpired_count = query.count()
        logger.info(f"Active and unexpired items count: {active_unexpired_count}")
        
        if item_type:
            logger.info(f"Filtering by item_type: {item_type}")
            query = query.filter(LostFoundItem.item_type == item_type)
            filtered_count = query.count()
            logger.info(f"Items after type filtering: {filtered_count}")
        
        # Get total count
        total_count = query.count()
        logger.info(f"Total count after all filters: {total_count}")
        
        # Get paginated results
        items = query.order_by(LostFoundItem.created_at.desc()).offset(offset).limit(limit).all()
        logger.info(f"Retrieved {len(items)} items from database")
        
        # Convert to response format
        items_out = []
        for item in items:
            logger.info(f"Processing item ID: {item.id}, title: {item.title}")
            user = db.query(User).filter(User.id == item.user_id).first()
            if user:
                logger.info(f"Found user {user.username} for item {item.id}")
                items_out.append(convert_lost_found_item_to_out(item, user))
            else:
                logger.warning(f"No user found for item {item.id} with user_id {item.user_id}")
        
        logger.info(f"Returning {len(items_out)} items in response")
        
        response = schemas.LostFoundItemsResponse(
            items=items_out,
            total_count=total_count
        )
        
        logger.info(f"Final response - items count: {len(response.items)}, total_count: {response.total_count}")
        return response
        
    except Exception as e:
        logger.error(f"Failed to get lost/found items: {e}")
        logger.error(f"Exception type: {type(e).__name__}")
        import traceback
        logger.error(f"Full traceback: {traceback.format_exc()}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get lost/found items"
        )


@router.get("/lost-found/{item_id}", response_model=schemas.LostFoundItemOut)
async def get_lost_found_item(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific lost/found item by ID"""
    try:
        item = db.query(LostFoundItem).filter(
            LostFoundItem.id == item_id,
            LostFoundItem.is_active == True
        ).first()
        
        if not item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lost/found item not found"
            )
        
        user = db.query(User).filter(User.id == item.user_id).first()
        return convert_lost_found_item_to_out(item, user)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get lost/found item: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get lost/found item"
        )


@router.put("/lost-found/{item_id}", response_model=schemas.LostFoundItemOut)
async def update_lost_found_item(
    item_id: int,
    item_data: schemas.LostFoundItemUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a lost/found item (only by owner)"""
    try:
        item = db.query(LostFoundItem).filter(
            LostFoundItem.id == item_id,
            LostFoundItem.user_id == current_user.id
        ).first()
        
        if not item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lost/found item not found or not owned by user"
            )
        
        # Update fields
        update_data = item_data.dict(exclude_unset=True)
        for field, value in update_data.items():
            setattr(item, field, value)
        
        db.commit()
        db.refresh(item)
        
        logger.info(f"Updated lost/found item {item_id} by user {current_user.username}")
        
        return convert_lost_found_item_to_out(item, current_user)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to update lost/found item: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update lost/found item"
        )


@router.delete("/lost-found/{item_id}")
async def delete_lost_found_item(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a lost/found item (only by owner)"""
    try:
        item = db.query(LostFoundItem).filter(
            LostFoundItem.id == item_id,
            LostFoundItem.user_id == current_user.id
        ).first()
        
        if not item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lost/found item not found or not owned by user"
            )
        
        # Soft delete by setting is_active to False
        item.is_active = False
        db.commit()
        
        logger.info(f"Deleted lost/found item {item_id} by user {current_user.username}")
        
        return {"message": "Lost/found item deleted successfully"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to delete lost/found item: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete lost/found item"
        )


@router.post("/lost-found/{item_id}/images")
async def upload_lost_found_image(
    item_id: int,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload an image for a lost/found item (only by owner)"""
    try:
        # Check if item exists and is owned by user
        item = db.query(LostFoundItem).filter(
            LostFoundItem.id == item_id,
            LostFoundItem.user_id == current_user.id
        ).first()
        
        if not item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lost/found item not found or not owned by user"
            )
        
        # Check if item already has maximum images (10)
        current_image_count = db.query(LostFoundImage).filter(
            LostFoundImage.lost_found_item_id == item_id
        ).count()
        
        if current_image_count >= 10:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Maximum of 10 images allowed per item"
            )
        
        # Validate file type
        if not file.content_type.startswith('image/'):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Only image files are allowed"
            )
        
        # Save file and get URL (using same logic as event images)
        file_url = await save_lost_found_image(file, item_id)
        
        # Create image record
        db_image = LostFoundImage(
            lost_found_item_id=item_id,
            image_url=file_url
        )
        
        db.add(db_image)
        db.commit()
        db.refresh(db_image)
        
        logger.info(f"Uploaded image for lost/found item {item_id} by user {current_user.username}")
        
        return {
            "message": "Image uploaded successfully",
            "image_url": file_url,
            "image_id": db_image.id
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to upload lost/found image: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to upload image"
        )


@router.delete("/lost-found/{item_id}/images/{image_id}")
async def delete_lost_found_image(
    item_id: int,
    image_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete an image from a lost/found item (only by owner)"""
    try:
        # Check if item exists and is owned by user
        item = db.query(LostFoundItem).filter(
            LostFoundItem.id == item_id,
            LostFoundItem.user_id == current_user.id
        ).first()
        
        if not item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lost/found item not found or not owned by user"
            )
        
        # Check if image exists for this item
        image = db.query(LostFoundImage).filter(
            LostFoundImage.id == image_id,
            LostFoundImage.lost_found_item_id == item_id
        ).first()
        
        if not image:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Image not found"
            )
        
        # Delete the image file (implement cleanup if needed)
        # await delete_lost_found_image_file(image.image_url)
        
        # Delete the image record
        db.delete(image)
        db.commit()
        
        logger.info(f"Deleted image {image_id} from lost/found item {item_id} by user {current_user.username}")
        
        return {"message": "Image deleted successfully"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to delete lost/found image: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete image"
        )


async def save_lost_found_image(file: UploadFile, item_id: int) -> str:
    """Save uploaded image for lost/found item and return URL"""
    import os
    import uuid
    from pathlib import Path
    
    try:
        # Create upload directory if it doesn't exist
        upload_dir = Path("uploads/lost_found")
        upload_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate unique filename
        file_extension = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
        unique_filename = f"{item_id}_{uuid.uuid4()}.{file_extension}"
        file_path = upload_dir / unique_filename
        
        # Save file
        content = await file.read()
        with open(file_path, "wb") as f:
            f.write(content)
        
        # Return URL (adjust based on your file serving setup)
        return f"/uploads/lost_found/{unique_filename}"
        
    except Exception as e:
        logger.error(f"Failed to save lost/found image: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to save image"
        )


def convert_lost_found_item_to_out(item: LostFoundItem, user: User) -> schemas.LostFoundItemOut:
    """Helper function to convert LostFoundItem to LostFoundItemOut schema"""
    from datetime import datetime
    
    # Calculate days remaining
    days_remaining = None
    if item.expires_at:
        delta = item.expires_at - datetime.now(timezone.utc)
        days_remaining = max(0, delta.days)
    
    # Get contact name
    contact_name = None
    contact_phone = None
    contact_email = None
    if user.user_type == "volunteer":
        contact_name = user.full_name or user.username
        contact_phone = user.phone
        contact_email = user.email
    elif user.user_type == "organizer":
        contact_name = user.organization_name or user.username
        contact_phone = user.phone
        contact_email = user.email
    else:
        contact_name = user.username
    
    # Get image URLs
    image_urls = [img.image_url for img in item.images] if item.images else []
    
    # Convert item_type string to enum safely
    try:
        item_type_enum = schemas.LostFoundType(item.item_type)
    except ValueError:
        # If the database contains invalid enum value, default to "lost"
        logger.warning(f"Invalid item_type '{item.item_type}' for item {item.id}, defaulting to 'lost'")
        item_type_enum = schemas.LostFoundType.LOST
    
    return schemas.LostFoundItemOut(
        id=item.id,
        user_id=item.user_id,
        title=item.title,
        description=item.description,
        location=item.location,
        item_type=item_type_enum,
        reward=item.reward,
        tags=item.tags or [],
        expiry_days=item.expiry_days,
        created_at=item.created_at,
        expires_at=item.expires_at,
        is_resolved=item.is_resolved,
        is_active=item.is_active,
        images=image_urls,
        contact_name=contact_name,
        contact_phone=contact_phone,
        contact_email=contact_email,
        days_remaining=days_remaining
    )


@router.post("/lost-found/cleanup-expired")
async def cleanup_expired_lost_found_items(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Cleanup expired lost/found items (admin only or can be called as background task)"""
    try:
        from datetime import datetime
        
        # Find expired items
        expired_items = db.query(LostFoundItem).filter(
            LostFoundItem.is_active == True,
            LostFoundItem.expires_at < datetime.now(timezone.utc)
        ).all()
        
        if not expired_items:
            return {"message": "No expired items to cleanup", "count": 0}
        
        # Deactivate expired items
        for item in expired_items:
            item.is_active = False
        
        db.commit()
        
        logger.info(f"Cleaned up {len(expired_items)} expired lost/found items")
        
        return {
            "message": f"Successfully cleaned up {len(expired_items)} expired items",
            "count": len(expired_items)
        }
        
    except Exception as e:
        logger.error(f"Failed to cleanup expired lost/found items: {e}")
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to cleanup expired items"
        )


def schedule_lost_found_cleanup():
    """Background task to automatically cleanup expired lost/found items"""
    import asyncio
    from datetime import datetime
    from .database import SessionLocal
    
    async def cleanup_task():
        db = SessionLocal()
        try:
            # Find expired items
            expired_items = db.query(LostFoundItem).filter(
                LostFoundItem.is_active == True,
                LostFoundItem.expires_at < datetime.now(timezone.utc)
            ).all()
            
            if expired_items:
                # Deactivate expired items
                for item in expired_items:
                    item.is_active = False
                
                db.commit()
                logger.info(f"Background cleanup: deactivated {len(expired_items)} expired lost/found items")
            
        except Exception as e:
            logger.error(f"Background cleanup failed: {e}")
            db.rollback()
        finally:
            db.close()
    
    # Run the cleanup task
    asyncio.create_task(cleanup_task())

@router.get("/users/{user_id}", response_model=schemas.User)
def get_user_by_id(user_id: int, db: Session = Depends(get_db)):
    """Public endpoint to fetch a user's basic profile by ID (no auth required)."""
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

@router.delete("/events/{event_id}/volunteers/{volunteer_id}")
async def kick_volunteer_from_event(
    event_id: int,
    volunteer_id: int,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Kick (remove) a volunteer from an event. Only event organizer can perform this action."""
    logger.info(
        f"Organizer {current_user.username} (ID: {current_user.id}) attempts to kick volunteer {volunteer_id} from event {event_id}"
    )

    # Verify user is an organizer
    if current_user.user_type != UserType.ORGANIZER:
        logger.error("Current user is not an organizer")
        raise HTTPException(status_code=403, detail="Only organizers can perform this action")

    # Verify event exists and belongs to current organizer
    event = db.get(Event, event_id)
    if not event:
        logger.error(f"Event {event_id} not found")
        raise HTTPException(status_code=404, detail="Event not found")

    if event.organizer_id != current_user.id:
        logger.error(
            f"Organizer {current_user.id} is not owner of event {event_id} (owner: {event.organizer_id})"
        )
        raise HTTPException(status_code=403, detail="Not allowed to modify this event")

    # Check volunteer exists in event
    existing_join = db.execute(
        select(volunteer_events).where(
            volunteer_events.c.volunteer_id == volunteer_id,
            volunteer_events.c.event_id == event_id,
        )
    ).first()

    if not existing_join:
        logger.warning(f"Volunteer {volunteer_id} is not joined to event {event_id}")
        raise HTTPException(status_code=400, detail="Volunteer not part of this event")

    try:
        # Delete association
        db.execute(
            volunteer_events.delete().where(
                volunteer_events.c.volunteer_id == volunteer_id,
                volunteer_events.c.event_id == event_id,
            )
        )

        # Decrement volunteer count safely
        if event.current_volunteers and event.current_volunteers > 0:
            event.current_volunteers -= 1

        db.commit()
        logger.info(f"Volunteer {volunteer_id} removed from event {event_id}")
        return {"message": "Volunteer removed from event", "current_volunteers": event.current_volunteers}

    except Exception as e:
        logger.error(f"Failed to remove volunteer {volunteer_id} from event {event_id}: {e}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Failed to remove volunteer from event")

# -------------------- Event Description Suggestion --------------------

@router.get("/events/description/suggestion")
async def get_event_description_suggestion(
    title: str,
    current_user: User = Depends(get_current_active_user),
):
    """Generate a concise, structured event description using OpenAI GPT."""

    if not title or not title.strip():
        raise HTTPException(status_code=400, detail="Event title is required")

    prompt = (
        "Generate a concise (80–120 words) volunteer-event description titled '" + title + "'. "
        "Structure: Overview, Objectives, Volunteer Tasks, Impact, Call to Action. "
        "Write in plain language, no numbering, persuasive tone."
    )

    suggested = _call_openai(prompt, max_tokens=160)
    return {"suggested_description": suggested}

# -------------------- Opportunity Idea Suggestion --------------------

@router.get("/events/idea/suggestion")
async def get_opportunity_idea_suggestion(
    current_user: User = Depends(get_current_active_user),
):
    """Generate creative volunteer opportunity ideas using OpenAI GPT."""
    
    prompt = (
        "Suggest three unique, engaging volunteer opportunity ideas for community organizers. "
        "Respond as a simple bullet list (no numbering). Keep each idea less than 10 words."
    )

    ideas_text = _call_openai(prompt, max_tokens=120)

    # Split into list, keep non-empty lines
    ideas = [line.lstrip("-•* ").strip() for line in ideas_text.split("\n") if line.strip()]

    # Ensure max 3 ideas
    ideas = ideas[:3]

    return {"ideas": ideas}
