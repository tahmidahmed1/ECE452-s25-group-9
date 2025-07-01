from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, WebSocket, WebSocketDisconnect
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta
import logging
from typing import List, Dict
from fastapi.responses import JSONResponse
from sqlalchemy import or_

from .database import get_db
from .models import User, UserType, InstitutionName, Sex
from .schemas import (
    UserCreate, User as UserSchema, Token, OnboardingStepOne, 
    OnboardingComplete, ProfilePictureUploadResponse, EventSchema,
    InstitutionName as SchemaInstitutionName,
    UserUpdate
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
                # Map the user type
                if user_type_str == "VOLUNTEER":
                    db_user.user_type = UserType.VOLUNTEER
                    db_user.full_name = f"Dev Volunteer {parts[2] if len(parts) > 2 else 'User'}"
                    db_user.phone = f"+1-555-DEV-{parts[2] if len(parts) > 2 else '123'}"
                elif user_type_str == "ORGANIZER":
                    db_user.user_type = UserType.ORGANIZER
                    db_user.full_name = f"Dev Organizer {parts[2] if len(parts) > 2 else 'User'}"
                    db_user.phone = f"+1-555-DEV-{parts[2] if len(parts) > 2 else '123'}"
                    db_user.organization_name = f"Dev Organization {parts[2] if len(parts) > 2 else '123'}"
                elif user_type_str == "INSTITUTION":
                    db_user.user_type = UserType.INSTITUTION
                    db_user.full_name = f"Dev Institution {parts[2] if len(parts) > 2 else 'User'}"
                    db_user.phone = f"+1-555-DEV-{parts[2] if len(parts) > 2 else '123'}"
                    db_user.institution_name = InstitutionName.INSTITUTION_1
                    
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
    logger.info(f"Profile picture upload request from user: {current_user.username}")
    
    # Delete old profile picture if exists
    if current_user.profile_picture_url:
        storage_service.delete_profile_picture(current_user.profile_picture_url)
    
    # Upload new profile picture
    profile_picture_url = await storage_service.upload_profile_picture(file, current_user.id)
    
    # Update user's profile picture URL in database
    current_user.profile_picture_url = profile_picture_url
    db.commit()
    db.refresh(current_user)
    
    logger.info(f"Profile picture uploaded successfully for user: {current_user.username}")
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
    current_user.full_name = onboarding_data.full_name
    current_user.phone = onboarding_data.phone
    current_user.onboarding_completed = True
    
    if onboarding_data.organization_name:
        current_user.organization_name = onboarding_data.organization_name
    elif onboarding_data.institution_name:
        current_user.institution_name = onboarding_data.institution_name
    
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

@router.get("/institutions")
def get_institutions():
    """Get available institutions for onboarding"""
    return [{"value": inst.value, "label": inst.value} for inst in SchemaInstitutionName] 

# Stub endpoint for fetching events
@router.get("/events", response_model=List[EventSchema])
def get_events_stub():
    return []

# CRUD stub endpoints for events
@router.post("/events", response_model=EventSchema)
def create_event_stub(event: EventSchema):
    return event

@router.put("/events/{event_id}", response_model=EventSchema)
def update_event_stub(event_id: int, event: EventSchema):
    return event

@router.delete("/events/{event_id}", status_code=204)
def delete_event_stub(event_id: int):
    return None 

# ------------------ Profile Edit ------------------

@router.put("/users/me", response_model=UserSchema)
def update_user_profile(
    updates: UserUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Update fields of the currently authenticated user. Only provided fields are updated."""
    logger.info(f"Profile update requested by {current_user.username}")

    # Iterate over update fields and set attributes if not None
    update_dict = updates.dict(exclude_unset=True)
    for field, value in update_dict.items():
        # Convert Enum values to correct DB enums if necessary
        if field == "sex" and value is not None:
            try:
                value = Sex[value.upper()]
            except KeyError:
                pass
        if field == "institution_name" and value is not None:
            try:
                value = InstitutionName[value]
            except KeyError:
                pass

        setattr(current_user, field, value)

    db.commit()
    db.refresh(current_user)

    logger.info(f"Profile updated successfully for {current_user.username}")
    return current_user 

@router.post("/events", response_model=EventSchema)
async def create_event(event: EventSchema, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    if current_user.user_type != UserType.ORGANIZER:
        raise HTTPException(status_code=403, detail="Only organizers can create events")
    db_event = EventSchema(**event.dict(), organizer_id=current_user.id)
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return db_event

@router.get("/events/{event_id}", response_model=EventSchema)
async def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.get(EventSchema, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    return event

@router.patch("/events/{event_id}", response_model=EventSchema)
async def update_event(event_id: int, payload: EventSchema, current_user: User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    event = db.get(EventSchema, event_id)
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
    event = db.get(EventSchema, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.organizer_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")
    db.delete(event)
    db.commit()
    return {"detail": "deleted"}

@router.get("/organizers/{organizer_id}/events", response_model=list[EventSchema])
async def list_events(organizer_id: int, db: Session = Depends(get_db)):
    return db.query(EventSchema).filter(EventSchema.organizer_id == organizer_id).all()

@router.websocket("/ws/chat/{room_id}")
async def chat_endpoint(websocket: WebSocket, room_id: int, db: Session = Depends(get_db)):
    await manager.connect(room_id, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            # data: {"sender_id":.., "receiver_id":.., "content":..}
            msg = EventSchema(**data)
            db.add(msg)
            db.commit()
            db.refresh(msg)
            await manager.broadcast(room_id, msg.dict())
    except WebSocketDisconnect:
        manager.disconnect(room_id, websocket) 
