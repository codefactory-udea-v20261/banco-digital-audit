# Banco Digital - Audit Service

The Audit Service provides an immutable audit trail of all domain events in the Banco Digital platform. It consumes events from Kafka and persists them for regulatory compliance and forensic analysis.

## Architecture

- **Event-Driven** - Consumes events from Kafka topic `banco-digital-events`
- **Resilience4j** - Circuit breaker for database failures with Kafka fallback queue
- **JPA/PostgreSQL** - Persistent audit event storage with JSONB payload support
- **JWT Authentication** - Validates tokens for query endpoints
- **Flyway** - Database migrations

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 16+
- Kafka (for event consumption)
- Docker (optional)

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
# Set environment variables
export APP_PROFILE=local
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=banco_digital_audit
export DB_USERNAME=audit_user
export DB_PASSWORD=your_password
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export JWT_SECRET=your_base64_encoded_256bit_secret_here

mvn spring-boot:run
```

### Docker

```bash
docker build -t banco-digital-audit .
docker run -p 8082:8082 \
  -e APP_PROFILE=local \
  -e DB_HOST=localhost \
  -e DB_NAME=banco_digital_audit \
  -e DB_USERNAME=audit_user \
  -e DB_PASSWORD=your_password \
  -e KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  -e JWT_SECRET=your_secret \
  banco-digital-audit
```

## API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/audit/{eventId}` | Get single audit event | Yes |
| GET | `/api/v1/audit` | Query audit events (paginated) | Yes |

### Query Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `eventType` | String | Filter by event type |
| `aggregateId` | String | Filter by aggregate ID |
| `userId` | String | Filter by user ID |
| `startDate` | LocalDateTime | Start of date range |
| `endDate` | LocalDateTime | End of date range |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20) |

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| `banco-digital-events` | Main event bus (consumed) |
| `banco-digital-events-dlq` | Dead letter queue for failed events |
| `audit-events-pending` | Fallback queue when DB circuit breaker is open |

## Database Schema

### audit_event

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `event_id` | VARCHAR(255) | Unique event identifier |
| `event_type` | VARCHAR(100) | Type of event |
| `aggregate_id` | VARCHAR(255) | Related aggregate ID |
| `correlation_id` | VARCHAR(255) | Correlation ID for tracing |
| `user_id` | VARCHAR(255) | User who triggered the event |
| `source_service` | VARCHAR(100) | Originating service |
| `occurred_at` | TIMESTAMP | When the event occurred |
| `payload` | JSONB | Full event payload |
| `created_at` | TIMESTAMP | When the record was created |

## Swagger UI

Available at: `http://localhost:8082/swagger-ui.html`

## Testing

```bash
mvn test
```
