# ParkTag – Project Progress Report

---

## 1. Project Information

**Project Name:** ParkTag
**Tagline:** Tag your spot. Find your car.
**Platform:** Native Android (Java)
**Development Environment:** Android Studio

### Problem Statement

Finding a parked car in large parking lots, shopping malls, airports, and multi-level garages is a common problem. ParkTag solves this by allowing users to save their parking location digitally the moment they leave their vehicle, and then retrieve it later with a map, distance indicator, and turn-by-turn directions.

### System Design

ParkTag is built as a native Android application with no remote backend server. All data is stored locally on the device using Room (an abstraction over SQLite). The application is divided into four layers:

**Data Layer (Oguzhan)**
- `ParkingSession` — Room entity representing a single parking record. Fields include latitude, longitude, GPS accuracy, timestamps (capturedAt, createdAt, updatedAt, closedAt), parking level, section, notes, photo path, and reminder time.
- `ParkingSessionDao` — Data Access Object defining insert, update, delete, and query operations.
- `ParkingDatabase` — Singleton Room database instance.
- `ParkingRepository` — Repository that wraps all DAO operations and executes them on a background thread using `ExecutorService`, then returns results to the UI thread via `Handler`.
- `ParkingService` — Service layer that validates all incoming data before passing it to the repository.

**UI Layer (Oguzhan)**
- `MainActivity` — Home screen. Loads the latest open parking session and displays its details. Provides buttons to save a new location, view on map, clear the current session, and browse history.
- `SaveParkingActivity` — Form screen for entering optional level, section, and notes before saving. Receives GPS coordinates from Will's location component.
- `HistoryActivity` — Lists all past parking sessions using a `RecyclerView`.
- `ParkingHistoryAdapter` — RecyclerView adapter for the history list.
- `MapActivity` — Loads the saved parking session from the database and passes coordinates to Will's Google Maps component.

**Location and Navigation Layer (Will)**
- GPS permissions and location capture via `FusedLocationProviderClient`
- Google Maps SDK integration
- Parking marker display
- Distance calculation
- Directions to the saved location

**Database Schema**

| Field         | Type    | Description                        |
|---------------|---------|------------------------------------|
| id            | long    | Auto-generated primary key         |
| latitude      | double  | GPS latitude                       |
| longitude     | double  | GPS longitude                      |
| accuracyMeters| float   | GPS accuracy in metres (optional)  |
| capturedAt    | long    | When GPS coordinates were captured |
| createdAt     | long    | When the record was saved          |
| updatedAt     | long    | When the record was last modified  |
| closedAt      | long    | When the session was ended (nullable) |
| level         | String  | Parking level (optional)           |
| section       | String  | Parking section (optional)         |
| note          | String  | User notes, max 280 characters     |
| photoPath     | String  | Path to parking photo (optional)   |
| reminderAt    | long    | Scheduled reminder time (nullable) |

---

## 2. Completed Work and Upcoming Phases

### Phase 1 – Setup (Complete)
- Android project created in Java with Empty Views Activity
- Room 2.6.1 and RecyclerView dependencies added to Gradle
- Git repository connected to GitHub
- Branch strategy established: feature branches merged into main

### Phase 2 – Data Layer (Complete)
- Full Room database implementation with entity, DAO, database class
- Repository with background threading
- Service layer with input validation
- All CRUD operations (create, read, update, delete/close)

### Phase 3 – UI Structure (Complete)
- Home screen with save, map, clear, and history buttons
- Save parking form with level, section, and notes fields
- History screen with RecyclerView
- Map screen stub ready for Will's Google Maps integration
- All activities registered in AndroidManifest

### Phase 4 – GPS and Maps Integration (In Progress – Will)
- Location permissions
- GPS coordinate capture
- Google Maps marker
- Distance calculation
- Directions

### Phase 5 – Full Integration (Upcoming)
- Connect Will's GPS coordinates to `SaveParkingActivity`
- Display distance on the home screen
- Test the complete save-and-find workflow end to end

### Phase 6 – Optional Features (Planned)
- Parking photo capture
- Push notifications and reminders
- UI polish and theming

---

## 3. Individual Assignments

---

### Oguzhan – Data Layer and UI

**Assignment**

My responsibility is the application's data management and user interface. This covers designing the parking data model, implementing the Room database, building the repository and service layers, and connecting everything to the screens the user interacts with.

**What I completed**

I designed the `ParkingSession` entity with all required fields, including fields for optional details such as parking level, section, and notes. I implemented the DAO with insert, update, and query operations. I built the `ParkingDatabase` singleton using double-checked locking to prevent multiple instances. I created the `ParkingRepository`, which was the most technically important part: Android does not allow database operations on the main thread, so every operation runs on a background thread using `ExecutorService` and posts the result back to the UI thread using a `Handler`. I added a `ParkingService` layer on top that validates all data before it reaches the database.

For the UI, I built the home screen showing the current saved location with all its details and buttons to navigate to other screens. I created the save parking form, the history list with a custom RecyclerView adapter, and the map screen stub that loads the saved session and passes coordinates to Will's mapping code.

**Challenges**

The main challenge was integrating two separate codebases. Will and I had both created a database and data model independently before coordinating. His structure was more complete, so we adopted his model and I rewrote the repository to add the background threading that was missing. Merging branches with unrelated histories in Git also required resolving conflicts carefully.

A second challenge was designing the interface between my data layer and Will's location component. The `SaveParkingActivity` currently uses placeholder coordinates (0.0, 0.0) until Will's GPS code is integrated. We agreed that his component will provide a `latitude`, `longitude`, `accuracyMeters`, and `capturedAt` timestamp, which I will receive and store.

**How I solved them**

I used `Executors.newSingleThreadExecutor()` to run all database operations off the main thread, which prevents the `NetworkOnMainThreadException`-style crash Android throws for blocking operations on the UI thread. For the merge conflict, I used `git pull --allow-unrelated-histories` and manually resolved the differences. For the GPS integration gap, I left a clearly commented placeholder in `SaveParkingActivity` so that connecting Will's coordinates requires changing only one section.

---

### Will – GPS, Google Maps, and Navigation

**Assignment**

My responsibility is everything related to location. This includes requesting and handling location permissions from the user, capturing the device's GPS coordinates at the time of parking, integrating Google Maps to display the saved location as a marker, calculating the distance between the user's current position and the saved location, and providing navigation back to the car.

**What I completed**

I created the `ParkingSession` data model with fields for GPS accuracy and multiple timestamps to support the full parking lifecycle. I built the initial `ParkingSessionDao` with insert and query operations, the `ParkingDatabase` singleton, and a synchronous `ParkingRepository`. I also built the `ParkingService` with comprehensive input validation, including coordinate range checks, accuracy validation, note length enforcement, and session state checks.

**Challenges**

GPS accuracy is unreliable in multi-level parking garages and indoor spaces. The GPS receiver may report a position that is many metres away from the true parking spot, or fail to get a fix at all. This is a fundamental hardware limitation that no amount of software can fully solve.

**How I solved them**

The `ParkingSession` entity includes an `accuracyMeters` field so the application can record and display how precise the saved location is. The optional level, section, and notes fields allow users to supplement the GPS coordinates with human-readable information. For the case where GPS fails entirely, the save flow will handle the error gracefully and prompt the user to try again.

---

## 4. Summary

ParkTag has a complete data layer and screen structure in place. The core save-and-find workflow is architecturally ready. The remaining work is GPS integration, Google Maps display, and end-to-end testing of the complete user journey from parking to finding the car.
