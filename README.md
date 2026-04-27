# Transfers API

REST API that acts as an **Adapter** between a mobile/web client and a legacy SOAP backend
(Banelco Transferencias). It translates REST requests into SOAP calls and maps the XML
response back to JSON, keeping the client completely decoupled from the SOAP backend.

---

## Prerequisites

- Java 21
- Maven 3.9+
- [Mockoon](https://mockoon.com/) (to run the SOAP mock backend)

---

## Running the application

### 1. Start the mock backend

See [Testing with Mockoon](#testing-with-mockoon) for setup instructions.

### 2. Run the application

```bash
mvn spring-boot:run
```

The `local` Spring profile is activated automatically by the Maven plugin configuration
in `pom.xml` — no extra flags needed. The API will be available at `http://localhost:8080`.

### 3. Test the endpoint

```
GET /v1/transfers/customers-document/{customer-document-number}/recipients?customer-document-type=01
```

Example:

```bash
curl -H "Authorization: Bearer test-token" \
  "http://localhost:8080/v1/transfers/customers-document/32345379/recipients?customer-document-type=01"
```

---

## Testing with Mockoon

The file `docs/Transfers-Mock-get-recipients.json` contains a pre-configured Mockoon environment that simulates the SOAP backend.

### Setup

1. Install [Mockoon Desktop](https://mockoon.com/download/) or the CLI:
   ```bash
   npm install -g @mockoon/cli
   ```

2. **Fix the Content-Type header before starting (required):**
   Open the file and change the response header from `application/xml` to `text/xml;`.

   > The provided mock uses `Content-Type: application/xml` but SOAP 1.1 requires `text/xml`
   > (WS-I Basic Profile 1.1, section 3.1.1). Spring WS will reject the response otherwise.
   > This is a problem in the mock file, not in the application.

3. Start the mock:

   **Desktop:** File → Open environment → select `docs/Transfers-Mock-get-recipients.json` → click ▶

   **CLI:**
   ```bash
   mockoon-cli start --data docs/Transfers-Mock-get-recipients.json --port 3003
   ```

### End-to-end call

With both the mock and the application running:

```bash
curl -s -H "Authorization: Bearer test-token" \
  "http://localhost:8080/v1/transfers/customers-document/32345379/recipients?customer-document-type=01" \
  | jq .
```

Expected response shape:

```json
{
  "recipient": [
    {
      "cuit": "20123456789",
      "description": "ROSA LOPEZ",
      "account": {
        "cbu": "2850672840027388702207",
        "code": 4,
        "description": "Es Otra Cuenta No Propia",
        "current": true,
        "own": false
      }
    }
  ]
}
```

---

## Running tests and full verification

```bash
mvn verify
```

---

## Code Style

This project uses Spotless with Google Java Format.

```bash
mvn spotless:apply   # apply formatting
mvn spotless:check   # verify (used in CI)
```

---

## Technical Decisions

### 1. Project structure — layered architecture with clear boundaries

```
controller/     ← HTTP interface only, no business logic
service/        ← orchestration: calls client, maps response, throws domain exceptions
client/         ← SOAP infrastructure (WebServiceGatewaySupport)
mapper/         ← SOAP → REST transformation (RecipientMapper)
model/
  soap/         ← generated from WSDL, never modified manually
  api/          ← REST response contracts
exception/      ← domain exceptions + GlobalExceptionHandler
config/         ← Spring beans, SoapProperties
```

The controller has a single responsibility: receive an HTTP request and return a response.
It knows nothing about SOAP. If the backend changes from SOAP to REST tomorrow, only
`client/` and `service/` change — the controller is unaffected.

`model/soap` and `model/api` are kept separate intentionally: the internal SOAP contract
must never leak into the REST response. The mapper is the only place that crosses that
boundary.

### 2. SOAP client — generated classes from WSDL via jaxws-maven-plugin

SOAP classes are generated during `generate-sources` from `PRISMA_TransferenciasService.wsdl`
using `jaxws-maven-plugin`. They are emitted to `target/generated-sources` and never
committed to the repository.

```
"If the WSDL changes, I regenerate the classes and the compiler tells me
 exactly where I broke something. It's type-safe and avoids manual XML parsing."
```

The plugin was chosen over `jaxb2-maven-plugin` because it is designed specifically for
JAX-WS and WSDL — it generates both the DTOs and the service port descriptor.

### 3. ObjectFactory — solving the missing @XmlRootElement problem

JAXB requires `@XmlRootElement` to know how to name the XML root element when marshalling.
The classes generated from the WSDL do not carry this annotation (they use `@XmlType`
instead, since they are declared as complex types, not document roots).

Adding `@XmlRootElement` manually is not viable: the classes live in `target/` and would
be overwritten on every regeneration.

The correct solution is to use the `ObjectFactory` that wsimport generates alongside the
DTOs. It wraps the object in a `JAXBElement` with the correct QName:

```java
ObjectFactory factory = new ObjectFactory();
JAXBElement<GetAgendaCBU> request = factory.createGetAgendaCBU(payload);
```

On the response side, JAXB returns a `JAXBElement<GetAgendaCBUResponse>`. The type is
verified with `instanceof` and unwrapped with `.getValue()`.

### 4. RecipientMapper — Mapper pattern with SRP

A dedicated `RecipientMapper` class handles the SOAP → REST transformation.
Keeping this logic out of the service means:

- The service remains readable: it orchestrates, it doesn't transform.
- The mapper can be unit-tested in isolation.
- If the SOAP contract changes, there is exactly one place to update.

Key mapping decisions inside the mapper:

**`normalizeCuit`:** SOAP sends `cuitCuil` as a fixed-width padded string (e.g., `"1111       "`).
The value is trimmed and mapped to `null` if blank, rather than exposing whitespace-only
strings in the REST response.

**`AccountType.fromCode`:** The SOAP `propiedadDTO.codigo` arrives as a `String` (inherited
from `BaseDTO`). It is parsed to `int` and matched against the enum using an explicit field
`code`, never `ordinal()`. Using `ordinal()` would break silently if constants were ever
reordered. If an unknown code arrives, the method throws `IllegalArgumentException`
(fail-fast — it is a backend contract violation, not a recoverable state).

**Null-safe `propiedadDTO`:** The WSDL declares `propiedadDTO` as `minOccurs="0"`, meaning
it can arrive as `null`. The mapper handles this explicitly: if absent, `cbu` is preserved
and all account fields are returned as `null`.

### 5. WebServiceGatewaySupport — SOAP client base class

`TransfersSoapClient` extends `WebServiceGatewaySupport` because it provides a pre-configured
`WebServiceTemplate` (marshaller, HTTP transport, fault handling) without manual boilerplate.
`marshalSendAndReceive` serializes the Java object to SOAP XML, executes the HTTP POST,
and deserializes the response — all in a single call.

### 6. Configuration — @ConfigurationProperties with Record

SOAP connection properties are grouped in `SoapProperties`, a Java `record` annotated with
`@ConfigurationProperties(prefix = "soap.transferencias")`.

A `record` is used because configuration is immutable by nature: once injected at startup,
it should not change. `@ConfigurationProperties` is preferred over `@Value` when multiple
related properties belong to the same concept — it makes the configuration explicit,
type-safe, and easy to validate.

In production, credentials (`password`, `canal`, `terminal`) would be externalized as
environment variables or injected from a secrets manager (AWS Secrets Manager, HashiCorp
Vault). They are in `application.yml` here for simplicity.

### 7. Exception Translation + GlobalExceptionHandler

**Exception Translation in the service layer**

The `SoapClient` is infrastructure. The `Service` should not leak Spring WS internals
(`WebServiceIOException`, `WebServiceFaultException`) into the domain layer. The service
catches those and translates them to domain exceptions:

```java
try {
    soapResponse = transfersSoapClient.getRecipientsCBU(...);
} catch (WebServiceIOException ex) {
    throw new SoapTimeoutException("SOAP backend unreachable or timed out", ex);
} catch (WebServiceFaultException ex) {
    throw new SoapGatewayException("SOAP backend returned a fault", ex);
}

try {
    response = recipientMapper.toRecipientsGetResponse(soapResponse);
} catch (IllegalArgumentException ex) {
    throw new SoapGatewayException("SOAP backend returned unrecognized data: " + ex.getMessage(), ex);
}
```

This is the **Exception Translation** pattern: infrastructure and mapping exceptions are
translated to domain exceptions at the boundary, so each layer works with its own
vocabulary. `GlobalExceptionHandler` then maps domain exceptions to HTTP status codes —
it never needs to know about Spring WS internals.

`IllegalArgumentException` is caught in the service — not in `GlobalExceptionHandler` —
because it is too generic. Catching it globally would intercept unrelated parts of the
system. In this specific context it means the SOAP backend sent an unrecognized account
code, which is semantically a 502 (invalid upstream response), not a 500.

**GlobalExceptionHandler — not extending ResponseEntityExceptionHandler**

A `@RestControllerAdvice` handles all exceptions centrally. The decision not to extend
`ResponseEntityExceptionHandler` was deliberate:

- Extending it and declaring `@ExceptionHandler` for the same exception types causes
  `Ambiguous @ExceptionHandler` errors at startup.
- In Spring Boot 3.x, the base class returns `ProblemDetail` (RFC 9457) by default.
  Overriding every method to return our own `ErrorResponse` would add more code, not less.
- For this API surface, explicit handlers are simpler and fully typed (`ResponseEntity<ErrorResponse>`).

HTTP status mapping:

| Exception                         | HTTP | Reason                                           |
|-----------------------------------|------|--------------------------------------------------|
| `RecipientNotFoundException`      | 404  | No agenda found for the given document           |
| `NoResourceFoundException`        | 404  | No handler matched the URL (Spring Boot 3.2+)    |
| `MethodArgumentNotValidException` | 400  | Bean validation failure on request body          |
| `SoapTimeoutException`            | 504  | Wraps `WebServiceIOException` — no response      |
| `SoapGatewayException`            | 502  | Wraps `WebServiceFaultException` — SOAP fault    |
| `Exception`                       | 500  | Unexpected failure                               |

`SoapTimeoutException` → 504 (not 502): the upstream never responded (typically a
`SocketTimeoutException`). 504 = "gateway did not receive a response in time".
502 = "upstream responded with something invalid" — that is `SoapGatewayException`.

Error responses follow the OpenAPI contract schema exactly (`Code`, `Id`, `Message`,
`Errors[]`). PascalCase field names are enforced via `@JsonProperty` — without it, Jackson
would serialize to camelCase and break the contract.

Logging levels are intentional:
- `WARN` for client errors (4xx) — does not page on-call.
- `ERROR` for server errors (5xx) — unexpected failures require attention.
- The `Exception` catch-all logs `ex` as the last SLF4J argument, which triggers automatic
  stack trace output — the idiomatic pattern, not `ex.printStackTrace()`.
- Internal exception messages are never exposed in 500 responses to avoid leaking
  implementation details.

### 8. Security — API Gateway model

In a microservice architecture there are two models for JWT validation:

- **Gateway model:** the API Gateway validates the token and forwards trusted requests
  to downstream services. The MS itself does not implement Spring Security.
- **MS model:** each service validates the JWT independently using a `JwtDecoder`
  and a `SecurityFilterChain`.

This service follows the **Gateway model**: token validation is assumed to be handled
upstream. The `Authorization` header is passed through but not validated here.

This is a deliberate decision, not an omission. Adding Spring Security without a real
identity provider or JWKS endpoint would introduce configuration with no real security
guarantee. In a production setup, the gateway enforces authentication before the request
reaches this service.

### 9. @JsonProperty for REST model fields

REST response models use `@JsonProperty` to decouple the Java field name (camelCase,
following Java conventions) from the JSON field name (as required by the OpenAPI contract).
This keeps the code idiomatic while honoring the external contract.
