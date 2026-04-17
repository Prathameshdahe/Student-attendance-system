# Render Setup

## Stack
This project now uses one primary deployment path:

- Render for the FastAPI backend
- Neon for PostgreSQL
- Cloudflare only if you later want a custom domain

Current live backend:

```text
https://kiwi-smart-attendance-api.onrender.com
```

## Required Render Environment Variables

```env
DATABASE_URL=<your-neon-postgres-connection-string>
SECRET_KEY=<generate-a-long-random-secret>
ADMIN_ALLOWED_CLIENT_TYPES=
ADMIN_ALLOWED_NETWORKS=
ADMIN_ALLOWED_DEVICE_IDS=
```

Leave the `ADMIN_ALLOWED_*` values empty if you want open login from any network.

## Neon Setup
1. Create a Neon project and database.
2. Copy the pooled PostgreSQL connection string.
3. Paste it into Render as `DATABASE_URL`.

## Render Setup
1. Push the repo to GitHub when you want Render to rebuild from the latest code.
2. In Render, create a Blueprint from this repository.
3. Use [render.yaml](/C:/Users/DELL/Desktop/smartattendance/render.yaml).
4. Use [Dockerfile](/C:/Users/DELL/Desktop/smartattendance/backend/Dockerfile) for the web service.
5. Set `DATABASE_URL` to the Neon connection string.
6. Let Render generate `SECRET_KEY`, or provide your own.
7. Deploy.

## Health Checks
After deploy, verify:

```text
GET  /                  -> {"status":"ok","service":"KIWI Smart Attendance API v3.0"}
POST /auth/login        -> reachable
GET  /attendance/live/today
GET  /attendance/geofence-events
```

## Android App Setup
The Android app already points to Render by default in [build.gradle.kts](/C:/Users/DELL/Desktop/smartattendance/frontend/app/build.gradle.kts).

If you ever need to override the backend URL for a build:

```powershell
.\gradlew.bat assembleDebug -PkiwiBackendUrl=https://your-api.example.com/
```

## Admin Portal Setup
Run:

```powershell
.\start.ps1
```

Then open:

```text
http://localhost:8080
```

The admin portal is already wired to the Render API in [index.html](/C:/Users/DELL/Desktop/smartattendance/admin-portal/index.html).

## Geofence Setup
The student app geofence uses:

- center: `18.458444, 73.855922`
- radius: `325 meters`

Relevant files:

- [GeofenceManager.kt](/C:/Users/DELL/Desktop/smartattendance/frontend/app/src/main/java/com/smartattendance/smartattendance/service/GeofenceManager.kt)
- [GeofenceBroadcastReceiver.kt](/C:/Users/DELL/Desktop/smartattendance/frontend/app/src/main/java/com/smartattendance/smartattendance/service/GeofenceBroadcastReceiver.kt)
- [GeofenceUploadWorker.kt](/C:/Users/DELL/Desktop/smartattendance/frontend/app/src/main/java/com/smartattendance/smartattendance/service/GeofenceUploadWorker.kt)

For reliable alerts on physical phones:

- grant fine location
- grant background location
- allow notifications
- disable battery optimization for the app
- install the fresh debug build after backend URL changes

## Optional Cloudflare Custom Domain
You do not need Cloudflare for the app to work.

Use Cloudflare later only if you want a stable custom domain such as:

```text
https://api.kiwiattendance.in
```

Typical flow:

1. Add the custom domain in Render.
2. Add the DNS record in Cloudflare exactly as Render instructs.
3. Wait for domain verification.
4. Replace the Render URL in Android and the portal with the custom domain.

## Recommended Admin Lockdown
If you want student access open everywhere but admin access limited to your Vivo phone:

```env
ADMIN_ALLOWED_CLIENT_TYPES=android-app
ADMIN_ALLOWED_DEVICE_IDS=<trusted-device-key-from-admin-home-screen>
ADMIN_ALLOWED_NETWORKS=
```

The admin app shows the trusted device key on the home screen.
