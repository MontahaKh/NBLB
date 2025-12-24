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
# Run order-service using the wrapper (Windows)
.\mvnw.cmd -pl order-service -am spring-boot:run

# Or use Maven directly
mvn -pl order-service -am spring-boot:run
```

If the project uses a discovery server, start `discovery` first, then other services.

## Contact

If you need more detailed README content (architecture diagram, env variables, CI steps, or Docker instructions), tell me which services you want documented and I will expand this file.
