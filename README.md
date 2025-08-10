# NexResQ – Smart Emergency Response System 🚑

**Optimized Ambulance Dispatch & Traffic Management**
Developed by **Team Square Root**

---

## 📌 Overview

NexResQ is a **smart and AI-enhanced emergency response system** designed to reduce ambulance response times in urban environments.
It automates ambulance allocation using **Priority Scheduling**, optimizes routing via **Dijkstra’s/A**\* algorithms, and employs **Geofencing with Spatial Hashing** to clear traffic in advance.

The system also includes a **🚨 real-time traffic clearance alert feature** that **sends notifications to all nearby vehicles within 2–4 km** of the ambulance’s route, ensuring the path is cleared for faster emergency response.

---

## 🚀 Features

* **Dual-Purpose Mobile App** – Interfaces for both patients & ambulance drivers
* **Dynamic Dispatch System** – AI-assisted priority queue & time-decay scheduling for emergencies
* **Shortest Path Routing** – Real-time navigation avoiding traffic congestion
* **🚨 Traffic Clearance Alerts** – **Sends instant notifications to all nearby vehicles (within 2–4 km) to clear the way for an approaching ambulance**
* **Real-Time Synchronization** – Powered by Firebase Realtime Database & PHP APIs
* **Secure Authentication** – Firebase Authentication for safe access

---

## 🏗 System Architecture

### 1. **Frontend (Android App)**

* Built with **Android Studio** (XML + Java)
* Role-based UI for patients and ambulance drivers

### 2. **Backend (PHP APIs)**

* **PHP** for API handling and server-side logic
* **Firebase Realtime Database + MySQL** for data storage

### 3. **Algorithmic Core**

* **AI-based Priority Scheduling** for ambulance dispatch
* **Dijkstra / A**\* for optimal routing
* **Spatial Hashing** for geofencing and alert zones

### 4. **Real-Time Communication**

* **Firebase** for instant updates
* **Google Maps API** for live tracking & navigation

---

## 📂 Tech Stack

* **Frontend:** Java, XML, Android Studio
* **Backend:** PHP
* **Database:** Firebase Realtime Database, MySQL
* **APIs:** Google Maps API, Firebase API
* **Algorithms:** Dijkstra’s, A\*, Priority Scheduling, Spatial Hashing

---

## 📸 Screenshots

*(Add screenshots of your app here)*

---

## 📥 Installation

### Prerequisites:

* Android Studio
* PHP 7.4+ and Apache/Nginx Server (XAMPP/WAMP recommended)
* MySQL Server
* Firebase Project Setup

### Steps:

1. **Clone the repository**

   ```bash
   git clone https://github.com/sudhanshushekhar-cw/NexResQ.git
   cd NexResQ
   ```

2. **Backend (PHP API) Setup**

   * Place the `php-api` folder in your server’s `htdocs` (XAMPP) or `www` (WAMP) directory
   * Import the provided `.sql` file into MySQL
   * Update `config.php` with database & Firebase credentials

3. **Android App Setup**

   * Open the project in Android Studio
   * Update Firebase config in `google-services.json`
   * Set the base URL in API service class to your server URL
   * Build & run on an Android device/emulator

---

## 📊 Testing

| Test Case                    | Status    | Notes                                                      |
| ---------------------------- | --------- | ---------------------------------------------------------- |
| Shortest Path Accuracy       | ✅ Pass    | Validated against test route                               |
| Priority Scheduling          | ✅ Pass    | Correct urgency-based order                                |
| Notification to Volunteers   | ✅ Pass    | Works with Firebase Cloud Messaging                        |
| **Traffic Clearance Alerts** | ✅ Pass    | Successfully notifies nearby vehicles within 2–4 km radius |
| Geofencing Alert Simulation  | ⚠ Partial | Needs tuning in dense urban zones                          |

---

## 📌 Pending Improvements

* Enhance **Geofencing accuracy** in urban areas
* Improve **traffic rerouting** logic using better data sources
* More **cross-device Android testing**

---

## 👥 Contributors

* **Sudhanshu Shekhar** – Lead Developer (Dispatch Logic, Geofencing, Firebase Integration)
* **Sital Jyala** – Routing Algorithms, Firebase Data Sync

---

## 📜 License

Custom License Agreement
Copyright © 2025 Team Square Root & Coaching Wood. All rights reserved.

Project: NexResQ – Smart Emergency Response System

Permission is hereby granted ONLY to view the source code for personal and educational purposes.
You may NOT, without explicit written permission from Team Square Root and Coaching Wood:

* Use this code or any part of it for commercial or non-commercial purposes.
* Modify, redistribute, sublicense, or host this project.
* Remove or alter any copyright or license notices.

This software is proprietary to Coaching Wood and its contributors.
Unauthorized use, reproduction, modification, or distribution of this software or its components will result in legal action under applicable laws.

For permissions, contact:
📧 [coachingwoodindia@gmail.com](mailto:coachingwoodindia@gmail.com)
🌐 [www.coachingwood.in](https://www.coachingwood.in)
🔒 [Privacy Policy](https://coachingwood.in/privacy-policy.html)


