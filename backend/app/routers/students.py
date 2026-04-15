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
    class_name: str | None = Query(default=None, alias="class"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: List all active students, optionally filtered by class."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    query = db.query(User).filter(User.role == "STUDENT", User.is_active == True)
    if class_name:
        query = query.filter(User.class_name == class_name)

    students = query.order_by(User.class_name, User.division, User.roll_number, User.name).all()

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
