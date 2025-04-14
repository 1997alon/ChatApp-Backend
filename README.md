ChatApp - Backend
👉 To go to the frontend: ChatApp-Front

❓ What is this project?
ChatApp is a simple real-time messaging platform that allows users to register, log in, and chat with others through a clean interface.
The system uses a structured backend and a modern frontend to provide a smooth and interactive experience.

All communication is handled via REST API over HTTP, and only registered users can interact with the system.

🧠 Technologies Used
🔙 Backend
Language: Java

Framework: Spring Boot (manually set up, without Maven)

Architecture:

Object-Oriented Programming (OOP) with clean structure and modularity

REST API used for communication between frontend and backend

Thread pooling used to handle multiple requests efficiently

JSON is used for data exchange

🖥️ Frontend
Language: JavaScript

Libraries/Technologies: React, CSS

Features:

User-friendly interface for login and messaging

Displays message history

Built with clean, responsive design

🗄️ Database
Engine: MySQL

Tables:

users – Stores user credentials

messages – Stores all sent and received messages

Other tables as needed for managing chat sessions and user data

✨ Features
👤 User registration and login

💬 Real-time messaging interface

🌐 REST API communication between frontend and backend

🧵 Multithreaded backend with thread pool

🧠 Object-Oriented backend design

🔄 Clear separation between frontend and backend

📄 JSON used for all data exchange

🛠️ Build & Run Instructions
🔧 Prerequisites
Java 17 or higher

MySQL running and configured

All required .jar libraries placed inside the libs/ directory

🏗️ Compile & Run the Server
bash
Copy
Edit
cd C:\Users\bardi\IdeaProjects\ChatAppBackend

javac -cp ".;libs/*;src" src\*.java src\backend\*.java

java -cp ".;libs/*;src" backend.Main
The server will be accessible via HTTP at:
http://localhost:8080
