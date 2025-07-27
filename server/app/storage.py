import os
import uuid
from typing import Optional
from minio import Minio
from minio.error import S3Error
from fastapi import HTTPException, UploadFile
import logging
from io import BytesIO
from PIL import Image

logger = logging.getLogger(__name__)

class ObjectStorageService:
    def __init__(self):
        self.endpoint = os.getenv("MINIO_ENDPOINT", "localhost:9001")
        # Public endpoint visible to clients (e.g. phone/emulator)
        # Defaults to localhost:9001 so URL resolves on host machine;
        # override with MINIO_PUBLIC_ENDPOINT when deploying elsewhere.
        self.public_endpoint = os.getenv("MINIO_PUBLIC_ENDPOINT", "localhost:9001")
        self.access_key = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
        self.secret_key = os.getenv("MINIO_SECRET_KEY", "minioadmin123")
        self.bucket_name = os.getenv("MINIO_BUCKET_NAME", "profile-pictures")
        self.secure = False  # Use HTTP for local development
        
        self.client = Minio(
            self.endpoint,
            access_key=self.access_key,
            secret_key=self.secret_key,
            secure=self.secure
        )
        
        self._ensure_bucket_exists()
    
    def _ensure_bucket_exists(self):
        """Ensure the bucket exists, create it if it doesn't"""
        try:
            if not self.client.bucket_exists(self.bucket_name):
                self.client.make_bucket(self.bucket_name)
                logger.info(f"Created bucket: {self.bucket_name}")
                
                # Set bucket policy to allow public read access to images
                policy = {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": "*"},
                            "Action": ["s3:GetObject"],
                            "Resource": [f"arn:aws:s3:::{self.bucket_name}/*"]
                        }
                    ]
                }
                import json
                self.client.set_bucket_policy(self.bucket_name, json.dumps(policy))
                logger.info(f"Set public read policy for bucket: {self.bucket_name}")
            else:
                logger.info(f"Bucket {self.bucket_name} already exists")
        except S3Error as e:
            logger.error(f"Error ensuring bucket exists: {e}")
            raise HTTPException(status_code=500, detail="Object storage initialization failed")
    
    def _validate_image(self, file: UploadFile) -> bool:
        """Validate that the uploaded file is a valid image"""
        if not file.content_type or not file.content_type.startswith('image/'):
            return False
        
        # Additional validation by trying to open with PIL
        try:
            file.file.seek(0)
            image = Image.open(file.file)
            image.verify()
            file.file.seek(0)  # Reset file pointer
            return True
        except Exception:
            return False
    
    def _resize_image(self, file: UploadFile, max_size: tuple = (500, 500)) -> BytesIO:
        """Resize image to fit within max_size while maintaining aspect ratio"""
        try:
            file.file.seek(0)
            image = Image.open(file.file)
            
            # Convert to RGB if necessary (handles RGBA, P mode images)
            if image.mode in ('RGBA', 'P'):
                image = image.convert('RGB')
            
            # Calculate new size maintaining aspect ratio
            image.thumbnail(max_size, Image.Resampling.LANCZOS)
            
            # Save to BytesIO
            output = BytesIO()
            image.save(output, format='JPEG', quality=85, optimize=True)
            output.seek(0)
            
            return output
        except Exception as e:
            logger.error(f"Error resizing image: {e}")
            raise HTTPException(status_code=400, detail="Failed to process image")
    
    async def upload_profile_picture(self, file: UploadFile, user_id: int) -> str:
        """Upload a profile picture and return the URL"""
        # Validate file
        if not self._validate_image(file):
            raise HTTPException(status_code=400, detail="Invalid image file")
        
        # Check file size (max 5MB)
        if file.size and file.size > 5 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 5MB")
        
        try:
            # Generate unique filename
            file_extension = "jpg"  # We'll convert everything to JPEG
            filename = f"profile_{user_id}_{uuid.uuid4().hex}.{file_extension}"
            
            # Resize image
            resized_image = self._resize_image(file)
            
            # Upload to MinIO
            self.client.put_object(
                self.bucket_name,
                filename,
                resized_image,
                length=resized_image.getbuffer().nbytes,
                content_type="image/jpeg"
            )
            
            # Generate public URL
            url = f"http://{self.public_endpoint}/{self.bucket_name}/{filename}"
            logger.info(f"Uploaded profile picture: {url}")
            
            return url
            
        except S3Error as e:
            logger.error(f"Error uploading to MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
        except Exception as e:
            logger.error(f"Unexpected error uploading image: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
    
    def delete_profile_picture(self, url: str) -> bool:
        """Delete a profile picture by URL"""
        try:
            # Extract filename from URL
            filename = url.split('/')[-1]
            self.client.remove_object(self.bucket_name, filename)
            logger.info(f"Deleted profile picture: {filename}")
            return True
        except S3Error as e:
            logger.error(f"Error deleting from MinIO: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error deleting image: {e}")
            return False

    # ---------------- Banner Images ---------------- #

    async def upload_profile_banner(self, file: UploadFile, user_id: int) -> str:
        """Upload a profile banner image and return the URL"""
        # Validate file
        if not self._validate_image(file):
            raise HTTPException(status_code=400, detail="Invalid image file")

        # Max 5MB
        if file.size and file.size > 5 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 5MB")

        try:
            file_extension = "jpg"
            filename = f"banner_{user_id}_{uuid.uuid4().hex}.{file_extension}"

            resized = self._resize_image(file, max_size=(1000, 400))  # Wider aspect for banners

            self.client.put_object(
                self.bucket_name,
                filename,
                resized,
                length=resized.getbuffer().nbytes,
                content_type="image/jpeg",
            )

            url = f"http://{self.public_endpoint}/{self.bucket_name}/{filename}"
            logger.info(f"Uploaded profile banner: {url}")
            return url
        except S3Error as e:
            logger.error(f"Error uploading banner to MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
        except Exception as e:
            logger.error(f"Unexpected error uploading banner image: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")

    # ---------------- Event Images ---------------- #

    async def upload_event_image(self, file: UploadFile, event_id: int) -> str:
        """Upload an event image and return the public URL"""
        # Validate file is an image
        if not self._validate_image(file):
            raise HTTPException(status_code=400, detail="Invalid image file")

        # Limit size to 5MB
        if file.size and file.size > 5 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 5MB")

        try:
            file_extension = "jpg"  # convert to JPEG
            filename = f"event_{event_id}_{uuid.uuid4().hex}.{file_extension}"

            resized_image = self._resize_image(file)

            self.client.put_object(
                self.bucket_name,
                filename,
                resized_image,
                length=resized_image.getbuffer().nbytes,
                content_type="image/jpeg",
            )

            url = f"http://{self.public_endpoint}/{self.bucket_name}/{filename}"
            logger.info(f"Uploaded event image: {url}")
            return url
        except S3Error as e:
            logger.error(f"Error uploading to MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
        except Exception as e:
            logger.error(f"Unexpected error uploading image: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")

    # ---------------- Lost & Found Images ---------------- #

    async def upload_lost_found_image(self, file: UploadFile, item_id: int) -> str:
        """Upload a lost & found image and return the public URL"""
        # Re-use same validation/resize logic as event images
        if not self._validate_image(file):
            raise HTTPException(status_code=400, detail="Invalid image file")

        if file.size and file.size > 5 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 5MB")

        try:
            filename = f"lostfound_{item_id}_{uuid.uuid4().hex}.jpg"

            resized_image = self._resize_image(file)

            self.client.put_object(
                self.bucket_name,
                filename,
                resized_image,
                length=resized_image.getbuffer().nbytes,
                content_type="image/jpeg",
            )

            url = f"http://{self.public_endpoint}/{self.bucket_name}/{filename}"
            logger.info(f"Uploaded lost & found image: {url}")
            return url
        except S3Error as e:
            logger.error(f"Error uploading lost & found image to MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
        except Exception as e:
            logger.error(f"Unexpected error uploading lost & found image: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")

    # ---------------- Organization Images ---------------- #

    async def upload_organization_image(self, file: UploadFile, user_id: int) -> str:
        """Upload an organization image and return the URL"""
        # Validate file
        if not self._validate_image(file):
            raise HTTPException(status_code=400, detail="Invalid image file")
        
        # Check file size (max 5MB)
        if file.size and file.size > 5 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 5MB")

        try:
            file_extension = "jpg"  # convert to JPEG
            filename = f"organization_{user_id}_{uuid.uuid4().hex}.{file_extension}"

            resized_image = self._resize_image(file)

            self.client.put_object(
                self.bucket_name,
                filename,
                resized_image,
                length=resized_image.getbuffer().nbytes,
                content_type="image/jpeg",
            )

            url = f"http://{self.public_endpoint}/{self.bucket_name}/{filename}"
            logger.info(f"Uploaded organization image: {url}")
            return url
        except S3Error as e:
            logger.error(f"Error uploading organization image to MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")
        except Exception as e:
            logger.error(f"Unexpected error uploading organization image: {e}")
            raise HTTPException(status_code=500, detail="Failed to upload image")

    async def delete_file_from_url(self, file_url: str):
        """Delete a file from storage using its URL."""
        try:
            # Extract object name from URL
            # Expected format: http://localhost:9001/bucket-name/object-name
            parts = file_url.split('/')
            object_name = parts[-1]  # Get the filename part
            
            self.client.remove_object(self.bucket_name, object_name)
            logger.info(f"Successfully deleted object: {object_name}")
        except S3Error as e:
            logger.error(f"Error deleting from MinIO: {e}")
            raise HTTPException(status_code=500, detail="Failed to delete image")
        except Exception as e:
            logger.error(f"Unexpected error deleting image: {e}")
            raise HTTPException(status_code=500, detail="Failed to delete image")

# Global instance
storage_service = ObjectStorageService() 
