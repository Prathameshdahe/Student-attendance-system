from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.database import engine, Base, run_startup_migrations, SessionLocal
from app.models.user import User                    # ensure table registered
from app.models.attendance import AttendanceRecord, ExitRequest, GeofenceEvent  # ensure table registered
from app.routers import auth, students, attendance
from app.core.security import hash_password
import uuid

# Create all tables on startup
Base.metadata.create_all(bind=engine)
run_startup_migrations()

db = SessionLocal()
try:
    if not db.query(User).filter_by(email="admin@bvucoep.edu.in").first():
        db.add(User(
            id=str(uuid.uuid4()), email="admin@bvucoep.edu.in",
            hashed_password=hash_password("Admin@1234"),
            name="KIWI Admin", role="ADMIN", is_active=True
        ))
        db.commit()
except Exception:
    pass
finally:
    db.close()

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
