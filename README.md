<div align="center">
  <img src="https://img.icons8.com/color/96/000000/kiwi-bird.png" width="120"/>
  <h1>🥝 KIWI Smart Attendance System</h1>
  <p><b>Next-Generation, Geofence-Powered Campus Attendance & Security System</b></p>

  [![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/)
  <br/><br/>
  ![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
  ![Android](https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
  ![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi)
  ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
  ![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
</div>

<br/>

## 🌟 The Vision

**KIWI** completely eliminates fraudulent proxies and manual roll-calls. Custom built for **Bharati Vidyapeeth College of Engineering (Pune)**, KIWI uses hyper-accurate location geofencing combined with offline-resilient background syncing to track when students enter or leave the campus boundary. 

Unlike traditional Bluetooth or QR code systems, KIWI is 100% autonomous, secure, and cloud-native.

<br/>

## 🎯 Groundbreaking Features

<details>
<summary><b>📍 Invisible Geofencing (Zero-Touch Check-ins)</b></summary>
<br/>
The Android app tracks student movement strictly within the physical latitude/longitude boundaries of the BVCOE campus. Automatic check-ins occur seamlessly without the student ever needing to take their phone out of their pocket.
</details>

<details>
<summary><b>📶 100% Offline Resilience (WorkManager Queueing)</b></summary>
<br/>
Losing cell service at the gate? No problem. KIWI intercepts geofence events locally using native Android Broadcast Receivers and <code>SharedPreferences</code>. It automatically syncs them up to the cloud via Android <code>WorkManager</code> the exact second a connection is restored.
</details>

<details>
<summary><b>🌐 Live Monitoring React Portal</b></summary>
<br/>
A real-time, beautiful React interface allowing administrators to view live campus occupancy, track individual student timelines, approve/deny early exit requests, and download perfectly formatted PDF attendance reports.
</details>

<details>
<summary><b>🛡️ Fortress Security & Cloud Scalability</b></summary>
<br/>
Powered by a hardened Python FastAPI backend, bcrypt-hashed passwords, and JWT Tokens. All attendance data is stored permanently in a horizontally scalable PostgreSQL Cloud instance hosted on Railway.
</details>

<hr>

## 🛠️ Architecture Stack

### Mobile Frontend 📱
* **Language:** Kotlin
* **Tools:** Retrofit2 (API Networking), Google Play Services (Location & Geofencing), WorkManager (Background Sync)

### Cloud Backend ☁️
* **Framework:** FastAPI (Python)
* **ORM:** SQLAlchemy
* **Database:** PostgreSQL (Production) / SQLite (Local Dev)
* **Hosting:** Railway.app (CI/CD via GitHub)
* **Security:** JWT Auth & PassLib (Bcrypt)

### Web Dashboard 💻
* **Tech:** Pure React + Babel standalone (Zero build step!)
* **Styling:** Vanilla CSS + Inter & JetBrains Mono Fonts
* **Exports:** PDF generation via `jsPDF`

<hr>

## 🚀 Live Environment

The production backend API is currently live on Railway!

* **API Endpoint:** `https://fabulous-gratitude-production-5eb1.up.railway.app`
* **Web Admin Portal:** Double click `start.ps1` to instantly launch your dashboard locally pointing to the cloud DB.

---

## 💻 Local Development / Testing

### 1. Launch the Android App
1. Open the project in **Android Studio**.
2. Connect your physical testing device (ensure Location Services are fully enabled).
3. Hit the Green `Play` button.
4. Login using a generated student credential:
   ```text
   Email: padahe23-comp@bvucoep.edu.in
   Password: Student@1234
   ```

### 2. View the Admin Interface
Access the real-time observer panel natively by double clicking the `start.ps1` file on Windows, which automatically spawns the React portal targeting the cloud infrastructure.
```text
Email: admin@bvucoep.edu.in
Password: Admin@1234
```

<hr>

<div align="center">
<i>Built with ❤️ for a smarter, safer campus.</i>
</div>
