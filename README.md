E-Commerce Backend — Spring Boot REST API

Overview This project is a backend system for an e-commerce application
built using Spring Boot. It handles user authentication, product
management, cart operations, order processing, and payment integration.

The goal was to design a clean, production-style backend that follows
good architecture practices and mimics real-world e-commerce systems.

Tech Stack - Java 17 - Spring Boot 3.2 - Spring Security + JWT - Spring
Data JPA (Hibernate) - H2 Database (development) - MySQL (production) -
Stripe API - Maven

Architecture The project follows a layered architecture:

Controller → Service → Repository → Database

-   Controllers handle HTTP requests
-   Services contain business logic
-   Repositories interact with the database

Authentication & Authorization Users can register and login to receive a
JWT token. This token must be included in requests: Authorization:
Bearer

Roles: - ROLE_USER - ROLE_ADMIN

Core Features

User: - Register & login - View profile - Manage cart - Place orders

Product & Category: - View products - Search and filter - Admin can
create/update/delete

Cart: - Add, update, remove items - View total

Orders: - Create order from cart - Track order status

Order lifecycle: PENDING → CONFIRMED → SHIPPED → DELIVERED → CANCELLED

Payments: - Stripe PaymentIntent used - Webhook updates order status

How It Works 1. User logs in 2. Browses products 3. Adds items to cart
4. Places order 5. System checks stock and creates order 6. Stripe
handles payment 7. Webhook updates order

How to Run

git clone cd Assignment mvn clean install mvn spring-boot:run

Access: Swagger: http://localhost:8080/swagger-ui.html H2 Console:
http://localhost:8080/h2-console

Testing Use Postman or Swagger: 1. Register 2. Login 3. Use token 4.
Test APIs

Database Development: H2 (in-memory) Production: MySQL

Project Structure - controller - service - repository - entity - dto -
security - exception

Important Notes - Runs with H2 by default - Stripe optional for
testing - Change JWT secret in production - Soft delete for products

What I Learned - REST API design - JWT authentication - Backend
architecture - Payment integration

Conclusion This project demonstrates a complete backend system for an
e-commerce platform with clean architecture and real-world
functionality.

Future Improvements - Order cancellation with stock restore - Redis
caching - Email notifications - React frontend - Docker support

## Database Access

### Development (H2 Console)

| Setting | Value |
|---|---|
| URL | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:mem:ecommercedb` |
| Username | `sa` |
| Password | *(leave blank)* |

The database is recreated fresh on every restart (`ddl-auto=create-drop`). Seed data is auto-populated by `DataInitializer`.



### 4. Clone and build

```bash
git clone <your-repo-url>
cd Assignment

# Download dependencies and compile (skip tests for speed)
mvn clean package -DskipTests

# Verify it compiled cleanly
echo "Build exit code: $?"
```

---

### 5. Set environment variables (optional for dev)

```bash
# macOS / Linux — paste in your terminal session
export STRIPE_SECRET_KEY=sk_test_51TLUJvRo2oOLgI2ww4IISXIeccrBJA5bitY4Shg45YW48R7zcOQ6CgPgrSvTkOas0GUTEoOjjNsXasyEbAb2v5It00IEg3L7Gs
export STRIPE_WEBHOOK_SECRET=whsec_41eac6e0e209a8b15ef2015258c736c16034a2f66e35889e024b33f363376a53

# Windows PowerShell
$env:STRIPE_SECRET_KEY="sk_test_your_key_here"
$env:STRIPE_WEBHOOK_SECRET="whsec_your_secret_here"
```

Or create a `.env` file at the project root (never commit this):

```
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

---

### 6. MySQL setup (production / optional)

If you want to run against a real database instead of H2:

```sql
-- Connect to MySQL as root, then run:
CREATE DATABASE ecommercedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ecommerce'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON ecommercedb.* TO 'ecommerce'@'localhost';
FLUSH PRIVILEGES;
```

Then set:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://localhost:3306/ecommercedb
export DB_USER=ecommerce
export DB_PASSWORD=yourpassword
```

Hibernate will create all tables automatically on first start (`ddl-auto=create`). After the first successful start, change to `ddl-auto=validate` in `application-prod.properties` to protect against accidental schema changes.

---

## Configuration

### Environment Variables

| Variable | Description | Required |
|---|---|---|
| `STRIPE_SECRET_KEY` | Stripe secret key (`sk_test_...`) | Yes (for payments) |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret (`whsec_...`) | Yes (for webhooks) |
| `DB_URL` | MySQL JDBC URL | Prod only |
| `DB_USER` | MySQL username | Prod only |
| `DB_PASSWORD` | MySQL password | Prod only |

### Key application.properties values

```properties
# JWT — change the secret in production (must be Base64-encoded, ≥256 bits)
app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
app.jwt.expiration-ms=86400000   # 24 hours

# Stripe
stripe.secret-key=${STRIPE_SECRET_KEY:sk_test_placeholder}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:whsec_placeholder}
```

---

## Running the Application

### Development (H2 in-memory database)

```bash
# Option 1: Maven
mvn spring-boot:run

# Option 2: JAR
mvn clean package -DskipTests
java -jar target/Assignment-1.0-SNAPSHOT.jar

# With Stripe keys
STRIPE_SECRET_KEY=sk_test_xxx mvn spring-boot:run
```

The app starts on **http://localhost:8080**

### Production (MySQL)

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://localhost:3306/ecommercedb
export DB_USER=root
export DB_PASSWORD=yourpassword
export STRIPE_SECRET_KEY=sk_live_xxx
export STRIPE_WEBHOOK_SECRET=whsec_xxx
java -jar target/Assignment-1.0-SNAPSHOT.jar
```

---

## API Reference

### Base URLs

| Environment | Base URL |
|---|---|
| Development | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

### Authentication Endpoints — Public

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT |

### User Endpoints — `ROLE_USER`

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/users/me` | Get current user profile |

### Product Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/products` | Public | List all products (paginated) |
| `GET` | `/api/products/{id}` | Public | Get product by ID |
| `GET` | `/api/products/search?q={query}` | Public | Search by name |
| `GET` | `/api/products/category/{categoryId}` | Public | Filter by category |
| `POST` | `/api/products` | `ROLE_ADMIN` | Create a product |
| `PUT` | `/api/products/{id}` | `ROLE_ADMIN` | Update a product |
| `DELETE` | `/api/products/{id}` | `ROLE_ADMIN` | Soft-delete a product |

### Category Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/categories` | Public | List all categories |
| `GET` | `/api/categories/{id}` | Public | Get category by ID |
| `POST` | `/api/categories` | `ROLE_ADMIN` | Create a category |
| `PUT` | `/api/categories/{id}` | `ROLE_ADMIN` | Update a category |
| `DELETE` | `/api/categories/{id}` | `ROLE_ADMIN` | Delete a category |

### Cart Endpoints — `ROLE_USER`

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/cart` | View current user's cart |
| `POST` | `/api/cart/items` | Add a product to cart |
| `PUT` | `/api/cart/items/{cartItemId}` | Update item quantity |
| `DELETE` | `/api/cart/items/{cartItemId}` | Remove item from cart |
| `DELETE` | `/api/cart` | Clear entire cart |

### Order Endpoints — `ROLE_USER`

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/orders` | Place an order from current cart |
| `GET` | `/api/orders` | List my orders (paginated) |
| `GET` | `/api/orders/{id}` | Get order details |

### Admin Endpoints — `ROLE_ADMIN`

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/admin/orders` | List ALL orders (paginated) |
| `PUT` | `/api/admin/orders/{id}/status` | Update order status |

### Payment Endpoints — Public (Stripe-Signature verified)

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/payments/webhook` | Stripe webhook receiver |

---


## Testing

### Run all tests

```bash
mvn test
```

### Run a specific test class

```bash
mvn test -Dtest=AuthServiceImplTest
mvn test -Dtest=CartControllerTest
mvn test -Dtest=JwtTokenProviderTest
```

### Run tests by category

```bash
# Service layer only
mvn test -Dtest="*ServiceImpl*"

# Controller layer only
mvn test -Dtest="*ControllerTest"

# Repository layer only
mvn test -Dtest="*RepositoryTest"
