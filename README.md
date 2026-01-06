# NBLB Marketplace

A full-stack microservices e-commerce platform built with Java Spring Boot backend services and a modern responsive frontend.

## Overview

NBLB is a comprehensive marketplace application featuring role-based access control, product inventory management, shopping cart functionality, and integrated payment processing. The system is organized as a microservices architecture with separate services for authentication, product management, orders, and payments.

### Core Components

**Backend Services:**
- `auth-service` — User authentication, JWT token management, role-based access control
- `order-service` — Product catalog, order management, shopping cart operations, inventory management
- `payment-service` — Payment processing and transaction management
- `gateway` — API Gateway with CORS configuration and request routing
- `discovery` — Eureka service discovery for microservices registration

**Frontend:**
- `Front/` — Modern responsive HTML/CSS/JavaScript SPA with role-based UI components

### Technology Stack

- **Backend:** Java 17, Spring Boot 4.0+, Spring Cloud (Netflix Eureka), JWT authentication
- **Database:** MySQL 8.0 with unified NBLB_* naming convention (NBLB_USER, NBLB_ORDER, NBLB_PAYMENT)
- **Frontend:** HTML5, Bootstrap 5, jQuery 3.4.1, Font Awesome 5, WOW.js animations
- **Containerization:** Docker & Docker Compose
- **Build:** Maven with wrapper scripts (mvnw/mvnw.cmd)

## Prerequisites

- JDK 17 or newer installed and JAVA_HOME configured
- Maven (or use bundled wrappers `mvnw` / `mvnw.cmd`)

## Environment Configuration

The project requires environment variables for sensitive configuration. Follow these steps:

1. **Copy the example file to create your local `.env`:**
   ```powershell
   copy .env.example .env
   ```

2. **Edit `.env` with your actual values:**
   ```dotenv
   GEMINI_API_KEY=your-google-gemini-api-key
   MYSQL_ROOT_PASSWORD=your-db-password
   JWT_SECRET=your-jwt-secret-key
   ```

3. **Obtain Required Keys:**
   - **GEMINI_API_KEY**: Get it from [Google AI Studio](https://ai.google.dev)
   - **MYSQL_ROOT_PASSWORD**: Create a secure password for MySQL
   - **JWT_SECRET**: Generate a secure random string (min 32 characters)

4. **Load environment variables before running services:**
   ```powershell
   # Windows PowerShell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^\s*([^=]+)\s*=\s*(.*)$') {
           [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
       }
   }
   ```

   Or manually set in your terminal:
   ```powershell
   $env:GEMINI_API_KEY='your-key'
   $env:JWT_SECRET='your-secret'
   ```

**Note:** The `.env` file is listed in `.gitignore` and should never be committed to version control.

## Run (examples)

You can run each service individually. Replace `<module>` with the module folder name.

```powershell
# Run order-service using the module wrapper (Windows)
cd .\order-service
.\mvnw.cmd spring-boot:run
```

If the project uses a discovery server, start `discovery` first, then other services.

### Start all backend services (Windows)

If you prefer a stable start order (and to avoid VS Code interrupting processes), you can start everything via the root script:

```powershell
cd .
# Build the JARs first
powershell -ExecutionPolicy Bypass -File .\build-backend.ps1 -SkipTests

powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
```

This starts (in order): `discovery` (8761) → `gateway` (8222) → `auth-service` (8090) → `order-service` (8091) → `payment` (8092).

## Frontend Features

The NBLB frontend is a responsive, single-page application with:

- **User Authentication** — Login/Register with JWT token management
- **Role-Based Navigation** — Different UI for Admin, Seller, and Customer roles
- **Product Management** — Browse products with expiry date indicators and stock status
- **Shopping Cart** — Full cart management with product images, quantity adjustment, and real-time updates
- **Order Management** — Track orders and payment status
- **Dashboard Pages** — Role-specific dashboards for administrators and sellers
- **Unified Navbar** — Dynamic navigation component with role-based visibility and cart badge
- **Toast Notifications** — Professional in-app notifications replacing browser alerts
- **Responsive Design** — Mobile-friendly layout with Bootstrap 5

### Running Frontend Locally

#### Option A: PowerShell Server

```powershell
cd .\Front
powershell -ExecutionPolicy Bypass -File .\serve.ps1 -Port 5500
```

Then visit: http://localhost:5500/

#### Option B: Python Server

```powershell
cd .\Front
py -m http.server 5500
```

Then visit: http://localhost:5500/

## Frontend (static HTML/JS)

The `Front/` folder is a static frontend (plain HTML/CSS/JS). To test it reliably, serve it via a local HTTP server (avoid opening HTML files directly with `file://`).

## Database Configuration

The project uses a unified database naming convention: **NBLB_\***

- `NBLB_USER` — User accounts and authentication data (auth-service)
- `NBLB_ORDER` — Products, orders, and inventory (order-service)
- `NBLB_PAYMENT` — Payment transactions (payment-service)

Each service maintains its own database for data isolation per microservices architecture principles.

### Database Initialization

The `docker/mysql/init.sql` script automatically creates all required databases and initial schemas when using Docker Compose.

For local development, ensure your MySQL instance creates the NBLB_* databases and configure each service's `application.properties` with the correct connection string.

## Docker (recommended for Ops)

This repo includes a `docker-compose.yml` that orchestrates the complete stack:

- **MySQL 8.0** (port `3307`) — Database server with NBLB_* databases
- **Eureka Discovery** (port `8761`) — Service registry and discovery
- **API Gateway** (port `8222`) — Central entry point for all API requests
- **Auth Service** (port `8090`) — User authentication and JWT management
- **Order Service** (port `8091`) — Product and order management
- **Payment Service** (port `8092`) — Payment processing
- **Frontend (Nginx)** (port `5500`) — Static frontend served via Nginx

### Container Networking

All services communicate through Docker's internal network. The `init.sql` script runs on MySQL startup to initialize all databases with the unified NBLB_* naming scheme.

### Prerequisites (Windows)

- Install Docker Desktop
- Ensure Docker Desktop is **running**
- Ensure you are using **Linux containers** (Docker Desktop setting: WSL2 engine / “Switch to Linux containers”).

If you see an error like `dockerDesktopLinuxEngine: The system cannot find the file specified`, Docker Desktop is not running or is in Windows-containers mode.

### Start

From the repo root:

```powershell
docker compose up --build -d
```

Open:

- Front: http://127.0.0.1:5500/
- Gateway: http://127.0.0.1:8222/
- Eureka: http://127.0.0.1:8761/

MySQL:

- Host: `127.0.0.1`
- Port: `3307` (or set `MYSQL_HOST_PORT`)

### Environment variables (optional)

- `MYSQL_ROOT_PASSWORD` (default: `rootpass`)
- `MYSQL_HOST_PORT` (default: `3307`)
- `JWT_SECRET` (default provided for dev)
- `STRIPE_SECRET_KEY` (optional)

Example:

```powershell
$env:MYSQL_ROOT_PASSWORD='rootpass'
$env:JWT_SECRET='change-me'
docker compose up --build -d
```

### Stop / cleanup

```powershell
docker compose down
```

Remove containers + volumes (drops MySQL data):

```powershell
docker compose down -v
```

### Option B: Python (if installed)

```powershell
cd .\Front
py -m http.server 5500
```

Then open:

- http://localhost:5500/index.html

## Key Features & Implementation

### Authentication & Authorization
- JWT token-based authentication with role support (ADMIN, SELLER, CLIENT)
- Secure password handling and token refresh mechanisms
- Role-specific dashboard and navigation controls

### Product Management
- Product catalog with search and filtering
- Inventory tracking with real-time stock status
- Expiry date management with color-coded freshness indicators (Green: 7+ days, Orange: 4-7 days, Yellow: 1-3 days, Red: Expired)
- Product image management with optimized display

### Shopping Cart
- Real-time cart updates with persistence in localStorage
- Dynamic cart badge displaying item count
- Product thumbnails in cart for visual confirmation
- Quantity management and item removal
- Automatic stock reduction after successful payment

### Order Management
- Order creation from cart checkout
- Order tracking and history
- Integration with payment service
- Stock deduction on payment completion

### Payment Processing
- Multiple payment methods support (Card, Cash on Delivery, Wallet)
- Post-payment stock reduction mechanism
- Payment status tracking
- Order confirmation and redirect to orders page

### User Interface
- Role-based navigation and dashboards
- Unified responsive navbar component
- Professional toast notifications (replacing browser alerts)
- Mobile-friendly Bootstrap 5 design
- Admin and Seller dashboard interfaces

## API Endpoints

The Gateway routes all requests. Key endpoints include:

**Auth Service:**
- `POST /auth-service/api/auth/register` — User registration
- `POST /auth-service/api/auth/login` — User login
- `POST /auth-service/api/auth/refresh` — Token refresh

**Order Service:**
- `GET /order-service/products` — List all products
- `GET /order-service/products/{id}` — Get product details
- `POST /order-service/api/checkout` — Create order from cart
- `POST /order-service/api/reduce-stock` — Reduce inventory after payment
- `GET /order-service/api/orders` — Get user orders

**Payment Service:**
- `POST /payment/api/process` — Process payment

## Development Workflow

1. **Start Backend Services**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
   ```
   Or use Docker Compose for complete stack

2. **Start Frontend**
   ```powershell
   cd .\Front
   powershell -ExecutionPolicy Bypass -File .\serve.ps1 -Port 5500
   ```

3. **Access Application**
   - Frontend: http://localhost:5500/
   - Gateway: http://localhost:8222/
   - Eureka: http://localhost:8761/

## Contact

For additional documentation, architecture diagrams, or specific service details, please refer to individual service README files or open an issue.
