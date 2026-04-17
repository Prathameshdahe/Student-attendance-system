"""
force_seed_students.py — Run this once to push all 4 students into Neon.
Usage: cd backend && python force_seed_students.py
"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))

from app.core.database import SessionLocal
from app.models.user import User
from app.core.security import hash_password
import uuid

db = SessionLocal()

STUDENTS = [
    {"first": "Prathamesh", "last": "Dahe", "roll": "110", "email": "padahe23-comp@bvucoep.edu.in", "division": "A", "prn": "2314110215", "mobile": "6239951476", "class_name": "comp-2"},
    {"first": "Manamrit",   "last": "Singh", "roll": "104", "email": "msingh23-comp@bvucoep.edu.in", "division": "B", "prn": "2314110207", "mobile": "8826611487", "class_name": "comp-2"},
    {"first": "Harsh Kumar","last": "Pal",   "roll": "99",  "email": "hkpal2-comp@bvucoep.edu.in",  "division": "A", "prn": "2314110203", "mobile": None,           "class_name": "comp-2"},
    {"first": "Anushka",    "last": "Srivastava", "roll": "91", "email": "srianushka1976@gmail.com", "division": "B", "prn": "2314110195", "mobile": None, "class_name": "comp-2"},
]

pw = hash_password("Student@1234")
for s in STUDENTS:
    existing = db.query(User).filter_by(email=s["email"]).first()
    if existing:
        # Force-reset their password
        existing.hashed_password = pw
        existing.is_active = True
        print(f"  Reset password for: {s['email']}")
    else:
        db.add(User(
            id=str(uuid.uuid4()),
            email=s["email"],
            hashed_password=pw,
            name=f"{s['first']} {s['last']}",
            role="STUDENT",
            is_active=True,
            roll_number=s["roll"],
            division=s["division"],
            prn=s["prn"],
            mobile=s["mobile"],
            class_name=s["class_name"]
        ))
        print(f"  Created: {s['email']}")

db.commit()
db.close()
print("\n[DONE] All 4 students are seeded with password: Student@1234")
