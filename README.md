# 💬 ChatApp - Backend
👉 To go to the frontend: "" *[ChatApp-Front](https://github.com/1997alon/ChatApp-Front)*

---

❓ What is this project?
ChatApp is a simple real-time messaging platform where users can:

🧾 Register and log in

💬 Chat with others through a clean and responsive interface

🌐 Communicate entirely through a structured REST API over HTTP

The system uses a modular Object-Oriented Java backend, paired with a modern React frontend, to deliver an efficient and interactive chat experience.

## 🧠 Technologies Used
### 🔙 Backend
Language: Java

Framework: Spring Boot (manually set up — no Maven)

Architecture:

Built with Object-Oriented Programming (OOP) principles

Communication via REST API

Thread pooling used to handle multiple concurrent requests

JSON for all data exchange

### 🖥️ Frontend
Language: JavaScript

Technologies: React, CSS

Features:

Clean and intuitive UI for login and messaging

Displays message history

Responsive design that works across devices

### 🗄️ Database
Engine: MySQL

Tables:

users – Stores user credentials

messages – Stores sent/received chat messages

Additional tables may exist for session and user metadata

### ✨ Features
👤 User registration and login

💬 Real-time messaging interface

🌐 REST API communication between frontend and backend

🧵 Multithreaded backend using a thread pool

🧠 Clean and modular OOP-based design

🔄 Full separation between frontend and backend

📄 Uses JSON for all data formats

## 🛠️ Build & Run Instructions
🔧 Prerequisites
Java 17 or higher

MySQL installed, running, and properly configured

All required .jar files placed in the libs/ directory

🏗️ Compile & Run the Backend
bash
Copy
Edit
cd C:\Users\bardi\IdeaProjects\ChatAppBackend

javac -cp ".;libs/*;src" src\*.java src\backend\*.java

java -cp ".;libs/*;src" backend.Main
Once running, the server will be available at:
📡 http://localhost:8080
