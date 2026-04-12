from sqlalchemy import Column, String, Boolean, Integer
from app.core.database import Base
import uuid


class User(Base):
    __tablename__ = "users"

    id           = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    email        = Column(String, unique=True, nullable=False, index=True)
    hashed_password = Column(String, nullable=False)
    name         = Column(String, nullable=False)
    role         = Column(String, nullable=False, default="STUDENT")  # STUDENT or ADMIN
    roll_number  = Column(String, nullable=True)
    is_active    = Column(Boolean, default=True)
    # Phase 3 additions
    division     = Column(String, nullable=True)   # "A" or "B"
    class_name   = Column(String, nullable=True)   # e.g. "comp-2"
    year         = Column(Integer, nullable=True)  # 1–4
    prn          = Column(String, nullable=True)
    mobile       = Column(String, nullable=True)
