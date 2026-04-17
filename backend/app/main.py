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

    # Bootstrap dummy students
    STUDENTS = [
        {"first": "Prathamesh", "last": "Dahe", "roll": "110", "email": "padahe23-comp@bvucoep.edu.in", "division": "A", "prn": "2314110215", "mobile": "6239951476", "class_name": "comp-2"},
        {"first": "Manamrit", "last": "Singh", "roll": "104", "email": "msingh23-comp@bvucoep.edu.in", "division": "B", "prn": "2314110207", "mobile": "8826611487", "class_name": "comp-2"},
        {"first": "Harsh Kumar", "last": "Pal", "roll": "99", "email": "hkpal2-comp@bvucoep.edu.in", "division": "A", "prn": "2314110203", "mobile": None, "class_name": "comp-2"},
        {"first": "Anushka", "last": "Srivastava", "roll": "91", "email": "srianushka1976@gmail.com", "division": "B", "prn": "2314110195", "mobile": None, "class_name": "comp-2"},
    ]
    student_pw = hash_password("Student@1234")
    for s_data in STUDENTS:
        if not db.query(User).filter_by(email=s_data["email"]).first():
            student = User(
                id=str(uuid.uuid4()),
                email=s_data["email"],
                hashed_password=student_pw,
                name=f"{s_data['first']} {s_data['last']}",
                role="STUDENT",
                is_active=True,
                roll_number=s_data["roll"],
                division=s_data["division"],
                prn=s_data["prn"],
                mobile=s_data["mobile"],
                class_name=s_data["class_name"]
            )
            db.add(student)
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
