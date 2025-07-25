from sqlalchemy import Boolean, Column, Integer, String, DateTime, Enum, Text, JSON, Float, ForeignKey, Table
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
import enum

from .database import Base

class UserType(enum.Enum):
    VOLUNTEER = "volunteer"
    ORGANIZER = "organizer"


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
    
    user_type = Column(Enum(UserType), nullable=True)
    onboarding_completed = Column(Boolean, default=False)
    
    full_name = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    
    profile_picture_url = Column(String, nullable=True)
    
    banner_url = Column(String, nullable=True)
    
    organization_name = Column(String, nullable=True)
    organization_description = Column(Text, nullable=True)
    organization_website = Column(String, nullable=True)
    organization_social_media = Column(JSON, nullable=True)  # Store
    organization_images = Column(JSON, nullable=True)  # Store array of image URLs
    
    sex = Column(Enum(Sex), nullable=True)
    description = Column(Text, nullable=True)
    skills = Column(JSON, nullable=True)  # Store as JSON array
    age = Column(Integer, nullable=True)
    emergency_contact_name = Column(String, nullable=True)
    emergency_contact_phone = Column(String, nullable=True)
    location_area = Column(String, nullable=True)
    has_drivers_license = Column(Boolean, nullable=True)
    disabilities = Column(Text, nullable=True)
    
    karma_points = Column(Integer, default=0, nullable=False)
    
    # Push notification fields
    fcm_token = Column(String, nullable=True)  # Firebase Cloud Messaging token
    notifications_enabled = Column(Boolean, default=True, nullable=False)  # User notification preference
    
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
    
    # Lost & Found items
    lost_found_items = relationship("LostFoundItem", back_populates="user")
    
    # Events this user has joined as a volunteer
    joined_events = relationship("Event", secondary="volunteer_events", back_populates="volunteers")


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
    users = relationship("User", secondary=user_badges, back_populates="badges")

class Message(Base):
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    receiver_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    content = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # --- New columns added in migration 009 ---
    is_read = Column(Boolean, default=False, nullable=False)
    is_important_sender = Column(Boolean, default=False, nullable=False)
    is_important_receiver = Column(Boolean, default=False, nullable=False)
    is_deleted_sender = Column(Boolean, default=False, nullable=False)
    is_deleted_receiver = Column(Boolean, default=False, nullable=False)

    
    # Relationships
    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])

    # One-to-many relationship to message reactions (defined below)
    reactions = relationship("MessageReaction", back_populates="message", cascade="all, delete-orphan")


# Separate model for message_reactions table (added via migration 009)
class MessageReaction(Base):
    __tablename__ = "message_reactions"

    id = Column(Integer, primary_key=True, index=True)
    message_id = Column(Integer, ForeignKey("messages.id", ondelete="CASCADE"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    emoji = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    message = relationship("Message", back_populates="reactions")
    user = relationship("User")


class OpportunityCategory(enum.Enum):
    COMMUNITY_SERVICE = "community_service"
    EDUCATION = "education"
    ENVIRONMENTAL = "environmental"
    HEALTHCARE = "healthcare"
    SOCIAL_SERVICES = "social_services"
    DISASTER_RELIEF = "disaster_relief"
    FOOD_SECURITY = "food_security"
    ANIMAL_WELFARE = "animal_welfare"
    ARTS_CULTURE = "arts_culture"
    YOUTH_MENTORING = "youth_mentoring"
    ELDERLY_CARE = "elderly_care"
    TECHNOLOGY = "technology"
    OTHER = "other"


class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    organizer_id = Column(Integer, ForeignKey('users.id', ondelete='CASCADE'), nullable=False)

    # Organizer relationship
    organizer = relationship("User", backref="organized_events")

    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    date = Column(String, nullable=True)
    start_time = Column(String, nullable=True)
    end_time = Column(String, nullable=True)
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

    # Relationship to event images
    images = relationship("EventImage", back_populates="event", cascade="all, delete-orphan")
    
    # Volunteers who have joined this event
    volunteers = relationship("User", secondary="volunteer_events", back_populates="joined_events")


class EventImage(Base):
    __tablename__ = "event_images"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id", ondelete="CASCADE"), nullable=False)
    image_url = Column(String, nullable=False)
    is_main = Column(Boolean, default=False, nullable=False)  # Mark if this is the main image
    display_order = Column(Integer, default=0, nullable=False)  # Order for carousel display
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationship back to event
    event = relationship("Event", back_populates="images")


# Association table for many-to-many relationship between users (volunteers) and organizers (subscriptions)
user_subscriptions = Table(
    'user_subscriptions',
    Base.metadata,
    Column('subscriber_id', Integer, ForeignKey('users.id'), primary_key=True),  # The volunteer who subscribes
    Column('organizer_id', Integer, ForeignKey('users.id'), primary_key=True),   # The organizer being subscribed to
    Column('subscribed_at', DateTime(timezone=True), server_default=func.now())
)

# Association table for many-to-many relationship between volunteers and events
volunteer_events = Table(
    'volunteer_events',
    Base.metadata,
    Column('volunteer_id', Integer, ForeignKey('users.id'), primary_key=True),  # The volunteer who joined
    Column('event_id', Integer, ForeignKey('events.id'), primary_key=True),     # The event being joined
    Column('joined_at', DateTime(timezone=True), server_default=func.now()),
    Column('status', String, default='joined', nullable=False),  # joined, completed, cancelled
    Column('karma_awarded', Boolean, default=False, nullable=False),  # Whether karma has been awarded
    # Attendance tracking columns (added in migration 011)
    Column('hours_worked', Float, nullable=True),
    Column('is_approved', Boolean, nullable=True),
    Column('rejection_reason', Text, nullable=True),
    Column('karma_points_earned', Integer, nullable=True, default=0)
)


class InAppNotification(Base):
    __tablename__ = "in_app_notifications"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    title = Column(String, nullable=False)
    message = Column(Text, nullable=False)
    data = Column(JSON, nullable=True)  # Additional data like event_id, organizer_id, etc.
    is_read = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    read_at = Column(DateTime(timezone=True), nullable=True)


class LostFoundItem(Base):
    __tablename__ = "lost_found_items"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    location = Column(String, nullable=False)
    item_type = Column(String, nullable=False)  # "lost" or "found"
    reward = Column(String, nullable=True)
    tags = Column(JSON, nullable=True)  # List of tags
    expiry_days = Column(Integer, default=30, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    expires_at = Column(DateTime(timezone=True), nullable=True)
    is_resolved = Column(Boolean, default=False, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)
    
    # Relationships
    user = relationship("User", back_populates="lost_found_items")
    images = relationship("LostFoundImage", back_populates="lost_found_item", cascade="all, delete-orphan")


class LostFoundImage(Base):
    __tablename__ = "lost_found_images"
    
    id = Column(Integer, primary_key=True, index=True)
    lost_found_item_id = Column(Integer, ForeignKey("lost_found_items.id", ondelete="CASCADE"), nullable=False)
    image_url = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    lost_found_item = relationship("LostFoundItem", back_populates="images")

