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

### Option B: Python (if installed)

```powershell
cd .\Front
py -m http.server 5500
```

Then open:

- http://localhost:5500/index.html

## Contact

If you need more detailed README content (architecture diagram, env variables, CI steps, or Docker instructions), tell me which services you want documented and I will expand this file.
