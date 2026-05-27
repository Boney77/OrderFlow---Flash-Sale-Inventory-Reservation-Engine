# Railway deployment — fix "Connection to localhost:5432 refused"

## Why this happens

Railway Postgres variables live on the **Postgres service**, not your backend. Until you **reference** them on the backend service, Spring Boot falls back to `localhost:5432` and crashes.

---

## Fix in Railway dashboard (do this first)

### Step 1 — Add databases (if missing)

1. **+ New** → **Database** → **PostgreSQL**
2. **+ New** → **Database** → **Redis**

### Step 2 — Link Postgres to your backend service

1. Click your **backend** service (not Postgres)
2. **Variables** tab → **+ New Variable** → **Add Reference**
3. Select the **Postgres** service
4. Add these references (names on the left must match exactly):

| Variable name | Reference value |
|---------------|-----------------|
| `POSTGRES_HOST` | `${{Postgres.PGHOST}}` |
| `POSTGRES_PORT` | `${{Postgres.PGPORT}}` |
| `POSTGRES_USER` | `${{Postgres.PGUSER}}` |
| `POSTGRES_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `POSTGRES_DB` | `${{Postgres.PGDATABASE}}` |

Also add (for Railway's dynamic port):

| Variable name | Reference value |
|---------------|-----------------|
| `SERVER_PORT` | `${{PORT}}` |

### Step 3 — Link Redis

On the same backend service, **Add Reference** → **Redis**:

| Variable name | Reference value |
|---------------|-----------------|
| `REDIS_HOST` | `${{Redis.REDISHOST}}` |
| `REDIS_PORT` | `${{Redis.REDISPORT}}` |
| `REDIS_PASSWORD` | `${{Redis.REDISPASSWORD}}` |

### Step 4 — Disable RabbitMQ (not on Railway)

Add a **raw** variable (not a reference):

```
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

Optional:

```
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

### Step 5 — Redeploy

Click **Deploy** on the backend service (or push to GitHub if auto-deploy is on).

---

## Verify

In Railway logs you should **not** see `Connection to localhost:5432 refused`.

Test the API:

```bash
curl https://YOUR-BACKEND.up.railway.app/api/inventory
```

---

## After pushing latest code (recommended)

The latest backend also supports `DATABASE_PRIVATE_URL` and auto-detects Railway. After push, you can simplify variables to:

```
DATABASE_PRIVATE_URL=${{Postgres.DATABASE_PRIVATE_URL}}
REDIS_URL=${{Redis.REDIS_URL}}
RABBITMQ_ENABLED=false
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

---

## Service settings

| Setting | Value |
|---------|--------|
| Root Directory | `backend` |
| Builder | Dockerfile |

---

## Vercel frontend (after backend works)

```
VITE_API_BASE_URL=https://YOUR-BACKEND.up.railway.app/api
VITE_WS_URL=https://YOUR-BACKEND.up.railway.app/ws
```
