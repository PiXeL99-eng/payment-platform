Browser
   │
   │ GET /hello
   ▼
Tomcat :8080
   │
   ▼
Spring Boot
   │
   ▼
PaymentController
   │
   ▼
"Hello World!"


-----------------

New Flow ->

HTTP Request
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
     │
     ▼
Database

-------------

Your Java code
      ↓
Spring Data JPA
      ↓
Hibernate
      ↓
JDBC
      ↓
PostgreSQL


--------------
### The Architecture: How It Fits Together

To understand JPA in Spring Boot, it helps to see the different layers involved:

| Layer                    | What it does                                                     | Example in Spring Boot |
| :---                     | :---                                                             | :---                   |
| **Spring Data JPA** | The developer interface. Provides automated repository methods.       | `JpaRepository` interface |
| **JPA (Specification)** | The standard rules and guidelines for Object-Relational Mapping (ORM). | `@Entity`, `@Id` annotations |
| **Hibernate (Provider)** | The actual engine. It does the heavy lifting of converting Java to SQL. | Automatically included by Spring Boot |
| **JDBC**                 | The low-level driver that talks directly to the database. | PostgreSQL / MySQL / H2 Driver |


------------

Spring Boot
    ↓
HikariCP (connection pool)
    ↓
PostgreSQL JDBC Driver
    ↓
PostgreSQL Docker Container

---------------------------------
So:

Database schema ≠ API contract

That's an important backend engineering principle.

---------------------------------

This is a very important backend principle:

Application-level checks improve behavior, but database constraints protect data integrity.

---------------------------------

POST -> You use POST when the user clicks the "Create New Playlist" button. The server will create a brand-new, empty playlist in the database and automatically give it a unique ID.

PUT -> You use PUT when the user wants to completely overwrite the existing playlist. Imagine they use a "Reset Playlist Layout" feature to change the title and add a whole new list of songs, wiping out whatever was there before.

PATCH -> You use PATCH when the user only wants to change one specific thing, like clicking the toggle switch to make the playlist Private instead of Public, without touching the title or the songs.

----------------------------------

Starting the server ->

$env:JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"  // Timezone fix, otherwise uses Asia/Calcutta by default

.\mvnw.cmd spring-boot:run