# Student Management System

A production-grade Java desktop application for managing student records, built with Java 17, Swing (FlatLaf), MySQL 8, and Maven.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Java Swing + FlatLightLaf 3.4.1 |
| Service | Plain Java (validation, business rules) |
| DAO | JDBC with PreparedStatement, try-with-resources |
| Connection pool | HikariCP 5.1.0 |
| Database | MySQL 8 |
| Logging | SLF4J + Logback |
| Tests | JUnit 5 + Mockito |
| Build | Maven 3.8+, maven-shade-plugin (fat JAR) |

---

## ER Diagram

```
┌──────────────────────────────────────────────┐
│ students                                      │
├──────────────────┬───────────────────────────┤
│ student_id (PK)  │ VARCHAR(20)  e.g. CS2021001│
│ name             │ VARCHAR(100)              │
│ branch           │ VARCHAR(60)  CSE/ECE/…    │
│ year             │ TINYINT      1–4           │
│ phone (UQ)       │ VARCHAR(15)  10 digits     │
│ marks            │ DECIMAL(5,2) 0–100         │
│ created_at       │ DATETIME     auto-set      │
│ updated_at       │ DATETIME     auto-update   │
└──────────────────┴───────────────────────────┘
```

Indexes: `idx_students_name`, `idx_students_branch`

---

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8 running on `localhost:3306`

---

## Setup

### 1. Create the database schema

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

### 2. Configure credentials

```bash
cp config.properties.example src/main/resources/config.properties
```

Edit `src/main/resources/config.properties` and fill in your MySQL password:

```properties
db.url=jdbc:mysql://localhost:3306/sms_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_PASSWORD_HERE
```

> **Note**: `config.properties` is gitignored. Never commit it.

---

## Running the Application

### Via Maven (development)

```bash
cd student-management-system
mvn compile
mvn exec:java -Dexec.mainClass=com.sms.Main
```

### Via runnable JAR

```bash
mvn package -DskipTests
java -jar target/student-management-system-1.0.0-shaded.jar
```

---

## Running Tests

```bash
mvn test
```

Test classes:
- `ValidatorTest` — parameterised field-level validation rules, 20+ test cases
- `StudentServiceTest` — service logic with Mockito-mocked DAO, no real DB needed

---

## Features

| Feature | Details |
|---|---|
| Add student | Ctrl+N or toolbar; field-level inline validation |
| Edit student | Select row → toolbar or double-click; ID is read-only |
| Delete student | Select row → Delete key or toolbar; confirmation dialog |
| Search | Partial name / branch filter / year filter; 300 ms debounce |
| Live summary | Total count, average marks, top scorer — updates with every search |
| Sort | Click any column header to sort ascending/descending |
| Export CSV | Exports current filtered view; UTF-8 BOM for Excel compatibility |
| Error handling | DB-down and duplicate-ID/phone shown as clear error dialogs |

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| Ctrl+N | Add new student |
| Ctrl+F | Focus search field |
| Delete | Delete selected student |
| Enter | Submit form (in dialog) |
| Escape | Close dialog |

---

## Architecture

```
UI Layer      → never touches JDBC
   ↓  SwingWorker
Service Layer → validates, enforces business rules
   ↓
DAO Layer     → PreparedStatement only, try-with-resources
   ↓
MySQL 8 via HikariCP
```

Package layout: `com.sms.{model, dao, service, ui, util, exception}`

---

## Project Structure

```
student-management-system/
├── pom.xml
├── README.md
├── config.properties.example
├── database/
│   ├── schema.sql
│   └── seed.sql
└── src/
    ├── main/
    │   ├── java/com/sms/
    │   │   ├── Main.java
    │   │   ├── model/Student.java
    │   │   ├── dao/{StudentDAO, StudentDAOImpl}.java
    │   │   ├── service/StudentService.java
    │   │   ├── ui/{MainFrame, StudentFormDialog, StudentTablePanel,
    │   │   │       SearchBar, StatusBar}.java
    │   │   ├── util/{DBConnection, Validator, Constants}.java
    │   │   └── exception/{ValidationException, DuplicateStudentException,
    │   │                   DataAccessException}.java
    │   └── resources/
    │       ├── config.properties   ← gitignored
    │       └── logback.xml
    └── test/java/com/sms/
        ├── util/ValidatorTest.java
        └── service/StudentServiceTest.java
```
