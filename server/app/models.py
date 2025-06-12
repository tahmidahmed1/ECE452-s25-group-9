from sqlalchemy import Boolean, Column, Integer, String, DateTime, Enum
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
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now()) 