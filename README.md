# Transfers API

REST API that acts as a proxy between a mobile/web client and a
SOAP backend (Banelco Transferencias), exposing a clean REST
interface for the recipients agenda.

## Prerequisites

- Java 21
- Maven 3.9+

## Running the application

```bash
mvn spring-boot:run
```

## Code Style

This project uses Spotless with Google Java Format.

Apply formatting before committing:
```bash
mvn spotless:apply
```

Verify formatting:
```bash
mvn spotless:check
```

## Running tests and full verification

```bash
mvn verify
```