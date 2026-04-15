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
            exit_columns = _table_columns(conn, "exit_requests", dialect)
            if "resolved_at" not in exit_columns:
                conn.execute(text("ALTER TABLE exit_requests ADD COLUMN resolved_at DATETIME"))
            if "left_campus_at" not in exit_columns:
                conn.execute(text("ALTER TABLE exit_requests ADD COLUMN left_campus_at DATETIME"))
            if "returned_campus_at" not in exit_columns:
                conn.execute(text("ALTER TABLE exit_requests ADD COLUMN returned_campus_at DATETIME"))
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
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
        )
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
