# 🏫 School Management System

A lightweight, high-performance School Management Web Application built with pure Java (Core Java SE) and PostgreSQL.

---

## 📌 1. Project & Tech Stack Overview

| Component | Technology | Description |
|---|---|---|
| **Language & Runtime** | Java 17+ (Java SE) | Built entirely in standard Java without external heavy web frameworks. |
| **HTTP Web Server** | `com.sun.net.httpserver.HttpServer` | Built-in JDK lightweight embedded HTTP server (zero heavy servlet container overhead). |
| **Concurrency** | `Executors.newCachedThreadPool()` | Multi-threaded asynchronous request dispatching. |
| **Database** | PostgreSQL 14 / 15 / 16 / 17 | Relational database persistence. |
| **Driver / Connectivity**| JDBC (`postgresql-42.7.13.jar`) | Direct SQL queries and `PreparedStatement` parameterized execution. |
| **Frontend / UI** | Server-Side Rendered HTML5 + CSS | Native server-side template generation with clean CSS card layouts and responsive tables. |
| **Security** | XSS sanitization & SQL injection defense | Custom `escape()` HTML sanitization and parameterized SQL statements. |

---

## 🚀 2. System Architecture & Features

### Core Modules
1. **Dashboard (`/`)**: Main hub displaying cards and navigation to all subsystems.
2. **Student Management (`/students`)**:
   - Create new student (`student_id`, `student_name`, `age`, `class`, `phone`).
   - List all students.
   - Update student records.
   - Delete student records with confirmation prompt.
3. **Teacher Management (`/teachers`)**:
   - Create new teacher (`teacher_id`, `teacher_name`, `subject`, `age`, `phone`).
   - List, update, and delete teacher records.
4. **Subject Management (`/subjects`)**:
   - Create new subject (`subject_id`, `subject_name`, `teacher_name`, `class`).
   - List, update, and delete subject records.

---

## 🛠️ 3. How to Run Locally

### Prerequisites
1. **Java JDK 17 or higher** installed (`java -version`, `javac -version`).
2. **PostgreSQL** installed and running on port `5432`.
3. Create the database:
   ```sql
   CREATE DATABASE school_management;
   ```
   *(Tables are automatically created on server startup)*

### Step 1: Compile the Project
From the project root directory:
```bash
javac -cp "lib/postgresql-42.7.13.jar" -d bin src/WebApp.java
```

### Step 2: Run the Server
Using default configuration:
```bash
java -cp "bin:lib/postgresql-42.7.13.jar" WebApp
```

Or configure custom port / database credentials via environment variables:
```bash
PORT=8080 DB_URL=jdbc:postgresql://localhost:5432/school_management DB_USER=postgres DB_PASSWORD=Digital@5588 java -cp "bin:lib/postgresql-42.7.13.jar" WebApp
```

Open your browser at: **`http://localhost:8080`**

---

## ☁️ 4. How to Deploy

### Option A: Free / Low-Cost PaaS (Render, Railway, Fly.io) - Recommended

1. **Push your code to GitHub**.
2. **Create a PostgreSQL Database** on [Render](https://render.com) or [Railway](https://railway.app).
3. **Create a Web Service**:
   - Connect your GitHub repository.
   - Set **Build Command**:
     ```bash
     mkdir -p bin && javac -cp "lib/postgresql-42.7.13.jar" -d bin src/WebApp.java
     ```
   - Set **Start Command**:
     ```bash
     java -cp "bin:lib/postgresql-42.7.13.jar" WebApp
     ```
4. **Configure Environment Variables**:
   - `PORT`: (Auto-assigned by host, or `8080`)
   - `DATABASE_URL` or `DB_URL`: Your hosted PostgreSQL connection string.
   - `DB_USER`: Your hosted DB user.
   - `DB_PASSWORD`: Your hosted DB password.

---

### Option B: Docker / Container Deployment

Create a `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY lib ./lib
COPY src ./src
RUN mkdir -p bin && javac -cp "lib/*" -d bin src/WebApp.java

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "bin:lib/*", "WebApp"]
```

Run with `docker-compose.yml`:
```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: school_management
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Digital@5588
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      PORT: 8080
      DB_URL: jdbc:postgresql://db:5432/school_management
      DB_USER: postgres
      DB_PASSWORD: Digital@5588
    depends_on:
      - db

volumes:
  pgdata:
```
Run both app and database with one command:
```bash
docker compose up --build
```

