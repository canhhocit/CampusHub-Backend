# CampusHub Backend

> Backend REST API for CampusHub — Student Marketplace with Smart Locker Delivery System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

**CampusHub** is a marketplace platform designed for students to buy and sell textbooks, electronics, accessories, and school supplies. The platform integrates a **smart locker system** for contactless item delivery, allowing buyers and sellers to exchange items without meeting face-to-face.

### Core Business Flow

```
1. Seller posts a product
   ↓
2. Buyer places an order → Status: PENDING_SELLER
   ↓
3. Seller accepts → System allocates a locker compartment → Status: AWAITING_DEPOSIT
   ↓
4. Seller deposits item → System generates 6-digit PIN code → Status: READY_FOR_PICKUP
   ↓
5. System sends PIN to buyer via email
   ↓
6. Buyer enters PIN → Receives item → Status: COMPLETED
   ↓
7. Product status → SOLD
```

---

## ✨ Features

### Authentication & Authorization
- ✅ User registration with email verification
- ✅ Login with username or email
- ✅ JWT-based authentication (access + refresh tokens)
- ✅ Forgot password / Reset password
- ✅ Token reuse detection for security
- ✅ Rate limiting for sensitive endpoints

### Marketplace
- ✅ Product CRUD operations
- ✅ Category management
- ✅ Search, filter, and pagination
- ✅ Product status management (ACTIVE, HIDDEN, SOLD)
- ✅ Owner-based authorization

### Smart Locker Integration
- ✅ Multi-locker management
- ✅ Compartment allocation and tracking
- ✅ Locker/compartment status management
- ✅ Mock IoT device service (production-ready interface)

### Order Management
- ✅ Complete order lifecycle (PENDING → DEPOSIT → PICKUP → COMPLETED)
- ✅ Seller accept/reject orders
- ✅ Buyer cancel orders
- ✅ Automatic compartment allocation
- ✅ 6-digit secure pickup code (BCrypt + AES-256-GCM encryption)
- ✅ Max 5 pickup attempts
- ✅ Auto-expiration after 48 hours
- ✅ Admin order clearance

### System Features
- ✅ OpenAPI/Swagger documentation
- ✅ Async email notifications
- ✅ Scheduled token cleanup
- ✅ Optimistic + Pessimistic locking for concurrency
- ✅ Comprehensive error handling
- ✅ Request validation

---

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.4.1 |
| **Build Tool** | Maven | 3.8+ |
| **Database** | PostgreSQL | 14+ |
| **Migration** | Flyway | - |
| **Security** | Spring Security + JWT | - |
| **JWT Library** | jjwt | 0.12.3 |
| **ORM** | Spring Data JPA / Hibernate | - |
| **Validation** | Jakarta Bean Validation | - |
| **Email** | Spring Mail | - |
| **Cache** | Caffeine | - |
| **API Docs** | Springdoc OpenAPI | 2.7.0 |
| **Testing** | JUnit 5 + Mockito | - |

---

## 🏗 Architecture

**Architecture Style:** Modular Monolith  
**Pattern:** Package-by-Feature + Layered Architecture

```
com.campushub/
├── CampusHubApplication.java    # Main application
│
├── common/                      # Shared infrastructure
│   ├── bootstrap/              # Data seeders
│   ├── config/                 # Spring configurations
│   ├── email/                  # Email service
│   ├── enums/                  # Shared enums
│   ├── exception/              # Error handling
│   ├── model/                  # Base entities
│   └── security/               # JWT, encryption, auth
│
└── modules/                    # Business modules
    ├── auth/                   # Authentication
    ├── user/                   # User profiles
    ├── marketplace/            # Products & categories
    ├── locker/                 # Smart lockers
    └── order/                  # Order management
```

Each module follows:
```
module/
├── controller/    # REST endpoints
├── dto/           # Request/Response DTOs
├── entity/        # JPA Entities
├── repository/    # Data access
└── service/       # Business logic
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 14+** ([Download](https://www.postgresql.org/download/))
- **Docker** (optional, for easy PostgreSQL + Mailpit setup)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/canhhocit/CampusHub-Backend.git
   cd CampusHub-Backend
   ```

2. **Start PostgreSQL and Mailpit (using Docker)**
   ```bash
   # PostgreSQL
   docker run -d \
     --name campushub-db \
     -e POSTGRES_DB=campushub \
     -e POSTGRES_USER=root \
     -e POSTGRES_PASSWORD=rootpassword \
     -p 5432:5432 \
     postgres:14

   # Mailpit (email testing UI at http://localhost:8025)
   docker run -d \
     --name campushub-mail \
     -p 1025:1025 \
     -p 8025:8025 \
     axllent/mailpit
   ```

   Or install PostgreSQL natively and configure SMTP.

### Configuration

1. **Copy environment file**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file** (if needed)
   ```env
   # Database
   DB_URL=jdbc:postgresql://localhost:5432/campushub
   DB_USER=root
   DB_PASSWORD=rootpassword

   # Security (MUST change in production)
   JWT_SECRET=your_secure_random_jwt_secret_min_32_bytes_length
   PICKUP_ENCRYPTION_KEY=your_secure_random_pickup_aes_key_32_bytes

   # Email (Mailpit for development)
   MAIL_HOST=localhost
   MAIL_PORT=1025

   # Admin account (optional)
   ADMIN_EMAIL=admin@campushub.com
   ADMIN_PASSWORD=StrongAdminP@ssw0rd!

   # Frontend URL
   FRONTEND_URL=http://localhost:5173
   CORS_ALLOWED_ORIGINS=http://localhost:5173
   ```

3. **Note on secrets:**
   - `JWT_SECRET` must be at least 32 bytes
   - `PICKUP_ENCRYPTION_KEY` must be exactly 32 bytes
   - Never commit `.env` file to version control

### Running the Application

**Option 1: Using Maven wrapper**
```bash
./mvnw spring-boot:run
```

**Option 2: Build JAR and run**
```bash
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**Option 3: Using IDE**
- Import as Maven project in IntelliJ IDEA or Eclipse
- Run `CampusHubApplication.java`

The application will start on `http://localhost:8080`

**Flyway migrations will automatically run on startup.**

---

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### API Overview

| Endpoint Group | Base Path | Description |
|----------------|-----------|-------------|
| Authentication | `/api/v1/auth` | Register, login, logout, password reset |
| Users | `/api/v1/users` | User profile management |
| Categories | `/api/v1/categories` | Product categories (admin CRUD) |
| Products | `/api/v1/products` | Product listings and management |
| Lockers | `/api/v1/lockers` | Locker and compartment management |
| Orders | `/api/v1/orders` | Order lifecycle and pickup |

### Authentication

Most endpoints require a JWT token. Include it in the `Authorization` header:

```
Authorization: Bearer <your_jwt_token>
```

**Getting a token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "student1",
    "password": "password123"
  }'
```

---

## 💾 Database

### Flyway Migrations

Database schema is version-controlled using Flyway migrations:

```
src/main/resources/db/migration/
├── V1__init.sql                          # Baseline
├── V2__auth_schema.sql                   # Users, tokens
├── V3__marketplace_schema.sql            # Categories, products
├── V4__locker_schema.sql                 # Lockers, compartments
├── V5__order_and_pickup_schema.sql       # Orders
└── V6__order_expiration_and_security.sql # Security enhancements
```

### ERD (Entity Relationship Diagram)

```
users ──1:N──< products (owner)
users ──1:N──< orders (buyer)
users ──1:N──< orders (seller)
users ──1:N──< refresh_tokens
users ──1:N──< verification_tokens

categories ──1:N──< products

lockers ──1:N──< compartments
lockers ──1:N──< orders
compartments ──1:N──< orders

products ──1:N──< orders
```

### Accessing the Database

```bash
# Using psql
psql -h localhost -U root -d campushub

# Using Docker
docker exec -it campushub-db psql -U root -d campushub
```

---

## 🧪 Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test

```bash
./mvnw test -Dtest=AuthIntegrationTest
```

### Test Coverage

- ✅ Unit tests for security components
- ✅ Integration tests for:
  - Auth flows
  - Marketplace operations
  - Locker management
  - Order lifecycle

### Test Structure

```
src/test/java/com/campushub/
├── CampusHubApplicationTests.java
├── common/security/
│   ├── AesEncryptionServiceTest.java
│   └── JwtSecurityTest.java
└── modules/
    ├── auth/AuthIntegrationTest.java
    ├── locker/LockerIntegrationTest.java
    ├── marketplace/MarketplaceIntegrationTest.java
    └── order/OrderIntegrationTest.java
```

---

## 📁 Project Structure

```
CampusHub-Backend/
├── .env.example                # Environment variables template
├── .gitignore                  # Git ignore rules
├── pom.xml                     # Maven configuration
├── mvnw                        # Maven wrapper (Unix)
├── mvnw.cmd                    # Maven wrapper (Windows)
├── README.md                   # This file
│
└── src/
    ├── main/
    │   ├── java/com/campushub/
    │   │   ├── CampusHubApplication.java
    │   │   ├── common/
    │   │   └── modules/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │
    └── test/
        └── java/com/campushub/
```

---

## 🔒 Security

### Authentication

- **JWT access token:** 15 minutes (configurable)
- **Refresh token:** 7 days (configurable)
- **Token storage:** 
  - Access token in memory (client-side)
  - Refresh token in HttpOnly cookie
- **Token reuse detection:** All tokens revoked on reuse attempt

### Password Security

- **Hashing:** BCrypt (via Spring Security)
- **Password reset:** Token-based (24-hour expiration)
- **Email verification:** Required for new accounts

### Pickup Code Security

- **Generation:** 6-digit SecureRandom
- **Storage:**
  - BCrypt hash for verification
  - AES-256-GCM encryption for recovery
- **Attempts:** Max 5 per order
- **Expiration:** 48 hours from deposit

### API Security

- **CORS:** Configurable allowed origins
- **CSRF:** Enabled for cookie-based endpoints
- **Rate Limiting:** In-memory (Caffeine) for:
  - Registration: 5 req/15min/IP
  - Login: 10 req/15min/IP
  - Forgot password: 3 req/15min/IP

### Roles

- **STUDENT:** Default role, can buy/sell products
- **ADMIN:** Full system access

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards

- Follow Spring Boot best practices
- Use package-by-feature organization
- Write tests for new features
- Update documentation for API changes
- Follow existing code style

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **canhhocit** - *Initial work* - [canhhocit](https://github.com/canhhocit)

---

## 📞 Support

For questions, issues, or suggestions:

- **Email:** canhhuu26@gmail.com
- **Issues:** [GitHub Issues](https://github.com/canhhocit/CampusHub-Backend/issues)

---

## 🗺 Roadmap

### Completed (MVP)
- ✅ Authentication & Authorization
- ✅ Marketplace (Products & Categories)
- ✅ Smart Locker Management
- ✅ Order Lifecycle
- ✅ Pickup System
- ✅ API Documentation

### In Progress
- 🔄 Frontend (React + TypeScript)
- 🔄 Admin Dashboard
- 🔄 Audit Logging
- 🔄 In-app Notifications

### Planned
- 📋 Image upload service
- 📋 Redis integration for distributed caching
- 📋 Real IoT hardware integration
- 📋 Payment gateway integration
- 📋 Monitoring & Logging (ELK/Grafana)
- 📋 CI/CD pipeline
- 📋 Docker Compose for development
- 📋 Kubernetes deployment

---

## 📖 Additional Documentation

For more detailed information, see:

- [Architecture Document](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [API Contract](docs/api-contract.md)
- [Security Design](docs/security-design.md)

---

**Made with ❤️ for the student community**
