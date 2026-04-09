import firebase_admin
from firebase_admin import credentials, firestore, auth, storage, messaging
import os

# Get the absolute path to the service account key
base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
cred_path = os.path.join(base_dir, "serviceAccountKey.json")

def initialize_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred, {
            'storageBucket': 'smart-attendance-app-5642d.appspot.com' # Replace with your actual bucket name later if needed
        })
    return firestore.client()

db = initialize_firebase()
