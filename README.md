# OrderFlow — Flash Sale & Inventory Reservation Engine

A production-style flash sale system demonstrating atomic inventory management under high concurrency using Redis Lua scripts, PostgreSQL for durability, and WebSockets for real-time updates.

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.x, Spring Data JPA, Spring WebSocket |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Recharts |
| **Database** | PostgreSQL 16 |
| **Cache** | Redis 7 (atomic inventory operations via Lua scripts) |
| **Messaging** | RabbitMQ 3 (order confirmation events) |
| **Load Testing** | k6 |
| **DevOps** | Docker Compose |

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Frontend    │────▶│     Backend     │────▶│    PostgreSQL   │
│  (React + Vite) │     │  (Spring Boot)  │     │   (Durability)  │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
            ┌───────────┐ ┌───────────┐ ┌───────────┐
            │   Redis   │ │ RabbitMQ  │ │ WebSocket │
            │  (Cache)  │ │(Messaging)│ │(Real-time)│
            └───────────┘ └───────────┘ └───────────┘
```

### Key Design Decisions

- **Atomic Inventory**: Redis Lua script (`DECRBY`) ensures no overselling under concurrent load
- **Reservation TTL**: 5-minute expiry with Redis key + PostgreSQL row for durability
- **Expiry Cleanup**: Scheduled task polls for expired reservations, restores stock atomically
- **Real-time Dashboard**: STOMP over WebSocket pushes metrics every second

## Quick Start

### Prerequisites

- Docker & Docker Compose
- (Optional) Java 21 & Node.js 20 for local development

### Running with Docker Compose

1. **Clone and configure environment:**

   ```bash
   cp .env.example .env
   # Edit .env if you want to change default credentials
   ```

2. **Start all services:**

   ```bash
   docker compose up --build
   ```

3. **Access the application:**

   | Service | URL |
   |---------|-----|
   | Frontend | http://localhost:5173 |
   | Backend API | http://localhost:8080 |
   | RabbitMQ Management | http://localhost:15672 (guest/guest) |

4. **Stop services:**

   ```bash
   docker compose down
   # To remove volumes (reset data):
   docker compose down -v
   ```

### Local Development

**Backend:**

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

## API Endpoints

### Inventory

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/inventory` | Get current stock levels |

### Purchase Flow

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/purchase` | Attempt to reserve inventory |
| `GET` | `/reservation/{token}` | Check reservation status |
| `POST` | `/confirm-order` | Convert reservation to order |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/dashboard/stats` | Get current metrics (REST fallback) |
| `WS` | `/ws` | WebSocket endpoint for real-time updates |

### Request/Response Examples

**POST /purchase**

```json
// Request
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "quantity": 1
}

// Response (200 OK)
{
  "reservationToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "expiresAt": "2024-01-15T10:05:00Z",
  "message": "Reservation successful"
}

// Response (409 Conflict)
{
  "error": "SOLD_OUT",
  "message": "Product is sold out"
}
```

**POST /confirm-order**

```json
// Request
{
  "reservationToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}

// Response (200 OK)
{
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "CONFIRMED",
  "message": "Order confirmed successfully"
}
```

## Project Structure

```
Flashsale Project/
├── backend/
│   ├── src/main/java/com/orderflow/
│   │   ├── config/          # Redis, RabbitMQ, WebSocket config
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/Response objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Global error handling
│   │   ├── repository/      # Data access layer
│   │   ├── scheduler/       # Reservation expiry jobs
│   │   ├── service/         # Business logic
│   │   └── websocket/       # Real-time publishers
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── pages/           # Route pages
│   │   └── services/        # API client
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── load-testing/
│   ├── purchase-test.js     # k6 load test script
│   └── README.md
├── docker-compose.yml
├── .env.example
└── README.md
```

## Load Testing

Run the k6 load test to verify the system handles 10,000 concurrent users:

```bash
cd load-testing
k6 run purchase-test.js
```

**Success Criteria:**
- Total stock: 100 units
- Confirmed orders: exactly 100
- Oversold: 0

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | PostgreSQL username | `orderflow` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `orderflow_secret` |
| `POSTGRES_DB` | PostgreSQL database name | `orderflow` |
| `SPRING_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_REDIS_PORT` | Redis port | `6379` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ username | `guest` |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |

## License

MIT
