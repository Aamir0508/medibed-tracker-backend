# MediBed Tracker - Backend & Database

Spring Boot 3.3 REST API + MySQL schema for the MediBed Tracker project (Team Pikachu).
Layered exactly as in the backend slides: **Controller -> Service -> Repository -> Database**.

```
medibed-tracker/
├── pom.xml
├── sql/medibed_tracker_schema.sql        <- MySQL: DDL, seed data, sample DML (run in Workbench)
├── src/main/java/com/medibed/tracker/
│   ├── controller/   Hospital, Doctor, Auth, Appointment, Ambulance, Notification, Analytics
│   ├── service/      business rules (available-now, wait time, bed alerts, SSE feed, analytics)
│   ├── repository/   Spring Data JPA interfaces
│   ├── model/        6 entities + bed_snapshots, enums
│   ├── dto/Dtos.java request/response shapes + validation rules
│   ├── security/     JWT, BCrypt, role + own-hospital checks
│   ├── exception/    global exception handler (one error shape)
│   └── config/       SecurityConfig, DataSeeder
├── src/main/resources/ application.properties, application-mysql.properties, seed/*.json
└── src/test/         JUnit 5 + MockMvc tests
```

## 1. Run it (needs JDK 17+ and Maven)

**Quickest - no database install (H2 in memory):**
```bash
mvn spring-boot:run
```
API is at http://localhost:8080 . On first start it creates the super admin and loads
**28 hospitals + 38 doctors** (same data as your frontend).

**With MySQL (what the database slides describe):**
1. In MySQL Workbench open `sql/medibed_tracker_schema.sql` and run it (creates `medibed_db`, tables, data).
2. Start the app with the mysql profile:
```bash
# Windows PowerShell:  $env:DB_PASSWORD="your_mysql_password"
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
(IDE users: install the **Lombok** plugin; IntelliJ has it built in.)

**Default login:** `superadmin` / `Admin@12345` (change via `ADMIN_PASSWORD`). Set `JWT_SECRET` (32+ chars) for anything real.

Run the tests: `mvn test`

## 2. Endpoints

Access: **Public** = no login. **Staff** = JWT; a `HOSPITAL_ADMIN` can only touch their own hospital. **Super** = `SUPER_ADMIN` only.

### Hospitals `/api/hospitals`
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/hospitals` | Register a hospital | Super |
| GET | `/api/hospitals` | List all | Public |
| GET | `/api/hospitals/{id}` | One hospital + its doctors | Public |
| GET | `/api/hospitals/area/{area}` | Filter by area | Public |
| GET | `/api/hospitals/type/{type}` | `Government` / `Private` | Public |
| GET | `/api/hospitals/search?keyword=` | Search by name | Public |
| PUT | `/api/hospitals/{id}` | Update details | Staff |
| PATCH | `/api/hospitals/{id}/beds` | Push live bed counts | Staff |
| DELETE | `/api/hospitals/{id}` | Remove (cascades to children) | Super |
| GET | `/api/hospitals/stream` | Live bed feed (Server-Sent Events) | Public |

### Doctors `/api/doctors`
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/doctors/hospital/{hospitalId}` | Add a doctor | Staff |
| GET | `/api/doctors` | All doctors | Public |
| GET | `/api/doctors/hospital/{hospitalId}` | Doctors at a hospital | Public |
| GET | `/api/doctors/specialization/{spec}` | Filter by specialization | Public |
| GET | `/api/doctors/hospital/{id}/available-now` | AVAILABLE and on shift right now | Public |
| GET | `/api/doctors/hospital/{id}/top-rated?limit=3` | Highest rated | Public |
| PUT | `/api/doctors/{id}` | Update profile / shift | Staff |
| PATCH | `/api/doctors/{id}/status` | AVAILABLE / BUSY / ON_LEAVE (+ queueCount) | Staff |
| DELETE | `/api/doctors/{id}` | Remove | Staff |

### Auth `/api/auth`
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/auth/login` | Get a JWT | Public |
| POST | `/api/auth/register` | Create an admin account | Super |
| POST | `/api/auth/refresh` | Swap token for a fresh one | Any logged-in |
| POST | `/api/auth/logout` | Invalidate the current token | Any logged-in |

### Appointments `/api/appointments`
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/appointments` | Book (`patientName, patientPhone, doctorId, slotTime`) | Public |
| GET | `/api/appointments/{id}` | Details | Staff |
| GET | `/api/appointments/doctor/{id}` | A doctor's appointments | Staff |
| PUT | `/api/appointments/{id}/reschedule` | New `slotTime` | Staff |
| DELETE | `/api/appointments/{id}` | Cancel (kept as CANCELLED) | Staff |

### Ambulance `/api/ambulance`
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/ambulance/request` | Raise a pickup request | Public |
| GET | `/api/ambulance/{id}/status` | Status + ETA (no patient details) | Public |
| GET | `/api/ambulance/nearest?hospitalId=` | Hospitals with a free emergency bed | Public |
| PATCH | `/api/ambulance/{id}/status` | Update dispatch status / ETA | Staff |

### Notifications & Analytics
| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| GET | `/api/notifications/hospital/{id}` | A hospital's notifications | Staff |
| PATCH | `/api/notifications/{id}/read` | Mark read | Staff |
| GET | `/api/analytics/hospital/{id}/occupancy?days=7` | Bed availability trend | Staff |
| GET | `/api/analytics/hospital/{id}/appointments?from=&to=` | Appointment volume report | Staff |
| GET | `/api/analytics/doctors/top-rated?limit=5` | Top doctors network-wide | Staff |
| GET | `/api/analytics/ambulance/response-times` | Avg time to dispatch | Super |

## 3. Try it

```bash
# public: all hospitals, doctors free right now at hospital 1
curl http://localhost:8080/api/hospitals
curl http://localhost:8080/api/doctors/hospital/1/available-now

# login, then push a live bed update
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"Admin@12345"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -X PATCH localhost:8080/api/hospitals/1/beds -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"generalBeds":20,"icuBeds":1,"oxygenBeds":15,"nicuBeds":5,"emergencyBeds":4}'

# create a hospital admin limited to hospital 1
curl -X POST localhost:8080/api/auth/register -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"kokilaben.admin","password":"Strong@123","role":"HOSPITAL_ADMIN","hospitalId":1}'

# book an appointment (slotTime must be in the future and inside the doctor's shift)
curl -X POST localhost:8080/api/appointments -H "Content-Type: application/json" \
  -d '{"patientName":"Test Patient","patientPhone":"9876543210","doctorId":1,"slotTime":"2026-12-01T10:00:00"}'
```
Watch live updates in a browser console: `new EventSource("http://localhost:8080/api/hospitals/stream").addEventListener("bed-update", e => console.log(JSON.parse(e.data)))`

## 4. Using it from your frontend

`GET /api/hospitals` returns objects shaped like the array already inside the frontend:
`{ id, name, area, address, phone, type, beds: {General, ICU, Oxygen, NICU, Emergency}, facilities: [...] , ...}`
so it can replace the hard-coded `hospitals` array with a `fetch()`. Admin actions map to:
form save -> `POST`/`PUT /api/hospitals`, bed form -> `PATCH /api/hospitals/{id}/beds`
(body keys `generalBeds, icuBeds, oxygenBeds, nicuBeds, emergencyBeds`), delete -> `DELETE /api/hospitals/{id}`.
Those write calls need `Authorization: Bearer <token>` from `/api/auth/login`.

## 5. Validation & errors (slide 13)
Hospital name 3-120 chars; phone pattern; bed counts integer >= 0; specialization from an allowed list;
doctor status AVAILABLE/BUSY/ON_LEAVE; rating 0.0-5.0; appointment slot in the future and inside the doctor's shift.
Every error has one shape:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "fieldErrors": { "generalBeds": "must be greater than or equal to 0" } }
```
(401 not logged in, 403 not allowed, 404 not found, 409 conflict such as a double-booked slot.)

## 6. Where this differs from the slides - know these for your viva
* **28 hospitals, not 30**: the data follows your frontend (28). The backend deck's "30" came from the demo page; 3 demo doctors whose hospitals aren't in the frontend were left out (38 doctors loaded). Change slide 2 to 28, or add 2 more hospitals to `seed/hospitals.json`.
* **One extra table, `bed_snapshots`**, written on every bed update so the occupancy trend endpoint has history. The six core entities are unchanged.
* **"Nearest" hospital is area-based** (same area first, then most free emergency beds); real driving distance is the roadmap item.
* **Notifications implemented**: low-bed alerts, new/changed/cancelled appointments, ambulance status. **Not implemented**: timed patient appointment reminders (would need a scheduler + a reminder-sent flag).
* **Staff-only**: viewing/rescheduling/cancelling an appointment requires login (otherwise anyone could read patient phone numbers by guessing ids). Patient self-service cancel would need an OTP step.
* **Emergency phone is optional** on hospital create/update, because the frontend admin form doesn't collect it.
* Passwords are BCrypt-hashed; the first super admin is created by the app on startup, not by the SQL file.
