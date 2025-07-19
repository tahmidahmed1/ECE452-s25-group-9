from pydantic import BaseModel, HttpUrl, EmailStr
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum

# Enums
class UserType(str, Enum):
    VOLUNTEER = "volunteer"
    ORGANIZER = "organizer"


class OpportunityCategory(str, Enum):
    COMMUNITY_SERVICE = "community_service"
    EDUCATION = "education"
    ENVIRONMENTAL = "environmental"
    HEALTHCARE = "healthcare"
    SOCIAL_SERVICES = "social_services"
    DISASTER_RELIEF = "disaster_relief"
    OTHER = "other"


class Sex(str, Enum):
    MALE = "male"
    FEMALE = "female"
    NON_BINARY = "non_binary"
    PREFER_NOT_TO_SAY = "prefer_not_to_say"

class SocialMediaPlatform(str, Enum):
    INSTAGRAM = "instagram"
    FACEBOOK = "facebook"
    TWITTER = "twitter"
    LINKEDIN = "linkedin"

class SocialMediaLink(BaseModel):
    platform: SocialMediaPlatform
    url: HttpUrl

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
    banner_url: Optional[str] = None
    
    # Organization fields (for organizers)
    organization_name: Optional[str] = None
    organization_description: Optional[str] = None
    organization_website: Optional[HttpUrl] = None
    organization_social_media: Optional[List[SocialMediaLink]] = None
    organization_images: Optional[List[str]] = None
    
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
    
    # Karma points for leaderboard
    karma_points: int = 0
    
    # Push notification fields
    fcm_token: Optional[str] = None
    notifications_enabled: bool = True

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
    organization_description: Optional[str] = None
    organization_website: Optional[HttpUrl] = None
    organization_social_media: Optional[List[SocialMediaLink]] = None
    organization_images: Optional[List[str]] = None

class OnboardingComplete(BaseModel):
    full_name: Optional[str] = None
    phone: str
    organization_name: Optional[str] = None
    organization_description: Optional[str] = None
    organization_website: Optional[HttpUrl] = None
    organization_social_media: Optional[List[SocialMediaLink]] = None
    organization_images: Optional[List[str]] = None
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

class ProfilePictureUploadResponse(BaseModel):
    profile_picture_url: str
    message: str

# Separate response for banner uploads
class ProfileBannerUploadResponse(BaseModel):
    banner_url: str
    message: str

# ------------------ Profile Update ------------------

# Reuse same fields but make them all optional so users can update any subset.
class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None

    # Organizer specific
    organization_name: Optional[str] = None
    organization_description: Optional[str] = None
    organization_website: Optional[HttpUrl] = None
    organization_social_media: Optional[List[SocialMediaLink]] = None
    organization_images: Optional[List[str]] = None

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

# Duplicate schema definitions removed to avoid conflicts

# ------------------ Event Schemas ------------------

class EventImageBase(BaseModel):
    image_url: str
    is_main: bool = False
    display_order: int = 0

class EventImageCreate(EventImageBase):
    pass

class EventImageOut(EventImageBase):
    id: int
    event_id: int
    created_at: datetime

    class Config:
        from_attributes = True

class EventBase(BaseModel):
    title: str
    description: Optional[str] = None
    date: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    location: Optional[str] = None
    image_url: Optional[str] = None

    # Geolocation coordinates
    latitude: Optional[float] = None
    longitude: Optional[float] = None

    # Volunteer counts
    max_volunteers: Optional[int] = None
    current_volunteers: Optional[int] = None

    # Event category
    category: OpportunityCategory = OpportunityCategory.OTHER

    # Karma points awarded to volunteers who complete this event
    karma_points: int = 10

class EventCreate(EventBase):
    pass


class EventOut(EventBase):
    id: int
    organizer_id: int
    images: List[EventImageOut] = []

    class Config:
        from_attributes = True

# Backwards compatibility alias used in routes
class EventSchema(EventOut):
    """Alias to maintain compatibility with earlier code referring to EventSchema."""
    pass 

# ------------------ Messaging Schemas ------------------


class MessageBase(BaseModel):
    receiver_id: int
    content: str


class MessageCreate(MessageBase):
    pass


class MessageOut(MessageBase):
    id: int
    sender_id: int
    created_at: datetime

    class Config:
        from_attributes = True

# Leaderboard schemas
class LeaderboardEntry(BaseModel):
    id: int
    username: str
    full_name: Optional[str] = None
    karma_points: int
    profile_picture_url: Optional[str] = None
    user_type: Optional[UserType] = None
    rank: int

    class Config:
        from_attributes = True

class LeaderboardResponse(BaseModel):
    entries: List[LeaderboardEntry]
    page: int
    page_size: int
    total_pages: int
    total_entries: int
    has_next: bool
    has_previous: bool 

# ------------------ Badge Schemas ------------------

class BadgeBase(BaseModel):
    name: str
    description: Optional[str] = None
    required_karma_points: int
    icon_name: str
    color: Optional[str] = None

class Badge(BadgeBase):
    id: int
    is_active: bool = True
    created_at: datetime
    
    class Config:
        from_attributes = True

class UserBadge(BaseModel):
    badge: Badge
    earned_at: datetime
    
    class Config:
        from_attributes = True

class BadgeAchievement(BaseModel):
    badge_id: int
    badge_name: str
    description: str
    icon_name: str
    color: Optional[str] = None
    earned_at: datetime

class BadgeCheckResponse(BaseModel):
    newly_earned_badges: List[BadgeAchievement]
    total_badges_earned: int
    next_badge: Optional[Badge] = None 
    
    class Config:
        from_attributes = True


# ------------------ Subscription Schemas ------------------

class SubscriptionCreate(BaseModel):
    organizer_id: int

class SubscriptionResponse(BaseModel):
    success: bool
    message: str
    is_subscribed: bool

class SubscriptionStatus(BaseModel):
    organizer_id: int
    is_subscribed: bool
    subscribed_at: Optional[datetime] = None

class UserSubscriptionsResponse(BaseModel):
    subscriptions: List[User]  # List of organizers the user is subscribed to
    
    class Config:
        from_attributes = True

class OrganizerWithSubscriptionStatus(User):
    is_subscribed: bool = False
    subscriber_count: int = 0
    
    class Config:
        from_attributes = True

# Notification schemas
class NotificationTokenUpdate(BaseModel):
    fcm_token: str

class NotificationPreferences(BaseModel):
    notifications_enabled: bool

class SubscriptionRequest(BaseModel):
    organizer_id: int

class NotificationRequest(BaseModel):
    title: str
    body: str
    data: Optional[Dict[str, str]] = None
    organizer_id: int

class NotificationResponse(BaseModel):
    success: bool
    message: Optional[str] = None

# In-app notification schemas
class InAppNotificationBase(BaseModel):
    title: str
    message: str
    data: Optional[Dict[str, str]] = None

class InAppNotificationCreate(InAppNotificationBase):
    user_id: int

class InAppNotificationOut(InAppNotificationBase):
    id: int
    user_id: int
    is_read: bool
    created_at: datetime
    read_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class InAppNotificationUpdate(BaseModel):
    is_read: Optional[bool] = None

class InAppNotificationsResponse(BaseModel):
    notifications: List[InAppNotificationOut]
    unread_count: int
    
    class Config:
        from_attributes = True
