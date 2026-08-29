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

----------------------------------

Starting the server ->
$env:JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"  // Timezone fix, otherwise uses Asia/Calcutta by default

.\mvnw.cmd spring-boot:run