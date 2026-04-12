from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.database import engine, Base
from app.models.user import User                    # ensure table registered
from app.models.attendance import AttendanceRecord  # ensure table registered
from app.routers import auth, students, attendance

# Create all tables on startup
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="KIWI Smart Attendance API",
    description="Secure attendance management for Bharati Vidyapeeth College of Engineering, Pune.",
    version="3.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(students.router)
app.include_router(attendance.router)


@app.get("/")
def read_root():
    return {"status": "ok", "service": "KIWI Smart Attendance API v3.0"}
