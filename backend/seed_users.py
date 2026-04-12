"""
seed_users.py — Populates the KIWI database with:
  - 1 Admin (admin@bvucoep.edu.in)
  - Exactly 4 Students as specified (Check-in ready, no fake history)

Run:
    python seed_users.py
"""

import sys, os
sys.path.insert(0, os.path.dirname(__file__))

from app.core.database import SessionLocal, engine, Base
from app.models.user import User
from app.models.attendance import AttendanceRecord
from app.core.security import hash_password
import uuid

Base.metadata.create_all(bind=engine)
db = SessionLocal()

STUDENTS = [
    {
        "first": "Prathamesh", "last": "Dahe", "roll": "110", 
        "email": "padahe23-comp@bvucoep.edu.in", "division": "A", 
        "prn": "2314110215", "mobile": "6239951476", "class_name": "comp-2"
    },
    {
        "first": "Manamrit", "last": "Singh", "roll": "104", 
        "email": "msingh23-comp@bvucoep.edu.in", "division": "B", 
        "prn": "2314110207", "mobile": "8826611487", "class_name": "comp-2"
    },
    {
        "first": "Harsh Kumar", "last": "Pal", "roll": "99", 
        "email": "hkpal2-comp@bvucoep.edu.in", "division": "A", 
        "prn": "2314110203", "mobile": None, "class_name": "comp-2"
    },
    {
        "first": "Anushka", "last": "Srivastava", "roll": "91", 
        "email": "srianushka1976@gmail.com", "division": "B", 
        "prn": "2314110195", "mobile": None, "class_name": "comp-2"
    },
]


def seed():
    # ── Admin Only (removing generic faculty list, just you) ──
    existing_admin = db.query(User).filter(User.email == "admin@bvucoep.edu.in").first()
    if not existing_admin:
        admin = User(
            id=str(uuid.uuid4()),
            email="admin@bvucoep.edu.in",
            hashed_password=hash_password("Admin@1234"),
            name="KIWI Administrator",
            role="ADMIN",
            is_active=True
        )
        db.add(admin)
        print("[OK] Admin created: admin@bvucoep.edu.in")
    else:
        print("  Admin already exists, skipping.")

    db.commit()

    # ── 4 Students ──
    for s_data in STUDENTS:
        email = s_data["email"]

        existing = db.query(User).filter(User.email == email).first()
        if existing:
            print(f"  Student already exists: {existing.name}")
            continue

        student = User(
            id=str(uuid.uuid4()),
            email=email,
            hashed_password=hash_password("Student@1234"),
            name=f"{s_data['first']} {s_data['last']}",
            role="STUDENT",
            roll_number=s_data["roll"],
            division=s_data["division"],
            class_name=s_data["class_name"],
            year=2,
            prn=s_data["prn"],
            mobile=s_data["mobile"],
            is_active=True
        )
        db.add(student)
        db.flush()
        print(f"[OK] Student created: {student.name} ({student.email})")

    db.commit()

    print("\n[DONE] Database generated. System is live and waiting for FIRST SCAN.")
    print("   Admin login:   admin@bvucoep.edu.in / Admin@1234")
    print("   Students:                      / Student@1234")


if __name__ == "__main__":
    seed()
    db.close()
