# OrderFlow Load Testing Suite

Load testing scripts for the OrderFlow Flash Sale Engine using [k6](https://k6.io/).

## Installation

### Windows (Chocolatey)
```bash
choco install k6
```

### macOS (Homebrew)
```bash
brew install k6
```

### Linux (APT)
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### Download Binary
Download the latest release from: https://github.com/grafana/k6/releases

## Test Scripts

### 1. `purchase-test.js` - Flash Sale Purchase Load Test

Simulates a massive flash sale with 10,000 concurrent users attempting to purchase a limited-stock product.

**Run:**
```bash
k6 run purchase-test.js
```

**With custom base URL:**
```bash
k6 run -e BASE_URL=http://your-server:8080 purchase-test.js
```

**Configuration:**
- Ramps up to 1,000 VUs over 10 seconds
- Ramps up to 10,000 VUs over 20 seconds
- Holds 10,000 VUs for 30 seconds
- Total test duration: ~60 seconds

### 2. `confirm-test.js` - Order Confirmation Flow Test

Tests the full purchase-to-confirmation flow with lower concurrency.

**Run:**
```bash
k6 run confirm-test.js
```

**Configuration:**
- Ramps up to 100 VUs
- Tests purchase → confirmation flow
- Validates reservation token extraction and order confirmation

## Expected Results

### Flash Sale Scenario (stock = 100)

When running `purchase-test.js` against a product with 100 units in stock:

| Metric | Expected Value | Description |
|--------|----------------|-------------|
| Total Requests | ~10,000+ | All VUs attempting purchase |
| HTTP 200 (Success) | **Exactly 100** | Successful reservations |
| HTTP 409 (Sold Out) | ~9,900+ | Expected "sold out" responses |
| HTTP 5xx (Server Error) | **0** | No server errors |
| Overselling | **0** | Stock should never go negative |

### Success Criteria

| Criteria | Threshold | Status |
|----------|-----------|--------|
| Successful orders = Initial stock | `success_count == 100` | ✅ Pass if equal |
| No overselling | `success_count <= 100` | ✅ Pass if true |
| Server stability | `5xx_errors == 0` | ✅ Pass if zero |
| Response time p95 | `< 500ms` | ✅ Pass if under threshold |
| Response time p99 | `< 1000ms` | ✅ Pass if under threshold |

### Understanding the Results

- **200 OK**: Successful purchase reservation. User received a reservation token.
- **409 Conflict**: Product sold out (expected behavior after stock depleted).
- **400 Bad Request**: Invalid request format.
- **500 Internal Server Error**: Server-side error (should not occur).

## Verifying Results

### 1. Check k6 Output

After the test completes, k6 will show:
```
✓ status is 200 (success) or 409 (sold out)
✓ response has body

     checks.....................: 99.99% ✓ 10000 ✗ 1
     http_req_duration..........: avg=45.2ms  p(95)=120ms p(99)=250ms
     http_reqs..................: 10000  166.67/s
```

### 2. Verify in Dashboard

Open the OrderFlow Dashboard at `http://localhost:5173/dashboard` to see:
- **Stock Remaining**: Should be 0 after test
- **Successful Orders**: Should be exactly 100
- **Failed Requests**: Count of 409 responses (expected)
- **Orders/Second**: Peak throughput during test

### 3. Query the Database

```sql
-- Check total successful reservations
SELECT COUNT(*) FROM reservations WHERE status = 'CONFIRMED' OR status = 'PENDING';

-- Check confirmed orders
SELECT COUNT(*) FROM orders WHERE status = 'CONFIRMED';

-- Verify no overselling
SELECT available_stock FROM products WHERE id = '<product-id>';
-- Should be 0 or positive, NEVER negative
```

### 4. Check Redis

```bash
# Connect to Redis
docker exec -it <redis-container> redis-cli

# Check remaining inventory
GET inventory:00000000-0000-0000-0000-000000000001

# Check stats
GET stats:successful_orders
GET stats:failed_requests
GET stats:total_requests
```

## Troubleshooting

### High Error Rate
- Ensure backend is running: `docker-compose up -d`
- Check backend logs: `docker-compose logs backend`
- Verify Redis connection: `docker exec -it redis redis-cli PING`

### Slow Response Times
- Check database connection pool settings
- Monitor Redis memory usage
- Consider running k6 from same network as backend

### Unexpected 500 Errors
- Check backend logs for stack traces
- Verify database schema is up to date
- Ensure Redis Lua script is loading correctly

## Custom Test Scenarios

### Light Load Test
```bash
k6 run --vus 100 --duration 30s purchase-test.js
```

### Extended Stress Test
```bash
k6 run --stage 30s:1000,1m:5000,2m:10000,30s:0 purchase-test.js
```

### CI/CD Integration
```bash
k6 run --out json=results.json purchase-test.js
```

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   k6 VUs     │────▶│   Backend    │────▶│    Redis     │
│  (10,000)    │     │  (Spring)    │     │ (Lua Script) │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  PostgreSQL  │
                     │ (Reservations)│
                     └──────────────┘
```

The load test validates:
1. **Atomicity**: Redis Lua script prevents race conditions
2. **Consistency**: Stock count never goes negative
3. **Isolation**: Each user gets unique reservation token
4. **Durability**: Orders persist in PostgreSQL
