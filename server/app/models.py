from sqlalchemy import Boolean, Column, Integer, String, DateTime, Enum, Text, JSON, Float, ForeignKey, Table
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
import enum

from .database import Base

class UserType(enum.Enum):
    VOLUNTEER = "volunteer"
    ORGANIZER = "organizer"

class OrganizationType(enum.Enum):
    NON_PROFIT = "non_profit"
    SCHOOL_GROUP = "school_group"
    CLUB = "club"
    CHARITY = "charity"
    CUSTOM = "custom"

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
    
    # Privacy setting for profile picture sharing
    share_profile_picture = Column(Boolean, default=True, nullable=False)

    # Banner image URL (for organizers)
    banner_url = Column(String, nullable=True)
    
    # Organization fields (for organizers)
    organization_name = Column(String, nullable=True)
    organization_type = Column(Enum(OrganizationType), nullable=True)
    organization_description = Column(Text, nullable=True)
    organization_website = Column(String, nullable=True)
    organization_social_media = Column(JSON, nullable=True)  # Store social media links as JSON
    organization_images = Column(JSON, nullable=True)  # Store array of image URLs
    organization_custom_type = Column(String, nullable=True)  # For custom organization type
    
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
    
    # Karma points for leaderboard
    karma_points = Column(Integer, default=0, nullable=False)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # Relationship to badges
    badges = relationship("Badge", secondary="user_badges", back_populates="users")
    
    # Subscription relationships
    # Organizers this user (volunteer) is subscribed to
    subscribed_to = relationship(
        "User",
        secondary="user_subscriptions",
        primaryjoin="User.id == user_subscriptions.c.subscriber_id",
        secondaryjoin="User.id == user_subscriptions.c.organizer_id",
        back_populates="subscribers"
    )
    
    # Volunteers subscribed to this user (organizer)
    subscribers = relationship(
        "User",
        secondary="user_subscriptions",
        primaryjoin="User.id == user_subscriptions.c.organizer_id",
        secondaryjoin="User.id == user_subscriptions.c.subscriber_id",
        back_populates="subscribed_to"
    )


# Association table for many-to-many relationship between users and badges
user_badges = Table(
    'user_badges',
    Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id'), primary_key=True),
    Column('badge_id', Integer, ForeignKey('badges.id'), primary_key=True),
    Column('earned_at', DateTime(timezone=True), server_default=func.now())
)


class Badge(Base):
    __tablename__ = "badges"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, nullable=False)
    description = Column(Text, nullable=True)
    required_karma_points = Column(Integer, nullable=False)
    icon_name = Column(String, nullable=False)  # Icon identifier for frontend
    color = Column(String, nullable=True)  # Hex color code for badge styling
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
        # Relationship to users who have earned this badge
    users = relationship("User", secondary=user_badges, back_populates="badges")

class Message(Base):
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, nullable=False)
    receiver_id = Column(Integer, nullable=False)
    content = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class OpportunityCategory(enum.Enum):
    COMMUNITY_SERVICE = "community_service"
    EDUCATION = "education"
    ENVIRONMENTAL = "environmental"
    HEALTHCARE = "healthcare"
    SOCIAL_SERVICES = "social_services"
    DISASTER_RELIEF = "disaster_relief"
    OTHER = "other"


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

    # Category for the event
    category = Column(Enum(OpportunityCategory), default=OpportunityCategory.OTHER, nullable=False)

    # Karma points awarded to volunteers who complete this event
    karma_points = Column(Integer, default=10, nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now()) 


# Association table for many-to-many relationship between users (volunteers) and organizers (subscriptions)
user_subscriptions = Table(
    'user_subscriptions',
    Base.metadata,
    Column('subscriber_id', Integer, ForeignKey('users.id'), primary_key=True),  # The volunteer who subscribes
    Column('organizer_id', Integer, ForeignKey('users.id'), primary_key=True),   # The organizer being subscribed to
    Column('subscribed_at', DateTime(timezone=True), server_default=func.now())
)

