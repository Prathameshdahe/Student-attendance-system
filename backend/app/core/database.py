from sqlalchemy import create_engine, text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{os.path.join(BASE_DIR, 'attendance.db')}")

if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def _table_columns(conn, table_name: str, dialect: str) -> set[str]:
    if dialect == "sqlite":
        rows = conn.execute(text(f"PRAGMA table_info({table_name})")).fetchall()
        return {row[1] for row in rows}
    else:
        rows = conn.execute(text(f"SELECT column_name FROM information_schema.columns WHERE table_name = '{table_name}'")).fetchall()
        return {row[0] for row in rows}


def _ensure_column(conn, table_name: str, column_name: str, column_type: str, dialect: str) -> None:
    columns = _table_columns(conn, table_name, dialect)
    if column_name not in columns:
        conn.execute(text(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {column_type}"))


def _deduplicate_attendance_rows(conn) -> None:
    duplicate_groups = conn.execute(
        text(
            """
            SELECT student_id, date
            FROM attendance_records
            GROUP BY student_id, date
            HAVING COUNT(*) > 1
            """
        )
    ).fetchall()

    for student_id, record_date in duplicate_groups:
        rows = conn.execute(
            text(
                """
                SELECT id, status, time_in, time_out, created_at
                FROM attendance_records
                WHERE student_id = :student_id AND date = :record_date
                ORDER BY created_at ASC, id ASC
                """
            ),
            {"student_id": student_id, "record_date": record_date},
        ).mappings().all()

        if len(rows) < 2:
            continue

        keep = rows[0]
        merged_time_in = next((row["time_in"] for row in rows if row["time_in"]), None)
        merged_time_out = next((row["time_out"] for row in reversed(rows) if row["time_out"]), None)
        merged_status = "COMPLETED" if any(row["status"] == "COMPLETED" or row["time_out"] for row in rows) else rows[-1]["status"]

        conn.execute(
            text(
                """
                UPDATE attendance_records
                SET status = :status, time_in = :time_in, time_out = :time_out
                WHERE id = :record_id
                """
            ),
            {
                "status": merged_status,
                "time_in": merged_time_in,
                "time_out": merged_time_out,
                "record_id": keep["id"],
            },
        )

        for row in rows[1:]:
            conn.execute(
                text("DELETE FROM attendance_records WHERE id = :record_id"),
                {"record_id": row["id"]},
            )


def run_startup_migrations() -> None:
    with engine.begin() as conn:
        dialect = engine.dialect.name
        if dialect == "sqlite":
            tables = {
                row[0]
                for row in conn.execute(
                    text("SELECT name FROM sqlite_master WHERE type='table'")
                ).fetchall()
            }
        else:
            tables = {
                row[0]
                for row in conn.execute(
                    text("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
                ).fetchall()
            }

        if "exit_requests" in tables:
            _ensure_column(conn, "exit_requests", "resolved_at", "DATETIME", dialect)
            _ensure_column(conn, "exit_requests", "left_campus_at", "DATETIME", dialect)
            _ensure_column(conn, "exit_requests", "returned_campus_at", "DATETIME", dialect)
            conn.execute(
                text("UPDATE exit_requests SET status = 'DENIED' WHERE status IN ('DENYD', 'DENY')")
            )

        if "attendance_records" in tables:
            _deduplicate_attendance_rows(conn)
            conn.execute(
                text(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_student_date
                    ON attendance_records(student_id, date)
                    """
                )
            )
            conn.execute(
                text(
                    """
                    CREATE INDEX IF NOT EXISTS idx_attendance_records_student_created
                    ON attendance_records(student_id, created_at DESC)
                    """
                )
            )

        conn.execute(
            text(
                """
                CREATE TABLE IF NOT EXISTS geofence_events (
                    id VARCHAR NOT NULL PRIMARY KEY,
                    student_id VARCHAR NOT NULL,
                    request_id VARCHAR,
                    event_type VARCHAR NOT NULL,
                    permission_status VARCHAR NOT NULL DEFAULT 'NONE',
                    source_type VARCHAR NOT NULL,
                    note VARCHAR,
                    device_id VARCHAR,
                    network_type VARCHAR,
                    latitude FLOAT,
                    longitude FLOAT,
                    accuracy_meters FLOAT,
                    distance_from_center_meters FLOAT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
        )
        geofence_columns = _table_columns(conn, "geofence_events", dialect)
        if geofence_columns:
            _ensure_column(conn, "geofence_events", "device_id", "VARCHAR", dialect)
            _ensure_column(conn, "geofence_events", "network_type", "VARCHAR", dialect)
            _ensure_column(conn, "geofence_events", "latitude", "FLOAT", dialect)
            _ensure_column(conn, "geofence_events", "longitude", "FLOAT", dialect)
            _ensure_column(conn, "geofence_events", "accuracy_meters", "FLOAT", dialect)
            _ensure_column(conn, "geofence_events", "distance_from_center_meters", "FLOAT", dialect)
        conn.execute(
            text(
                """
                CREATE INDEX IF NOT EXISTS idx_geofence_events_student_created
                ON geofence_events(student_id, created_at DESC)
                """
            )
        )
        conn.execute(
            text(
                """
                CREATE INDEX IF NOT EXISTS idx_geofence_events_request
                ON geofence_events(request_id)
                """
            )
        )
        conn.execute(
            text(
                """
                CREATE INDEX IF NOT EXISTS idx_exit_requests_student_created
                ON exit_requests(student_id, created_at DESC)
                """
            )
        )


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
