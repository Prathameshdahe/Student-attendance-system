from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from datetime import date as dt_date, datetime

from app.core.database import get_db
from app.routers.auth import get_current_user
from app.models.user import User
from app.models.attendance import AttendanceRecord, ExitRequest

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

# ─── POST /attendance/geofence-alert ─────────────────────────────────────────

class GeofenceRequest(BaseModel):
    type: str  # "UNAUTHORIZED_EXIT" or "RETURN_TO_CAMPUS"

@router.post("/geofence-alert")
def geofence_alert(
    req: GeofenceRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Student-only: Background GPS receiver hits this when leaving/entering campus bounding box."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")
    
    # Logs this into backend logic asynchronously
    print(f"[GEOFENCE] Alert: {req.type} triggered by Student ID: {current_user.id} ({current_user.name})")

    return {
        "status": "SUCCESS",
        "message": f"Logged {req.type} for {current_user.name}"
    }

# ─── PHASE 8: EXIT REQUESTS ──────────────────────────────────────────────────

class SubmitExitRequest(BaseModel):
    reason: str

@router.post("/exit-requests")
def submit_exit_request(
    req: SubmitExitRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Student-only: Request a campus exit pass."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")
    
    # Check if a pending request already exists
    existing = db.query(ExitRequest).filter(
        ExitRequest.student_id == current_user.id,
        ExitRequest.status == "PENDING"
    ).first()
    
    if existing:
        return {"status": "ERROR", "message": "You already have a pending exit request"}
        
    new_request = ExitRequest(
        student_id=current_user.id,
        reason=req.reason
    )
    db.add(new_request)
    db.commit()
    
    return {"status": "SUCCESS", "message": f"Exit request submitted for '{req.reason}'"}

@router.get("/exit-requests/me")
def get_my_exit_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Student-only: Get today's exit requests for the caller."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")
    
    today_start = datetime.combine(dt_date.today(), datetime.min.time())
    
    records = (
        db.query(ExitRequest)
        .filter(
            ExitRequest.student_id == current_user.id,
            ExitRequest.created_at >= today_start
        )
        .order_by(ExitRequest.created_at.desc())
        .all()
    )
    
    return [
        {
            "id": req.id,
            "reason": req.reason,
            "status": req.status,
            "time": req.created_at.strftime("%I:%M %p")
        }
        for req in records
    ]

@router.get("/exit-requests/pending")
def get_pending_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: Get all pending exit requests from today."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")
    
    today_start = datetime.combine(dt_date.today(), datetime.min.time())
    
    records = (
        db.query(ExitRequest, User)
        .join(User, ExitRequest.student_id == User.id)
        .filter(
            ExitRequest.status == "PENDING",
            ExitRequest.created_at >= today_start
        )
        .order_by(ExitRequest.created_at.desc())
        .all()
    )
    
    return [
        {
            "request_id": req.id,
            "student_id": usr.id,
            "name":       usr.name,
            "roll":       usr.roll_number,
            "reason":     req.reason,
            "time":       req.created_at.strftime("%I:%M %p")
        }
        for req, usr in records
    ]

class ResolveExitRequest(BaseModel):
    action: str # "APPROVE" or "DENY"

@router.post("/exit-requests/{request_id}/resolve")
def resolve_request(
    request_id: str,
    req: ResolveExitRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: Approve or Deny a pending request."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")
        
    exit_req = db.query(ExitRequest).filter(ExitRequest.id == request_id).first()
    if not exit_req:
        raise HTTPException(status_code=404, detail="Request not found")
        
    if req.action not in ["APPROVE", "DENY"]:
        raise HTTPException(status_code=400, detail="Invalid action")
        
    exit_req.status = req.action + "D" # "APPROVED" or "DENIED"
    exit_req.resolved_by = current_user.id
    db.commit()
    
    return {"status": "SUCCESS", "message": f"Request {req.action}D"}


# ─── PHASE 10: ATTENDANCE HISTORY ────────────────────────────────────────────

@router.get("/history/me")
def get_my_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Student-only: Get caller's own attendance history (all time)."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    records = (
        db.query(AttendanceRecord)
        .filter(AttendanceRecord.student_id == current_user.id)
        .order_by(AttendanceRecord.date.desc())
        .all()
    )

    return [
        {
            "date":     r.date,
            "status":   r.status,
            "time_in":  r.time_in  or "—",
            "time_out": r.time_out or "—",
        }
        for r in records
    ]


# ─── PHASE 10: ADMIN ANALYTICS SUMMARY ───────────────────────────────────────

from fastapi import Query

@router.get("/analytics/summary")
def get_analytics_summary(
    from_date: str = Query(default=None, description="YYYY-MM-DD"),
    to_date:   str = Query(default=None, description="YYYY-MM-DD"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Admin-only: Summarize each student's attendance between date range."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    students = db.query(User).filter(User.role == "STUDENT").all()

    results = []
    for s in students:
        q = db.query(AttendanceRecord).filter(AttendanceRecord.student_id == s.id)
        if from_date:
            q = q.filter(AttendanceRecord.date >= from_date)
        if to_date:
            q = q.filter(AttendanceRecord.date <= to_date)
        records = q.order_by(AttendanceRecord.date.desc()).all()

        completed = [r for r in records if r.status == "COMPLETED"]
        total_days = len(records)
        present_days = len(completed)
        pct = round((present_days / total_days * 100) if total_days > 0 else 0, 1)

        # Average check-in (rough: average hour from "HH:MM AM/PM")
        last_seen = records[0].date if records else None

        results.append({
            "id":           s.id,
            "name":         s.name,
            "roll":         s.roll_number,
            "division":     s.division,
            "email":        s.email,
            "total_days":   total_days,
            "present_days": present_days,
            "attendance_pct": pct,
            "last_seen":    last_seen,
            "records":      [{"date": r.date, "status": r.status, "time_in": r.time_in or "—", "time_out": r.time_out or "—"} for r in records]
        })

    return results
