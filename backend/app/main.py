from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import auth

app = FastAPI(
    title="Smart Attendance System API",
    description="API for the Smart Attendance System including face verification, QR tokens, and geofencing.",
    version="1.0.0"
)

# Allow CORS for all origins for testing purposes
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)

@app.get("/")
def read_root():
    return {"message": "Smart Attendance API is running"}
