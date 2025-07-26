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
        logger.info("🔥 FIREBASE INIT: Starting Firebase initialization")
        logger.info(f"🔥 FIREBASE INIT: Current working directory: {os.getcwd()}")
        logger.info(f"🔥 FIREBASE INIT: Environment variables: FIREBASE_SERVICE_ACCOUNT_KEY={'set' if os.getenv('FIREBASE_SERVICE_ACCOUNT_KEY') else 'not set'}")
        logger.info(f"🔥 FIREBASE INIT: Environment variables: GOOGLE_APPLICATION_CREDENTIALS={os.getenv('GOOGLE_APPLICATION_CREDENTIALS', 'not set')}")
        
        try:
            # Check if Firebase is already initialized
            if firebase_admin._apps:
                logger.info("🔥 FIREBASE INIT: Firebase already initialized, skipping")
                for app_name, app in firebase_admin._apps.items():
                    logger.info(f"🔥 FIREBASE INIT: Existing app: {app_name} - {app}")
                return
            
            # Try to get service account from environment variable
            service_account_key = os.getenv('FIREBASE_SERVICE_ACCOUNT_KEY')
            logger.info(f"🔥 FIREBASE INIT: Service account key environment variable present: {bool(service_account_key)}")
            
            if service_account_key:
                logger.info("🔥 FIREBASE INIT: Using service account key from environment variable")
                try:
                    # Parse JSON from environment variable
                    service_account_info = json.loads(service_account_key)
                    logger.info(f"🔥 FIREBASE INIT: Successfully parsed service account JSON. Project ID: {service_account_info.get('project_id', 'N/A')}")
                    cred = credentials.Certificate(service_account_info)
                    logger.info("🔥 FIREBASE INIT: Certificate credentials created successfully")
                except json.JSONDecodeError as e:
                    logger.error(f"🔥 FIREBASE INIT: Failed to parse service account JSON: {e}")
                    return
                except Exception as e:
                    logger.error(f"🔥 FIREBASE INIT: Failed to create certificate credentials: {e}")
                    return
            else:
                logger.info("🔥 FIREBASE INIT: No service account key in environment, trying default credentials")
                # Try to use default credentials or service account file
                # In production, you should set GOOGLE_APPLICATION_CREDENTIALS
                google_app_creds = os.getenv('GOOGLE_APPLICATION_CREDENTIALS')
                logger.info(f"🔥 FIREBASE INIT: GOOGLE_APPLICATION_CREDENTIALS: {google_app_creds}")
                try:
                    cred = credentials.ApplicationDefault()
                    logger.info("🔥 FIREBASE INIT: Application default credentials created successfully")
                except Exception as e:
                    logger.warning(f"🔥 FIREBASE INIT: No Firebase credentials found. Push notifications will be disabled. Error: {e}")
                    return
            
            logger.info("🔥 FIREBASE INIT: Initializing Firebase app with credentials")
            firebase_admin.initialize_app(cred)
            logger.info("🔥 FIREBASE INIT: ✅ Firebase Admin SDK initialized successfully")
            
        except Exception as e:
            logger.error(f"🔥 FIREBASE INIT: ❌ Failed to initialize Firebase: {e}")
            logger.error(f"🔥 FIREBASE INIT: Exception type: {type(e).__name__}")
            import traceback
            logger.error(f"🔥 FIREBASE INIT: Full traceback: {traceback.format_exc()}")
    
    def send_notification_to_token(
        self, 
        token: str, 
        title: str, 
        body: str, 
        data: Optional[Dict[str, str]] = None
    ) -> bool:
        """Send notification to a specific FCM token"""
        logger.info(f"📱 FCM SEND: Starting notification send to token")
        logger.info(f"📱 FCM SEND: Token: {token[:20]}...{token[-10:] if len(token) > 30 else token}")
        logger.info(f"📱 FCM SEND: Title: '{title}'")
        logger.info(f"📱 FCM SEND: Body: '{body}'")
        logger.info(f"📱 FCM SEND: Data: {data}")
        
        try:
            if not firebase_admin._apps:
                logger.error("📱 FCM SEND: ❌ Firebase not initialized. Cannot send notification.")
                return False
            
            logger.info("📱 FCM SEND: Firebase app is initialized, proceeding with message creation")
            
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
            
            logger.info("📱 FCM SEND: Message object created successfully, sending to Firebase")
            response = messaging.send(message)
            logger.info(f"📱 FCM SEND: ✅ Successfully sent message. Response: {response}")
            return True
            
        except messaging.UnregisteredError as e:
            logger.warning(f"📱 FCM SEND: ⚠️ FCM token is invalid or unregistered: {token[:20]}... Error: {e}")
            return False
        except messaging.InvalidArgumentError as e:
            logger.error(f"📱 FCM SEND: ❌ Invalid argument error: {e}")
            logger.error(f"📱 FCM SEND: Token validation failed for: {token[:20]}...")
            return False
        except messaging.QuotaExceededError as e:
            logger.error(f"📱 FCM SEND: ❌ Quota exceeded error: {e}")
            return False
        except messaging.SenderIdMismatchError as e:
            logger.error(f"📱 FCM SEND: ❌ Sender ID mismatch error: {e}")
            return False
        except messaging.ThirdPartyAuthError as e:
            logger.error(f"📱 FCM SEND: ❌ Third party auth error: {e}")
            return False
        except Exception as e:
            logger.error(f"📱 FCM SEND: ❌ Unexpected error sending notification: {e}")
            logger.error(f"📱 FCM SEND: Exception type: {type(e).__name__}")
            import traceback
            logger.error(f"📱 FCM SEND: Full traceback: {traceback.format_exc()}")
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
