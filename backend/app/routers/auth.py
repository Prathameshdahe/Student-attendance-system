from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.access_policy import enforce_admin_access
from app.core.security import verify_password, create_access_token, decode_token, hash_password
from app.models.user import User

router = APIRouter(prefix="/auth", tags=["auth"])
security = HTTPBearer()


# ─── Request / Response Schemas ────────────────────────────────────────────────

class LoginRequest(BaseModel):
    email: str
    password: str


class RegisterRequest(BaseModel):
    email: str
    password: str
    name: str
    role: str = "STUDENT"  # STUDENT or ADMIN
    roll_number: str = ""


class LoginResponse(BaseModel):
    token: str
    role: str
    name: str
    email: str
    user_id: str


class UserProfile(BaseModel):
    id: str
    email: str
    name: str
    role: str
    roll_number: str


# ─── Helper: get current user from JWT ─────────────────────────────────────────

def get_current_user(
    request: Request,
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    token = credentials.credentials
    payload = decode_token(token)
    if not payload:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    user = db.query(User).filter(User.id == payload.get("sub")).first()
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    enforce_admin_access(request, user)
    return user


# ─── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/login", response_model=LoginResponse)
def login(req: LoginRequest, request: Request, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == req.email.lower().strip()).first()
    if not user or not verify_password(req.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    if not user.is_active:
        raise HTTPException(status_code=403, detail="Account is deactivated")
    enforce_admin_access(request, user)

    token = create_access_token({"sub": user.id, "role": user.role, "email": user.email})
    return LoginResponse(
        token=token,
        role=user.role,
        name=user.name,
        email=user.email,
        user_id=user.id
    )


@router.post("/register", status_code=201)
def register(req: RegisterRequest, db: Session = Depends(get_db),
             current_user: User = Depends(get_current_user)):
    """Admin-only: create a new user account."""
    if current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Only admins can register users")
    existing = db.query(User).filter(User.email == req.email.lower().strip()).first()
    if existing:
        raise HTTPException(status_code=409, detail="Email already registered")
    new_user = User(
        email=req.email.lower().strip(),
        hashed_password=hash_password(req.password),
        name=req.name,
        role=req.role.upper(),
        roll_number=req.roll_number
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {"message": f"User '{req.name}' created successfully", "user_id": new_user.id}


@router.get("/me", response_model=UserProfile)
def get_me(current_user: User = Depends(get_current_user)):
    return UserProfile(
        id=current_user.id,
        email=current_user.email,
        name=current_user.name,
        role=current_user.role,
        roll_number=current_user.roll_number or ""
    )
