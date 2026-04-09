from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import random
import time
import firebase_admin
from firebase_admin import firestore

router = APIRouter(prefix="/auth", tags=["auth"])

# Using a generic dev email for sending OTPs.
# Replace with actual App Password if running in production
SENDER_EMAIL = "antigravity.demo@gmail.com"
SENDER_PASS = "czzy uvqg xzqw tzjk" # App password

class SendOtpRequest(BaseModel):
    email: str

class VerifyOtpRequest(BaseModel):
    email: str
    otp: str

@router.post("/send-otp")
def send_otp(req: SendOtpRequest):
    # Generate 6 digit OTP
    otp_code = str(random.randint(100000, 999999))
    
    # Store OTP in Firestore with expiration (5 mins)
    db = firestore.client()
    try:
        db.collection("otp_tokens").document(req.email).set({
            "otp": otp_code,
            "expires_at": time.time() + 300
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail="Database Error")
    
    # Send Email
    try:
        msg = MIMEMultipart()
        msg['From'] = f"Smart Attendance <{SENDER_EMAIL}>"
        msg['To'] = req.email
        msg['Subject'] = "Your Login Verification Code"
        
        body = f"""
        <html>
            <body>
                <h2>Smart Attendance System</h2>
                <p>Hello,</p>
                <p>Your one-time verification code is:</p>
                <h1 style="color: #6C47FF;">{otp_code}</h1>
                <p>This code will expire in 5 minutes.</p>
                <p>If you did not request this code, please ignore this email.</p>
            </body>
        </html>
        """
        msg.attach(MIMEText(body, 'html'))
        
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.starttls()
        server.login(SENDER_EMAIL, SENDER_PASS)
        server.send_message(msg)
        server.quit()
        
        return {"message": "OTP sent successfully"}
    except Exception as e:
        print(f"SMTP Error: {e}")
        raise HTTPException(status_code=500, detail="Failed to send email OTP.")

@router.post("/verify-otp")
def verify_otp(req: VerifyOtpRequest):
    db = firestore.client()
    doc_ref = db.collection("otp_tokens").document(req.email)
    doc = doc_ref.get()
    
    if not doc.exists:
        raise HTTPException(status_code=400, detail="OTP not found or expired")
    
    data = doc.to_dict()
    if time.time() > data.get("expires_at", 0):
        doc_ref.delete()
        raise HTTPException(status_code=400, detail="OTP expired")
        
    if str(data.get("otp")) != req.otp:
        raise HTTPException(status_code=400, detail="Invalid OTP")
        
    # Success, consume the OTP
    doc_ref.delete()
    return {"message": "OTP verified successfully"}
