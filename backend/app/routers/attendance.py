from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from pydantic import BaseModel
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, aliased

from app.core.database import get_db
from app.models.attendance import AttendanceRecord, ExitRequest, GeofenceEvent
from app.models.user import User
from app.routers.auth import get_current_user

router = APIRouter(prefix="/attendance", tags=["attendance"])

UTC = timezone.utc
CAMPUS_TIMEZONE = ZoneInfo("Asia/Kolkata")
ALERT_PERMISSION_STATUSES = {"NONE", "DENIED"}


def utc_now() -> datetime:
    return datetime.now(UTC).replace(tzinfo=None)


def campus_now() -> datetime:
    return datetime.now(CAMPUS_TIMEZONE)


def campus_today() -> str:
    return campus_now().strftime("%Y-%m-%d")


def as_local_time(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is None:
        value = value.replace(tzinfo=UTC)
    return value.astimezone(CAMPUS_TIMEZONE)


def local_day_bounds(day: str) -> tuple[datetime, datetime]:
    start_local = datetime.strptime(day, "%Y-%m-%d").replace(tzinfo=CAMPUS_TIMEZONE)
    end_local = start_local + timedelta(days=1)
    return (
        start_local.astimezone(UTC).replace(tzinfo=None),
        end_local.astimezone(UTC).replace(tzinfo=None),
    )


def format_dt(value: datetime | None) -> str | None:
    local_value = as_local_time(value)
    return local_value.strftime("%I:%M %p") if local_value else None


def format_date(value: datetime | None) -> str | None:
    local_value = as_local_time(value)
    return local_value.strftime("%Y-%m-%d") if local_value else None


def format_timestamp(value: datetime | None) -> str | None:
    local_value = as_local_time(value)
    return local_value.isoformat() if local_value else None


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
            return f"Approved exit recorded{f' for {reason}' if reason else ''}"
        if permission_status == "PENDING":
            return f"Exit request pending - alert suppressed{f' for {reason}' if reason else ''}"
        if permission_status == "DENIED":
            return f"Alert: Student left campus after request was denied{f' for {reason}' if reason else ''}"
        return "Alert: Student left campus without an approved exit request"
    if permission_status == "APPROVED":
        return "Returned to campus after an approved exit"
    if permission_status == "PENDING":
        return "Returned to campus while request was still pending"
    if permission_status == "DENIED":
        return "Returned to campus after an unauthorized exit"
    return "Returned to campus"


def should_alert_geofence_event(event_type: str, permission_status: str) -> bool:
    return event_type == "EXIT" and permission_status in ALERT_PERMISSION_STATUSES


def geofence_alert_level(event_type: str, permission_status: str) -> str:
    if should_alert_geofence_event(event_type, permission_status):
        return "critical" if permission_status == "DENIED" else "warning"
    return "info"


def serialize_attendance(record: AttendanceRecord, student: User | None = None) -> dict:
    payload = {
        "id": record.id,
        "student_id": record.student_id,
        "date": record.date,
        "status": record.status,
        "time_in": record.time_in,
        "time_out": record.time_out,
        "created_at": format_timestamp(record.created_at),
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
        "date": format_date(request.created_at) or request.date,
        "time": format_dt(request.created_at),
        "created_at": format_timestamp(request.created_at),
        "resolved_at": format_timestamp(request.resolved_at),
        "resolution_time": format_dt(request.resolved_at),
        "resolved_by": request.resolved_by,
        "resolved_by_name": resolver.name if resolver else None,
        "left_campus_at": format_timestamp(request.left_campus_at),
        "left_campus_time": format_dt(request.left_campus_at),
        "returned_campus_at": format_timestamp(request.returned_campus_at),
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
    should_alert = should_alert_geofence_event(event.event_type, permission_status)
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
        "date": format_date(event.created_at),
        "device_id": event.device_id,
        "network_type": event.network_type,
        "latitude": event.latitude,
        "longitude": event.longitude,
        "accuracy_meters": event.accuracy_meters,
        "distance_from_center_meters": event.distance_from_center_meters,
        "created_at": format_timestamp(event.created_at),
        "should_alert": should_alert,
        "alert_level": geofence_alert_level(event.event_type, permission_status),
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

    today = campus_today()
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

    today = campus_today()
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

    now_str = campus_now().strftime("%I:%M %p")

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
    timestamp: int | None = None
    latitude: float | None = None
    longitude: float | None = None
    accuracy_meters: float | None = None
    distance_from_center_meters: float | None = None
    device_id: str | None = None
    network_type: str | None = None


class GeofenceEventUpload(BaseModel):
    type: str | None = None
    transition_type: str | None = None
    timestamp: int | None = None
    date: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    accuracy_meters: float | None = None
    distance_from_center_meters: float | None = None
    device_id: str | None = None
    network_type: str | None = None


def normalize_geofence_transition(raw_type: str | None) -> tuple[str, str]:
    value = (raw_type or "").strip().upper()
    mapping = {
        "UNAUTHORIZED_EXIT": ("EXIT", "UNAUTHORIZED_EXIT"),
        "EXIT": ("EXIT", "UNAUTHORIZED_EXIT"),
        "RETURN_TO_CAMPUS": ("RETURN", "RETURN_TO_CAMPUS"),
        "RETURN": ("RETURN", "RETURN_TO_CAMPUS"),
        "ENTER": ("RETURN", "RETURN_TO_CAMPUS"),
    }
    if value not in mapping:
        raise HTTPException(status_code=400, detail="Invalid geofence event type")
    return mapping[value]


def geofence_event_time(timestamp_ms: int | None) -> datetime:
    if not timestamp_ms:
        return utc_now()
    return datetime.fromtimestamp(timestamp_ms / 1000, tz=UTC).replace(tzinfo=None)


def persist_geofence_event(
    *,
    db: Session,
    current_user: User,
    transition_type: str,
    event_time: datetime,
    device_id: str | None,
    network_type: str | None,
    latitude: float | None,
    longitude: float | None,
    accuracy_meters: float | None,
    distance_from_center_meters: float | None,
) -> dict[str, Any]:
    event_type, source_type = normalize_geofence_transition(transition_type)
    event_local_day = format_date(event_time) or campus_today()
    recent_duplicate = (
        db.query(GeofenceEvent)
        .filter(
            GeofenceEvent.student_id == current_user.id,
            GeofenceEvent.event_type == event_type,
            GeofenceEvent.created_at >= event_time - timedelta(seconds=90),
            GeofenceEvent.created_at <= event_time + timedelta(seconds=90),
        )
        .order_by(GeofenceEvent.created_at.desc())
        .first()
    )
    if recent_duplicate:
        return {"status": "IGNORED", "message": "Duplicate geofence event ignored"}

    day_start_utc, day_end_utc = local_day_bounds(event_local_day)
    latest_request = (
        db.query(ExitRequest)
        .filter(
            ExitRequest.student_id == current_user.id,
            ExitRequest.created_at >= day_start_utc,
            ExitRequest.created_at < day_end_utc,
        )
        .order_by(ExitRequest.created_at.desc())
        .first()
    )
    permission_status = normalize_exit_status(latest_request.status) if latest_request else "NONE"

    if latest_request and event_type == "EXIT" and permission_status == "APPROVED" and not latest_request.left_campus_at:
        latest_request.left_campus_at = event_time
    if latest_request and event_type == "RETURN" and latest_request.left_campus_at and not latest_request.returned_campus_at:
        latest_request.returned_campus_at = event_time

    event = GeofenceEvent(
        student_id=current_user.id,
        request_id=latest_request.id if latest_request else None,
        event_type=event_type,
        permission_status=permission_status,
        source_type=source_type,
        note=geofence_note(event_type, permission_status, latest_request.reason if latest_request else None),
        device_id=device_id,
        network_type=network_type,
        latitude=latitude,
        longitude=longitude,
        accuracy_meters=accuracy_meters,
        distance_from_center_meters=distance_from_center_meters,
        created_at=event_time,
    )
    db.add(event)
    db.commit()
    db.refresh(event)

    return {
        "status": "SUCCESS",
        "message": event.note,
        "event": serialize_geofence_event(event, current_user, latest_request),
    }


@router.post("/geofence-alert")
def geofence_alert(
    req: GeofenceRequest,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Persist a geofence exit/return event."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")
    return persist_geofence_event(
        db=db,
        current_user=current_user,
        transition_type=req.type,
        event_time=geofence_event_time(req.timestamp),
        device_id=req.device_id or request.headers.get("X-KIWI-Device-ID"),
        network_type=req.network_type,
        latitude=req.latitude,
        longitude=req.longitude,
        accuracy_meters=req.accuracy_meters,
        distance_from_center_meters=req.distance_from_center_meters,
    )


@router.post("/geofence-events")
def geofence_event_upload(
    req: GeofenceEventUpload,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Student-only: Accept queued mobile geofence uploads."""
    if current_user.role != "STUDENT":
        raise HTTPException(status_code=403, detail="Student access required")

    transition_type = req.type or req.transition_type
    return persist_geofence_event(
        db=db,
        current_user=current_user,
        transition_type=transition_type,
        event_time=geofence_event_time(req.timestamp),
        device_id=req.device_id or request.headers.get("X-KIWI-Device-ID"),
        network_type=req.network_type,
        latitude=req.latitude,
        longitude=req.longitude,
        accuracy_meters=req.accuracy_meters,
        distance_from_center_meters=req.distance_from_center_meters,
    )


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

    new_request = ExitRequest(
        student_id=current_user.id,
        reason=reason,
        date=campus_today(),
        created_at=utc_now(),
    )
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
    exit_request.resolved_at = utc_now()
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
            from_start_utc, _ = local_day_bounds(from_date)
            request_query = request_query.filter(ExitRequest.created_at >= from_start_utc)
            geofence_query = geofence_query.filter(GeofenceEvent.created_at >= from_start_utc)
        if to_date:
            attendance_query = attendance_query.filter(AttendanceRecord.date <= to_date)
            _, to_end_utc = local_day_bounds(to_date)
            request_query = request_query.filter(ExitRequest.created_at < to_end_utc)
            geofence_query = geofence_query.filter(GeofenceEvent.created_at < to_end_utc)

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
