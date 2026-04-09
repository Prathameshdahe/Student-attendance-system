import os
import firebase_admin
from firebase_admin import credentials, auth, firestore

# Initialize Firebase Admin
cred = credentials.Certificate("serviceAccountKey.json")
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()

def wipe_and_seed_users():
    print("Fetching existing users to wipe...")
    try:
        # Get all users and delete them
        for user in auth.list_users().iterate_all():
            auth.delete_user(user.uid)
            print(f"Deleted auth for {user.email}")
            
        # Delete user docs in firestore
        docs = db.collection('users').stream()
        for doc in docs:
            doc.reference.delete()
        print("Wiped Firestore users.")

    except Exception as e:
        print("Error wiping existing data:", e)

    print("\n--- Creating New Records ---")
    
    records = [
        {
            "role": "ADMIN",
            "name": "Antigravity Admin",
            "email": "admin@bvucoep.edu.in",
            "password": "Admin@123",
            "phone": "N/A"
        },
        {
            "role": "STUDENT",
            "name": "Manamrit singh",
            "email": "msingh23-comp@bvucoep.edu.in",
            "password": "Student@123", # standard password for the demo
            "phone": "+918826611487",
            "roll": "104",
            "prn": "2314110207",
            "branch": "CE"
        }
    ]

    for data in records:
        try:
            # 1. Create in Firebase Auth
            user = auth.create_user(
                email=data["email"],
                password=data["password"],
                display_name=data["name"]
            )
            print(f"[SUCCESS] Created auth for: {user.email} (UID: {user.uid})")

            # 2. Create in Firestore
            doc_data = {
                "name": data["name"],
                "email": data["email"],
                "role": data["role"]
            }
            if data["role"] == "STUDENT":
                doc_data["roll_number"] = data["roll"]
                doc_data["prn"] = data["prn"]
                doc_data["branch"] = data["branch"]
                doc_data["phone"] = data["phone"]

            db.collection("users").document(user.uid).set(doc_data)
            print(f"   -> Firestore document created for {user.email}")
        
        except Exception as e:
            print(f"[ERROR] Failed to create {data['email']}: {e}")

if __name__ == "__main__":
    print("Welcome to Database Setup Script.")
    wipe_and_seed_users()
    print("\n--- Credentials for Demo ---")
    print("Admin: admin@bvucoep.edu.in / Admin@123")
    print("Student: msingh23-comp@bvucoep.edu.in / Student@123")
