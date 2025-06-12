from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from enum import Enum

# Enums
class UserType(str, Enum):
    VOLUNTEER = "volunteer"
    ORGANIZER = "organizer"
    INSTITUTION = "institution"

class InstitutionName(str, Enum):
    INSTITUTION_1 = "Institution 1"
    INSTITUTION_2 = "Institution 2"
    INSTITUTION_3 = "Institution 3"

# User schemas
class UserBase(BaseModel):
    username: str
    email: EmailStr

class UserCreate(UserBase):
    password: str

class User(UserBase):
    id: int
    is_active: bool
    user_type: Optional[UserType] = None
    onboarding_completed: bool = False
    full_name: Optional[str] = None
    phone: Optional[str] = None
    profile_picture_url: Optional[str] = None
    organization_name: Optional[str] = None
    institution_name: Optional[InstitutionName] = None

    class Config:
        from_attributes = True

# Onboarding schemas
class OnboardingStepOne(BaseModel):
    user_type: UserType

class OnboardingStepTwoVolunteer(BaseModel):
    full_name: str
    phone: str

class OnboardingStepTwoOrganizer(BaseModel):
    full_name: str
    phone: str
    organization_name: str

class OnboardingStepTwoInstitution(BaseModel):
    full_name: str
    phone: str
    institution_name: InstitutionName

class OnboardingComplete(BaseModel):
    full_name: str
    phone: str
    organization_name: Optional[str] = None
    institution_name: Optional[InstitutionName] = None

# Authentication schemas
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: Optional[str] = None

class Institution(BaseModel):
    name: str
    value: str

class ProfilePictureUploadResponse(BaseModel):
    profile_picture_url: str
    message: str 