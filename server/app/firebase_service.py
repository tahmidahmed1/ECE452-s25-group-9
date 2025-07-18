import firebase_admin
from firebase_admin import credentials, messaging
from typing import List, Dict, Optional
import os
import json
import logging

logger = logging.getLogger(__name__)

class FirebaseService:
    _instance = None
    _initialized = False
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(FirebaseService, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not self._initialized:
            self._initialize_firebase()
            self._initialized = True
    
    def _initialize_firebase(self):
        """Initialize Firebase Admin SDK"""
        try:
            # Check if Firebase is already initialized
            if firebase_admin._apps:
                logger.info("Firebase already initialized")
                return
            
            # Try to get service account from environment variable
            service_account_key = os.getenv('FIREBASE_SERVICE_ACCOUNT_KEY')
            
            if service_account_key:
                # Parse JSON from environment variable
                service_account_info = json.loads(service_account_key)
                cred = credentials.Certificate(service_account_info)
            else:
                # Try to use default credentials or service account file
                # In production, you should set GOOGLE_APPLICATION_CREDENTIALS
                try:
                    cred = credentials.ApplicationDefault()
                except Exception:
                    logger.warning("No Firebase credentials found. Push notifications will be disabled.")
                    return
            
            firebase_admin.initialize_app(cred)
            logger.info("Firebase Admin SDK initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize Firebase: {e}")
    
    def send_notification_to_token(
        self, 
        token: str, 
        title: str, 
        body: str, 
        data: Optional[Dict[str, str]] = None
    ) -> bool:
        """Send notification to a specific FCM token"""
        try:
            if not firebase_admin._apps:
                logger.warning("Firebase not initialized. Cannot send notification.")
                return False
            
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=data or {},
                token=token,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(
                        priority='high',
                        default_sound=True,
                        default_vibrate_timings=True,
                    )
                )
            )
            
            response = messaging.send(message)
            logger.info(f"Successfully sent message: {response}")
            return True
            
        except messaging.UnregisteredError:
            logger.warning(f"FCM token is invalid or unregistered: {token[:20]}...")
            return False
        except Exception as e:
            logger.error(f"Error sending notification: {e}")
            return False
    
    def send_notification_to_multiple_tokens(
        self, 
        tokens: List[str], 
        title: str, 
        body: str, 
        data: Optional[Dict[str, str]] = None
    ) -> Dict[str, bool]:
        """Send notification to multiple FCM tokens"""
        results = {}
        
        if not firebase_admin._apps:
            logger.warning("Firebase not initialized. Cannot send notifications.")
            return {token: False for token in tokens}
        
        try:
            # Firebase allows sending to up to 500 tokens at once
            batch_size = 500
            for i in range(0, len(tokens), batch_size):
                batch_tokens = tokens[i:i + batch_size]
                
                message = messaging.MulticastMessage(
                    notification=messaging.Notification(
                        title=title,
                        body=body,
                    ),
                    data=data or {},
                    tokens=batch_tokens,
                    android=messaging.AndroidConfig(
                        priority='high',
                        notification=messaging.AndroidNotification(
                            priority='high',
                            default_sound=True,
                            default_vibrate_timings=True,
                        )
                    )
                )
                
                response = messaging.send_multicast(message)
                
                # Process results
                for idx, result in enumerate(response.responses):
                    token = batch_tokens[idx]
                    if result.success:
                        results[token] = True
                        logger.debug(f"Successfully sent to token: {token[:20]}...")
                    else:
                        results[token] = False
                        if result.exception:
                            if isinstance(result.exception, messaging.UnregisteredError):
                                logger.warning(f"Token unregistered: {token[:20]}...")
                            else:
                                logger.error(f"Failed to send to token {token[:20]}...: {result.exception}")
                
                logger.info(f"Batch sent: {response.success_count}/{len(batch_tokens)} successful")
        
        except Exception as e:
            logger.error(f"Error sending multicast notification: {e}")
            # Mark all as failed
            for token in tokens:
                results[token] = False
        
        return results
    
    def send_notification_to_topic(
        self, 
        topic: str, 
        title: str, 
        body: str, 
        data: Optional[Dict[str, str]] = None
    ) -> bool:
        """Send notification to a topic"""
        try:
            if not firebase_admin._apps:
                logger.warning("Firebase not initialized. Cannot send notification.")
                return False
            
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=data or {},
                topic=topic,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(
                        priority='high',
                        default_sound=True,
                        default_vibrate_timings=True,
                    )
                )
            )
            
            response = messaging.send(message)
            logger.info(f"Successfully sent message to topic {topic}: {response}")
            return True
            
        except Exception as e:
            logger.error(f"Error sending notification to topic {topic}: {e}")
            return False

# Singleton instance
firebase_service = FirebaseService()
