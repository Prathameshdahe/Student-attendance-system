from sqlalchemy import Column, DateTime, Float, String
from app.core.database import Base
from datetime import datetime
import uuid


class AttendanceRecord(Base):
    __tablename__ = "attendance_records"

    id         = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    student_id = Column(String, nullable=False, index=True)   # FK → users.id
    date       = Column(String, nullable=False, index=True)   # YYYY-MM-DD
    status     = Column(String, nullable=False)               # "IN" | "COMPLETED"
    time_in    = Column(String, nullable=True)                # e.g., "09:30 AM"
    time_out   = Column(String, nullable=True)                # e.g., "04:30 PM"
    scanned_by = Column(String, nullable=True)                # FK → users.id (admin)
    created_at = Column(DateTime, default=datetime.utcnow)

class ExitRequest(Base):
    __tablename__ = "exit_requests"

    id         = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    student_id = Column(String, nullable=False, index=True)   # FK -> users.id
    reason     = Column(String, nullable=False)               # "Lunch", "Medical", etc.
    status     = Column(String, default="PENDING")            # "PENDING", "APPROVED", "DENIED"
    resolved_by= Column(String, nullable=True)                # FK -> users.id (admin)
    date       = Column(String, default=lambda: datetime.utcnow().strftime("%Y-%m-%d"))
    created_at = Column(DateTime, default=datetime.utcnow)
    resolved_at = Column(DateTime, nullable=True)
    left_campus_at = Column(DateTime, nullable=True)
    returned_campus_at = Column(DateTime, nullable=True)


class GeofenceEvent(Base):
    __tablename__ = "geofence_events"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    student_id = Column(String, nullable=False, index=True)
    request_id = Column(String, nullable=True, index=True)
    event_type = Column(String, nullable=False)              # "EXIT" | "RETURN"
    permission_status = Column(String, nullable=False, default="NONE")
    source_type = Column(String, nullable=False)             # "UNAUTHORIZED_EXIT" | "RETURN_TO_CAMPUS"
    note = Column(String, nullable=True)
    device_id = Column(String, nullable=True)
    network_type = Column(String, nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    accuracy_meters = Column(Float, nullable=True)
    distance_from_center_meters = Column(Float, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
