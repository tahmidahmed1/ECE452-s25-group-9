from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta
import logging

from .database import get_db
from .models import User
from .schemas import (
    UserCreate, User as UserSchema, Token, OnboardingStepOne, 
    OnboardingComplete, UserType, InstitutionName, ProfilePictureUploadResponse
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

@router.post("/register", response_model=Token)
def register_user(user: UserCreate, db: Session = Depends(get_db)):
    logger.info(f"Registration request received for username: {user.username}, email: {user.email}")
    
    # Check if user already exists
    db_user = db.query(User).filter(User.username == user.username).first()
    if db_user:
        logger.warning(f"Registration failed: Username {user.username} already exists")
        raise HTTPException(status_code=400, detail="Username already registered")
    
    db_user = db.query(User).filter(User.email == user.email).first()
    if db_user:
        logger.warning(f"Registration failed: Email {user.email} already exists")
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Create new user (onboarding_completed defaults to False)
    hashed_password = get_password_hash(user.password)
    db_user = User(
        username=user.username,
        email=user.email,
        hashed_password=hashed_password
    )
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

@router.get("/institutions")
def get_institutions():
    """Get available institutions for onboarding"""
    return [{"value": inst.value, "label": inst.value} for inst in InstitutionName] 