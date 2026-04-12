from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from datetime import date as dt_date, datetime

from app.core.database import get_db
from app.routers.auth import get_current_user
from app.models.user import User
from app.models.attendance import AttendanceRecord

router = APIRouter(prefix="/attendance", tags=["attendance"])


# ─── GET /attendance/{student_id} ─────────────────────────────────────────────

@router.get("/{student_id}")
def get_student_attendance(
    student_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: Return full attendance log for a student."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    records = (
        db.query(AttendanceRecord)
        .filter(AttendanceRecord.student_id == student_id)
        .order_by(AttendanceRecord.date.desc(), AttendanceRecord.created_at.desc())
        .all()
    )

    return [
        {
            "date":     r.date,
            "status":   r.status,
            "time_in":  r.time_in,
            "time_out": r.time_out,
        }
        for r in records
    ]


# ─── GET /attendance/live/today ──────────────────────────────────────────────
@router.get("/live/today")
def get_live_attendance_today(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Return all attendance records for today to drive the Live Dashboard."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")
    
    today = str(dt_date.today())
    
    records = (
        db.query(AttendanceRecord, User)
        .join(User, AttendanceRecord.student_id == User.id)
        .filter(AttendanceRecord.date == today)
        .order_by(AttendanceRecord.created_at.desc())
        .all()
    )
    
    return [
        {
            "id":         rec.id,
            "student_id": rec.student_id,
            "name":       usr.name,
            "roll":       usr.roll_number,
            "status":     rec.status,
            "time_in":    rec.time_in,
            "time_out":   rec.time_out,
        }
        for rec, usr in records
    ]


# ─── POST /attendance/scan ─────────────────────────────────────────────────────

class ScanRequest(BaseModel):
    qr_payload: str    # format: "{user_id}::{YYYY-MM-DD}"
    scanned_by: str    # admin user_id


@router.post("/scan")
def scan_qr(
    req: ScanRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: Scan a student QR for Check-In or Check-Out."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    # Parse QR payload
    try:
        parts = req.qr_payload.strip().split("::")
        if len(parts) != 2:
            raise ValueError
        student_id, qr_date = parts[0], parts[1]
    except Exception:
        return {"status": "INVALID", "message": "Malformed QR code"}

    # Replay-attack prevention: QR must match today's date
    today = str(dt_date.today())
    if qr_date != today:
        return {"status": "INVALID", "message": f"QR expired — generated for {qr_date}"}

    # Check student exists
    student = db.query(User).filter(User.id == student_id, User.role == "STUDENT").first()
    if not student:
        return {"status": "INVALID", "message": "Student not found"}

    # Check if a record already exists for today
    existing = db.query(AttendanceRecord).filter(
        AttendanceRecord.student_id == student_id,
        AttendanceRecord.date == today
    ).first()

    now_str = datetime.now().strftime("%I:%M %p")

    if not existing:
        # Check-in (first scan)
        record = AttendanceRecord(
            student_id=student_id,
            date=today,
            status="IN",
            time_in=now_str,
            scanned_by=req.scanned_by
        )
        db.add(record)
        db.commit()
        return {
            "status": "CHECK_IN",
            "message": f"Check-In successful for {student.name}",
            "student_name": student.name,
            "roll": student.roll_number,
            "time": now_str
        }
    else:
        # Record exists. If status is "IN", this scan is a Check-Out.
        if existing.status == "IN":
            existing.time_out = now_str
            existing.status = "COMPLETED"
            db.commit()
            return {
                "status": "CHECK_OUT",
                "message": f"Check-Out successful for {student.name}",
                "student_name": student.name,
                "roll": student.roll_number,
                "time": now_str
            }
        else:
            # Already COMPLETED
            return {
                "status": "DUPLICATE",
                "message": f"{student.name} has already checked out today",
                "student_name": student.name
            }
