# NBLB (No Bite Left Behind)

Multi-module Java (Spring Boot) microservices project.

## Overview

This repository contains multiple Spring Boot services organized as modules:

- `auth-service` — authentication service
- `discovery` — service discovery (Eureka/Consul style)
- `gateway` — API gateway
- `order-service` — order management
- `payment` — payment processing

Each module is a Maven module with its own `pom.xml`.

## Prerequisites

- JDK 17 or newer installed and JAVA_HOME configured
- Maven (or use bundled wrappers `mvnw` / `mvnw.cmd`)

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

## Frontend (static HTML/JS)

The `Front/` folder is a static frontend (plain HTML/CSS/JS). To test it reliably, serve it via a local HTTP server (avoid opening HTML files directly with `file://`).

### Option A: PowerShell (no dependencies)

```powershell
cd .\Front
powershell -ExecutionPolicy Bypass -File .\serve.ps1 -Port 5500
```

Then open:

- http://localhost:5500/index.html

## Docker (recommended for Ops)

This repo includes a `docker-compose.yml` that starts:

- MySQL (exposed on host port `3307` by default)
- `discovery` (`8761`)
- `gateway` (`8222`)
- `auth-service` (`8090`)
- `order-service` (`8091`)
- `payment` (`8092`)
- `front` (Nginx, exposed on `5500`)

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

## Contact

If you need more detailed README content (architecture diagram, env variables, CI steps, or Docker instructions), tell me which services you want documented and I will expand this file.
