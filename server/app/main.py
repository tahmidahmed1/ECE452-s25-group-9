from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import logging
import json

from .routes import router
from .database import engine, wait_for_db
from . import models

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Wait for database to be ready before proceeding
logger.info("Waiting for database to be ready...")
wait_for_db()

# Create database tables
models.Base.metadata.create_all(bind=engine)

# Initialize FastAPI app
app = FastAPI(
    title="GoodDeedFeed API",
    description="API for GoodDeedFeed application",
    version="0.1.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins in development
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

# Custom validation error handler
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error(f"Validation error for {request.method} {request.url}: {exc.errors()}")
    
    # Extract specific error messages
    error_messages = []
    for error in exc.errors():
        field = " -> ".join(str(loc) for loc in error["loc"])
        msg = error["msg"]
        error_messages.append(f"{field}: {msg}")
    
    error_detail = "; ".join(error_messages)
    
    return JSONResponse(
        status_code=422,
        content={
            "success": False,
            "message": f"Validation error: {error_detail}",
            "errors": exc.errors()
        }
    )

# Add request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    logger.info(f"Incoming request: {request.method} {request.url}")
    logger.info(f"Headers: {dict(request.headers)}")
    
    # Log request body for POST requests
    if request.method == "POST":
        try:
            body = await request.body()
            if body:
                logger.info(f"Request body: {body.decode('utf-8')}")
        except Exception as e:
            logger.error(f"Failed to read request body: {e}")
    
    response = await call_next(request)
    logger.info(f"Response status: {response.status_code}")
    return response

# Include routers
app.include_router(router, prefix="/api")

# Root endpoint
@app.get("/")
def read_root():
    return {"message": "Welcome to GoodDeedFeed API"}

# Health check endpoint
@app.get("/health")
def health_check():
    return {"status": "healthy"}

# Custom HTTPException handler to standardize error schema
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    logger.error(f"HTTPException {exc.status_code} at {request.url}: {exc.detail}")
    return JSONResponse(status_code=exc.status_code, content={
        "success": False,
        "message": exc.detail or "Unexpected server error"
    }) 
