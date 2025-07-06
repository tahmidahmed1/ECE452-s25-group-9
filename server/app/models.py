from sqlalchemy import Boolean, Column, Integer, String, DateTime, Enum, Text, JSON, Float
from sqlalchemy.sql import func
import enum

from .database import Base

class UserType(enum.Enum):
    VOLUNTEER = "volunteer"
    ORGANIZER = "organizer"
    INSTITUTION = "institution"

class InstitutionName(enum.Enum):
    INSTITUTION_1 = "Institution 1"
    INSTITUTION_2 = "Institution 2"
    INSTITUTION_3 = "Institution 3"

class Sex(enum.Enum):
    MALE = "male"
    FEMALE = "female"
    NON_BINARY = "non_binary"
    PREFER_NOT_TO_SAY = "prefer_not_to_say"

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    is_active = Column(Boolean, default=True)
    
    # Onboarding and user type fields
    user_type = Column(Enum(UserType), nullable=True)
    onboarding_completed = Column(Boolean, default=False)
    
    # Contact info (for all users)
    full_name = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    
    # Profile picture URL
    profile_picture_url = Column(String, nullable=True)
    
    # Organization name (for organizers)
    organization_name = Column(String, nullable=True)
    
    # Institution (for institutions)
    institution_name = Column(Enum(InstitutionName), nullable=True)
    
    # Enhanced volunteer profile fields
    sex = Column(Enum(Sex), nullable=True)
    description = Column(Text, nullable=True)
    skills = Column(JSON, nullable=True)  # Store as JSON array
    age = Column(Integer, nullable=True)
    emergency_contact_name = Column(String, nullable=True)
    emergency_contact_phone = Column(String, nullable=True)
    location_area = Column(String, nullable=True)
    has_drivers_license = Column(Boolean, nullable=True)
    disabilities = Column(Text, nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

class Message(Base):
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, nullable=False)
    receiver_id = Column(Integer, nullable=False)
    content = Column(Text, nullable=False)
    sent_at = Column(DateTime(timezone=True), server_default=func.now())


class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    organizer_id = Column(Integer, nullable=False)

    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    date = Column(String, nullable=True)
    location = Column(String, nullable=True)
    image_url = Column(String, nullable=True)

    max_volunteers = Column(Integer, nullable=True)
    current_volunteers = Column(Integer, nullable=False, default=0)

    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now()) 
