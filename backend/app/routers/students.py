from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List

from app.core.database import get_db
from app.routers.auth import get_current_user
from app.models.user import User

router = APIRouter(prefix="/students", tags=["students"])


class StudentOut:
    pass


@router.get("")
def list_students(
    class_name: str = Query("comp-2", alias="class"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: List all students in a given class."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    students = (
        db.query(User)
        .filter(User.role == "STUDENT", User.class_name == class_name, User.is_active == True)
        .order_by(User.roll_number)
        .all()
    )

    return [
        {
            "id":       s.id,
            "name":     s.name,
            "email":    s.email,
            "roll":     s.roll_number or "",
            "division": s.division or "",
            "year":     s.year or 2,
        }
        for s in students
    ]
