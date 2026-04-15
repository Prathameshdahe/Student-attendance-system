from datetime import date as dt_date, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, aliased

from app.core.database import get_db
from app.models.attendance import AttendanceRecord, ExitRequest, GeofenceEvent
from app.models.user import User
from app.routers.auth import get_current_user

router = APIRouter(prefix="/attendance", tags=["attendance"])


def format_dt(value: datetime | None) -> str | None:
    return value.strftime("%I:%M %p") if value else None


def normalize_exit_status(status: str | None) -> str:
    if status in {"DENYD", "DENY"}:
        return "DENIED"
    return status or "PENDING"


def status_label(status: str) -> str:
    return {
        "PENDING": "Awaiting faculty approval",
        "APPROVED": "Approved by faculty",
        "DENIED": "Denied by faculty",
    }.get(status, status.replace("_", " ").title())


def geofence_note(event_type: str, permission_status: str, reason: str | None = None) -> str:
    if event_type == "EXIT":
        if permission_status == "APPROVED":
            return f"Left campus after approval{f' for {reason}' if reason else ''}"
        if permission_status == "PENDING":
            return f"Left campus while request was pending{f' for {reason}' if reason else ''}"
        if permission_status == "DENIED":
            return f"Left campus after request was denied{f' for {reason}' if reason else ''}"
        return "Left campus without an approved exit request"
    if permission_status == "APPROVED":
        return "Returned to campus after an approved exit"
    return "Returned to campus"


def serialize_attendance(record: AttendanceRecord, student: User | None = None) -> dict:
    payload = {
        "id": record.id,
        "student_id": record.student_id,
        "date": record.date,
        "status": record.status,
        "time_in": record.time_in,
        "time_out": record.time_out,
        "created_at": record.created_at.isoformat() if record.created_at else None,
    }
    if student is not None:
        payload.update(
            {
                "name": student.name,
                "roll": student.roll_number or "",
                "division": student.division or "",
                "email": student.email,
            }
        )
    return payload


def serialize_exit_request(
    request: ExitRequest,
    student: User | None = None,
    resolver: User | None = None,
) -> dict:
    normalized_status = normalize_exit_status(request.status)
    return {
        "id": request.id,
        "request_id": request.id,
        "student_id": request.student_id,
        "name": student.name if student else None,
        "roll": student.roll_number if student else None,
        "reason": request.reason,
        "status": normalized_status,
        "status_label": status_label(normalized_status),
        "date": request.date,
        "time": format_dt(request.created_at),
        "created_at": request.created_at.isoformat() if request.created_at else None,
        "resolved_at": request.resolved_at.isoformat() if request.resolved_at else None,
        "resolution_time": format_dt(request.resolved_at),
        "resolved_by": request.resolved_by,
        "resolved_by_name": resolver.name if resolver else None,
        "left_campus_at": request.left_campus_at.isoformat() if request.left_campus_at else None,
        "left_campus_time": format_dt(request.left_campus_at),
        "returned_campus_at": request.returned_campus_at.isoformat() if request.returned_campus_at else None,
        "returned_campus_time": format_dt(request.returned_campus_at),
    }


def serialize_geofence_event(
    event: GeofenceEvent,
    student: User | None = None,
    related_request: ExitRequest | None = None,
) -> dict:
    permission_status = normalize_exit_status(event.permission_status)
    note = event.note or geofence_note(
        event.event_type,
        permission_status,
        related_request.reason if related_request else None,
    )
    return {
        "id": event.id,
        "student_id": event.student_id,
        "name": student.name if student else None,
        "roll": student.roll_number if student else None,
        "request_id": event.request_id,
        "event_type": event.event_type,
        "permission_status": permission_status,
        "source_type": event.source_type,
        "reason": related_request.reason if related_request else None,
        "note": note,
        "time": format_dt(event.created_at),
        "date": event.created_at.strftime("%Y-%m-%d") if event.created_at else None,
        "created_at": event.created_at.isoformat() if event.created_at else None,
    }


@router.get("/{student_id}")
def get_student_attendance(
    student_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
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

    return [serialize_attendance(record) for record in records]


@router.get("/live/today")
def get_live_attendance_today(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return all attendance records for today to drive the live dashboard."""
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
    return [serialize_attendance(record, student) for record, student in records]


class ScanRequest(BaseModel):
    qr_payload: str
    scanned_by: str


@router.post("/scan")
def scan_qr(
    req: ScanRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Scan a student QR for check-in or check-out."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    try:
        student_id, qr_date = req.qr_payload.strip().split("::")
    except ValueError:
        return {"status": "INVALID", "message": "Malformed QR code"}

    today = str(dt_date.today())
    if qr_date != today:
        return {"status": "INVALID", "message": f"QR expired - generated for {qr_date}"}

    student = (
        db.query(User)
        .filter(User.id == student_id, User.role == "STUDENT", User.is_active == True)
        .first()
    )
    if not student:
        return {"status": "INVALID", "message": "Student not found"}

    existing = (
        db.query(AttendanceRecord)
        .filter(AttendanceRecord.student_id == student_id, AttendanceRecord.date == today)
        .order_by(AttendanceRecord.created_at.desc())
        .first()
    )

    now_str = datetime.now().strftime("%I:%M %p")

    if not existing:
        record = AttendanceRecord(
            student_id=student_id,
            date=today,
            status="IN",
            time_in=now_str,
            scanned_by=req.scanned_by,
        )
        db.add(record)
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            return {
                "status": "DUPLICATE",
                "message": f"Attendance was already captured for {student.name}",
                "student_name": student.name,
                "roll": student.roll_number,
            }

        return {
            "status": "CHECK_IN",
            "message": f"Check-In successful for {student.name}",
            "student_name": student.name,
            "roll": student.roll_number,
            "time": now_str,
        }

    if existing.status == "IN":
        existing.time_out = now_str
        existing.status = "COMPLETED"
        db.commit()
        return {
            "status": "CHECK_OUT",
            "message": f"Check-Out successful for {student.name}",
            "student_name": student.name,
            "roll": student.roll_number,
            "time": now_str,
        }

    return {
        "status": "DUPLICATE",
        "message": f"{student.name} has already checked out today",
        "student_name": student.name,
    }


class GeofenceRequest(BaseModel):
    type: str


@router.post("/geofence-alert")
def geofence_alert(
    req: GeofenceRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Persist a geofence exit/return event."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    event_map = {
        "UNAUTHORIZED_EXIT": "EXIT",
        "RETURN_TO_CAMPUS": "RETURN",
    }
    event_type = event_map.get(req.type)
    if not event_type:
        raise HTTPException(status_code=400, detail="Invalid geofence event type")

    now_utc = datetime.utcnow()
    recent_duplicate = (
        db.query(GeofenceEvent)
        .filter(
            GeofenceEvent.student_id == current_user.id,
            GeofenceEvent.event_type == event_type,
            GeofenceEvent.created_at >= now_utc - timedelta(seconds=90),
        )
        .order_by(GeofenceEvent.created_at.desc())
        .first()
    )
    if recent_duplicate:
        return {"status": "IGNORED", "message": "Duplicate geofence event ignored"}

    latest_request = (
        db.query(ExitRequest)
        .filter(ExitRequest.student_id == current_user.id)
        .order_by(ExitRequest.created_at.desc())
        .first()
    )
    permission_status = normalize_exit_status(latest_request.status) if latest_request else "NONE"

    if latest_request and event_type == "EXIT" and permission_status == "APPROVED" and not latest_request.left_campus_at:
        latest_request.left_campus_at = now_utc
    if latest_request and event_type == "RETURN" and latest_request.left_campus_at and not latest_request.returned_campus_at:
        latest_request.returned_campus_at = now_utc

    event = GeofenceEvent(
        student_id=current_user.id,
        request_id=latest_request.id if latest_request else None,
        event_type=event_type,
        permission_status=permission_status,
        source_type=req.type,
        note=geofence_note(event_type, permission_status, latest_request.reason if latest_request else None),
    )
    db.add(event)
    db.commit()

    return {
        "status": "SUCCESS",
        "message": event.note,
        "event": serialize_geofence_event(event, current_user, latest_request),
    }


class SubmitExitRequest(BaseModel):
    reason: str


@router.post("/exit-requests")
def submit_exit_request(
    req: SubmitExitRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Request permission to leave campus."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    reason = req.reason.strip()
    if not reason:
        raise HTTPException(status_code=400, detail="Reason is required")

    existing = (
        db.query(ExitRequest)
        .filter(ExitRequest.student_id == current_user.id, ExitRequest.status == "PENDING")
        .order_by(ExitRequest.created_at.desc())
        .first()
    )
    if existing:
        return {"status": "ERROR", "message": "You already have a pending exit request"}

    new_request = ExitRequest(student_id=current_user.id, reason=reason)
    db.add(new_request)
    db.commit()
    db.refresh(new_request)

    return {
        "status": "SUCCESS",
        "message": f"Exit request submitted for '{reason}'",
        "request": serialize_exit_request(new_request, current_user),
    }


@router.get("/exit-requests/me")
def get_my_exit_requests(
    limit: int = Query(default=20, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Get the caller's exit request history."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    resolver_alias = aliased(User)
    records = (
        db.query(ExitRequest, resolver_alias)
        .outerjoin(resolver_alias, ExitRequest.resolved_by == resolver_alias.id)
        .filter(ExitRequest.student_id == current_user.id)
        .order_by(ExitRequest.created_at.desc())
        .limit(limit)
        .all()
    )
    return [
        serialize_exit_request(request, current_user, resolver)
        for request, resolver in records
    ]


@router.get("/exit-requests/pending")
def get_pending_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Get all pending exit requests."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    records = (
        db.query(ExitRequest, User)
        .join(User, ExitRequest.student_id == User.id)
        .filter(ExitRequest.status == "PENDING")
        .order_by(ExitRequest.created_at.desc())
        .all()
    )
    return [serialize_exit_request(request, student) for request, student in records]


@router.get("/exit-requests/history")
def get_exit_request_history(
    student_id: str | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Get recent exit request history, optionally for one student."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    resolver_alias = aliased(User)
    query = (
        db.query(ExitRequest, User, resolver_alias)
        .join(User, ExitRequest.student_id == User.id)
        .outerjoin(resolver_alias, ExitRequest.resolved_by == resolver_alias.id)
    )
    if student_id:
        query = query.filter(ExitRequest.student_id == student_id)

    records = query.order_by(ExitRequest.created_at.desc()).limit(limit).all()
    return [
        serialize_exit_request(request, student, resolver)
        for request, student, resolver in records
    ]


class ResolveExitRequest(BaseModel):
    action: str


@router.post("/exit-requests/{request_id}/resolve")
def resolve_request(
    request_id: str,
    req: ResolveExitRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Approve or deny a pending request."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    action = req.action.upper().strip()
    action_map = {"APPROVE": "APPROVED", "DENY": "DENIED"}
    if action not in action_map:
        raise HTTPException(status_code=400, detail="Invalid action")

    exit_request = db.query(ExitRequest).filter(ExitRequest.id == request_id).first()
    if not exit_request:
        raise HTTPException(status_code=404, detail="Request not found")
    if normalize_exit_status(exit_request.status) != "PENDING":
        raise HTTPException(status_code=409, detail="Request has already been resolved")

    exit_request.status = action_map[action]
    exit_request.resolved_by = current_user.id
    exit_request.resolved_at = datetime.utcnow()
    db.commit()
    db.refresh(exit_request)

    return {
        "status": "SUCCESS",
        "message": f"Request {action_map[action].lower()}",
        "request": serialize_exit_request(exit_request, resolver=current_user),
    }


@router.get("/history/me")
def get_my_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Get the caller's attendance history."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    records = (
        db.query(AttendanceRecord)
        .filter(AttendanceRecord.student_id == current_user.id)
        .order_by(AttendanceRecord.date.desc(), AttendanceRecord.created_at.desc())
        .all()
    )
    return [serialize_attendance(record) for record in records]


@router.get("/geofence-events/me")
def get_my_geofence_events(
    limit: int = Query(default=20, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Get the caller's recent geofence history."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    request_alias = aliased(ExitRequest)
    events = (
        db.query(GeofenceEvent, request_alias)
        .outerjoin(request_alias, GeofenceEvent.request_id == request_alias.id)
        .filter(GeofenceEvent.student_id == current_user.id)
        .order_by(GeofenceEvent.created_at.desc())
        .limit(limit)
        .all()
    )
    return [
        serialize_geofence_event(event, current_user, related_request)
        for event, related_request in events
    ]


@router.get("/geofence-events")
def get_geofence_events(
    student_id: str | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Get recent geofence events, optionally for a single student."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    request_alias = aliased(ExitRequest)
    query = (
        db.query(GeofenceEvent, User, request_alias)
        .join(User, GeofenceEvent.student_id == User.id)
        .outerjoin(request_alias, GeofenceEvent.request_id == request_alias.id)
    )
    if student_id:
        query = query.filter(GeofenceEvent.student_id == student_id)

    events = query.order_by(GeofenceEvent.created_at.desc()).limit(limit).all()
    return [
        serialize_geofence_event(event, student, related_request)
        for event, student, related_request in events
    ]


@router.get("/analytics/summary")
def get_analytics_summary(
    from_date: str = Query(default=None, description="YYYY-MM-DD"),
    to_date: str = Query(default=None, description="YYYY-MM-DD"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Admin-only: Summarize each student's attendance between the selected dates."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin access required")

    students = (
        db.query(User)
        .filter(User.role == "STUDENT", User.is_active == True)
        .order_by(User.class_name, User.division, User.roll_number, User.name)
        .all()
    )

    results = []
    for student in students:
        attendance_query = db.query(AttendanceRecord).filter(AttendanceRecord.student_id == student.id)
        request_query = db.query(ExitRequest).filter(ExitRequest.student_id == student.id)
        geofence_query = db.query(GeofenceEvent).filter(GeofenceEvent.student_id == student.id)

        if from_date:
            attendance_query = attendance_query.filter(AttendanceRecord.date >= from_date)
            request_query = request_query.filter(ExitRequest.date >= from_date)
            geofence_query = geofence_query.filter(GeofenceEvent.created_at >= datetime.fromisoformat(f"{from_date}T00:00:00"))
        if to_date:
            attendance_query = attendance_query.filter(AttendanceRecord.date <= to_date)
            request_query = request_query.filter(ExitRequest.date <= to_date)
            geofence_query = geofence_query.filter(GeofenceEvent.created_at < datetime.fromisoformat(f"{to_date}T23:59:59"))

        records = attendance_query.order_by(AttendanceRecord.date.desc()).all()
        requests = request_query.all()
        geofence_events = geofence_query.all()

        completed = [record for record in records if record.status == "COMPLETED"]
        total_days = len(records)
        present_days = len(completed)
        pct = round((present_days / total_days * 100) if total_days > 0 else 0, 1)
        last_seen = records[0].date if records else None

        results.append(
            {
                "id": student.id,
                "name": student.name,
                "roll": student.roll_number,
                "division": student.division,
                "email": student.email,
                "total_days": total_days,
                "present_days": present_days,
                "attendance_pct": pct,
                "last_seen": last_seen,
                "request_count": len(requests),
                "approved_requests": len([request for request in requests if normalize_exit_status(request.status) == "APPROVED"]),
                "denied_requests": len([request for request in requests if normalize_exit_status(request.status) == "DENIED"]),
                "geofence_exits": len([event for event in geofence_events if event.event_type == "EXIT"]),
                "records": [
                    {
                        "date": record.date,
                        "status": record.status,
                        "time_in": record.time_in or "-",
                        "time_out": record.time_out or "-",
                    }
                    for record in records
                ],
            }
        )

    return results
