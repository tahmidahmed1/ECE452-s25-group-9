from datetime import datetime, timedelta
from typing import Optional, Dict
import uuid
import logging
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from .database import get_db
from .models import User

logger = logging.getLogger(__name__)

# In-memory session store (in production, use Redis or database)
active_sessions: Dict[str, dict] = {}

# Session configuration
SESSION_EXPIRE_HOURS = 24 * 7  # 7 days
security = HTTPBearer()

def create_session(user: User) -> str:
    """Create a new session for a user"""
    session_id = str(uuid.uuid4())
    expires_at = datetime.utcnow() + timedelta(hours=SESSION_EXPIRE_HOURS)
    
    session_data = {
        "user_id": user.id,
        "username": user.username,
        "created_at": datetime.utcnow(),
        "expires_at": expires_at,
        "last_accessed": datetime.utcnow()
    }
    
    active_sessions[session_id] = session_data
    
    logger.info(f"🔑 Session created for user {user.username} (ID: {user.id})")
    logger.info(f"🔑 Session ID: {session_id}")
    logger.info(f"🔑 Session expires at: {expires_at}")
    
    return session_id

def get_session(session_id: str) -> Optional[dict]:
    """Get session data by session ID"""
    if session_id not in active_sessions:
        logger.warning(f"🔑 Session not found: {session_id}")
        return None
    
    session = active_sessions[session_id]
    
    # Check if session has expired
    if datetime.utcnow() > session["expires_at"]:
        logger.warning(f"🔑 Session expired for user {session['username']}: {session_id}")
        del active_sessions[session_id]
        return None
    
    # Update last accessed time
    session["last_accessed"] = datetime.utcnow()
    
    logger.info(f"🔑 Session valid for user {session['username']} (ID: {session['user_id']})")
    return session

def invalidate_session(session_id: str) -> bool:
    """Invalidate a session"""
    if session_id in active_sessions:
        session = active_sessions[session_id]
        logger.info(f"🔑 Session invalidated for user {session['username']}: {session_id}")
        del active_sessions[session_id]
        return True
    return False

def invalidate_all_user_sessions(user_id: int):
    """Invalidate all sessions for a specific user"""
    sessions_to_remove = []
    for session_id, session_data in active_sessions.items():
        if session_data["user_id"] == user_id:
            sessions_to_remove.append(session_id)
    
    for session_id in sessions_to_remove:
        del active_sessions[session_id]
    
    logger.info(f"🔑 Invalidated {len(sessions_to_remove)} sessions for user ID: {user_id}")

def cleanup_expired_sessions():
    """Remove expired sessions from memory"""
    current_time = datetime.utcnow()
    expired_sessions = []
    
    for session_id, session_data in active_sessions.items():
        if current_time > session_data["expires_at"]:
            expired_sessions.append(session_id)
    
    for session_id in expired_sessions:
        session = active_sessions[session_id]
        logger.info(f"🔑 Cleaning up expired session for user {session['username']}: {session_id}")
        del active_sessions[session_id]
    
    if expired_sessions:
        logger.info(f"🔑 Cleaned up {len(expired_sessions)} expired sessions")

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    """Get current user from session"""
    session_id = credentials.credentials
    
    logger.info(f"🔍 Validating session: {session_id}")
    
    # Clean up expired sessions periodically
    cleanup_expired_sessions()
    
    # Get session data
    session_data = get_session(session_id)
    if not session_data:
        logger.warning(f"🔍 Invalid or expired session: {session_id}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired session",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Get user from database
    user = db.query(User).filter(User.id == session_data["user_id"]).first()
    if not user:
        logger.warning(f"🔍 User not found for session: {session_data['username']} (ID: {session_data['user_id']})")
        # Invalidate session if user doesn't exist
        invalidate_session(session_id)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    logger.info(f"🔍 Session validation successful - User: {user.username} (ID: {user.id})")
    return user

async def get_current_active_user(current_user: User = Depends(get_current_user)) -> User:
    """Get current active user"""
    if not current_user.is_active:
        logger.warning(f"🔍 Inactive user attempted access: {current_user.username}")
        raise HTTPException(status_code=400, detail="Inactive user")
    return current_user

# Utility functions for backward compatibility
def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash"""
    from passlib.context import CryptContext
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    """Hash a password"""
    from passlib.context import CryptContext
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    return pwd_context.hash(password)

def authenticate_user(db: Session, username: str, password: str) -> Optional[User]:
    """Authenticate a user with username and password"""
    user = db.query(User).filter(User.username == username).first()
    if not user:
        logger.warning(f"🔍 Authentication failed - user not found: {username}")
        return None
    if not verify_password(password, user.hashed_password):
        logger.warning(f"🔍 Authentication failed - invalid password for user: {username}")
        return None
    
    logger.info(f"🔍 Authentication successful for user: {username}")
    return user
