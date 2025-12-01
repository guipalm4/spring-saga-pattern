# Saga Pattern with Spring Boot

A distributed transaction management implementation using the **Saga Pattern** with Spring Boot, AWS SQS, and LocalStack.

## 📋 Overview

This project demonstrates the **Orchestrated Saga Pattern** for managing distributed transactions in a microservices architecture. It simulates an e-commerce order flow with the following steps:

1. **Order Creation** - Create a new order
2. **Payment Processing** - Process payment for the order
3. **Inventory Reservation** - Reserve stock for the products
4. **Shipping Arrangement** - Arrange delivery for the order

If any step fails, the saga orchestrator automatically triggers **compensating transactions** to rollback previous steps.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Saga Orchestrator                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Order   │→ │ Payment  │→ │Inventory │→ │ Shipping │        │
│  │ Created  │  │Processed │  │ Reserved │  │ Arranged │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│       ↑              ↑              ↑              ↑            │
│       └──────────────┴──────────────┴──────────────┘            │
│                    Compensation Flow                            │
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Tech Stack

- **Java 24** with Eclipse Temurin
- **Spring Boot 3.5.5**
- **Spring Data JPA** with H2 Database
- **Spring Cloud AWS** for SQS, S3, DynamoDB
- **Spring State Machine** for saga state management
- **LocalStack** for local AWS simulation
- **Micrometer** with Prometheus for metrics
- **Lombok** for boilerplate reduction
- **Docker** & **Docker Compose**

## 📁 Project Structure

```
src/main/java/com/guipalm4/sagapatternspring/
├── api/
│   ├── controller/     # REST controllers
│   ├── request/        # API request DTOs
│   └── response/       # API response DTOs
├── config/             # Configuration classes
├── domain/             # JPA entities and enums
├── messaging/
│   ├── events/         # Event DTOs
│   ├── request/        # Service request DTOs
│   └── response/       # Service response DTOs
├── repository/         # JPA repositories
└── service/            # Business logic and saga orchestration
```

## 🚀 Getting Started

### Prerequisites

- **Java 24** or higher
- **Docker** and **Docker Compose**
- **Maven** (or use the included Maven Wrapper)

### Running with Docker Compose

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd saga-pattern-spring
   ```

2. **Start all services**
   ```bash
   docker-compose up -d
   ```

   This will start:
   - **LocalStack** on port `4566` (SQS, S3, DynamoDB)
   - **Saga Application** on port `8080`

3. **Check if services are running**
   ```bash
   # Check LocalStack health
   curl http://localhost:4566/health

   # Check application health
   curl http://localhost:8080/actuator/health
   ```

### Running Locally (Development)

1. **Start LocalStack only**
   ```bash
   docker-compose up -d localstack
   ```

2. **Run the application**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

### Building the Project

```bash
# Compile and package
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

## 📡 API Endpoints

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/orders` | Create a new order |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `GET` | `/api/orders/customer/{customerId}` | Get orders by customer |
| `GET` | `/api/orders/status/{status}` | Get orders by status |
| `POST` | `/api/orders/{id}/cancel` | Cancel an order |

### Sagas

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/sagas` | List all sagas |
| `GET` | `/api/sagas/{sagaId}/status` | Get saga status |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/metrics/saga` | Get saga metrics |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics |

## 📝 Usage Examples

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "productId": "product-123",
    "quantity": 2,
    "amount": 150.00
  }'
```

**Response:**
```json
{
  "orderId": 1,
  "sagaId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Pedido criado e saga iniciada com sucesso"
}
```

### Check Saga Status

```bash
curl http://localhost:8080/api/sagas/550e8400-e29b-41d4-a716-446655440000/status
```

**Response:**
```json
{
  "sagaId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": 1,
  "status": "COMPLETED",
  "currentStep": "SHIPPING_ARRANGED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:05"
}
```

### Get Saga Metrics

```bash
curl http://localhost:8080/api/metrics/saga
```

**Response:**
```json
{
  "totalStarted": 100,
  "totalCompleted": 85,
  "totalFailed": 10,
  "totalCompensated": 5,
  "averageDurationMs": 2500.0,
  "successRate": 85.0,
  "failureRate": 15.0
}
```

## 🗄️ H2 Console

Access the H2 database console at: http://localhost:8080/h2-console

- **JDBC URL:** `jdbc:h2:mem:sagadb`
- **Username:** `sa`
- **Password:** *(leave empty)*

## ⚙️ Configuration

Key configuration properties in `application.yml`:

```yaml
saga:
  timeout:
    default-minutes: 5    # Saga timeout threshold
  retry:
    max-attempts: 3       # Max retry attempts
    backoff-delay: 1000   # Delay between retries (ms)
```

## 🔄 Saga States

| Status | Description |
|--------|-------------|
| `STARTED` | Saga has been initiated |
| `IN_PROGRESS` | Saga steps are being executed |
| `COMPLETED` | All steps completed successfully |
| `COMPENSATING` | Rollback in progress |
| `COMPENSATED` | Rollback completed |
| `FAILED` | Saga failed without recovery |

## 📊 Monitoring

The application exposes metrics via Micrometer:

- `saga_started_total` - Total sagas started
- `saga_completed_total` - Total sagas completed successfully
- `saga_failed_total` - Total sagas that failed
- `saga_compensated_total` - Total sagas that were compensated
- `saga_duration_seconds` - Saga execution duration

Access Prometheus metrics at: http://localhost:8080/actuator/prometheus

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📚 Additional Resources

- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Spring Cloud AWS Documentation](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/)
- [LocalStack Documentation](https://docs.localstack.cloud/)

## 📄 License

This project is for educational purposes.

## 👤 Author

**guipalm4**

