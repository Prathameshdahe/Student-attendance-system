from sqlalchemy import Column, String, DateTime
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
