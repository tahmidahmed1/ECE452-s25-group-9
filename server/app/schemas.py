from pydantic import BaseModel, EmailStr
from typing import Optional, List
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

class Sex(str, Enum):
    MALE = "male"
    FEMALE = "female"
    NON_BINARY = "non_binary"
    PREFER_NOT_TO_SAY = "prefer_not_to_say"

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
    
    # Enhanced volunteer profile fields
    sex: Optional[Sex] = None
    description: Optional[str] = None
    skills: Optional[List[str]] = None
    age: Optional[int] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    location_area: Optional[str] = None
    has_drivers_license: Optional[bool] = None
    disabilities: Optional[str] = None

    class Config:
        from_attributes = True

# Onboarding schemas
class OnboardingStepOne(BaseModel):
    user_type: UserType

class OnboardingStepTwoVolunteer(BaseModel):
    full_name: str
    phone: str
    sex: Sex
    description: str
    skills: List[str]
    age: int
    emergency_contact_name: str
    emergency_contact_phone: str
    location_area: str
    has_drivers_license: bool
    disabilities: Optional[str] = None

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
    # Volunteer-specific fields
    sex: Optional[Sex] = None
    description: Optional[str] = None
    skills: Optional[List[str]] = None
    age: Optional[int] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    location_area: Optional[str] = None
    has_drivers_license: Optional[bool] = None
    disabilities: Optional[str] = None

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

class EventSchema(BaseModel):
    id: int
    title: str
    description: str
    organization_id: int
    organization_name: str
    location: str
    date: str
    start_time: str
    end_time: str
    max_volunteers: int
    current_volunteers: int
    category: str
    requirements: List[str] = []
    status: str
    created_at: str
    updated_at: str
    latitude: float = 0.0
    longitude: float = 0.0

# ------------------ Profile Update ------------------

# Reuse same fields but make them all optional so users can update any subset.
class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None

    # Organizer / Institution specific
    organization_name: Optional[str] = None
    institution_name: Optional[InstitutionName] = None

    # Volunteer-specific optional fields
    sex: Optional[Sex] = None
    description: Optional[str] = None
    skills: Optional[List[str]] = None
    age: Optional[int] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    location_area: Optional[str] = None
    has_drivers_license: Optional[bool] = None
    disabilities: Optional[str] = None

class EventBase(BaseModel):
    title: str
    description: str | None = None
    date: str | None = None
    location: str | None = None
    image_url: str | None = None


class EventCreate(EventBase):
    pass


class EventOut(EventBase):
    id: int
    organizer_id: int

    class Config:
        orm_mode = True


class MessageBase(BaseModel):
    receiver_id: int
    content: str


class MessageCreate(MessageBase):
    pass


class MessageOut(MessageBase):
    id: int
    sender_id: int
    sent_at: datetime

    class Config:
        orm_mode = True 
