# Local production-like deployment (Docker Compose)

This runs the whole stack - MySQL, the Spring Boot backend, and the React
frontend behind nginx - in containers on a single machine. It's the smallest
production-quality setup for this project: no cloud account, no Kubernetes,
just Docker.

## Architecture

```
 browser
    |
    v
 frontend (nginx, published on host)
    |  /api/*  ---->  backend (internal network only, no published port)
    |  everything else -> the built React static files
    v
 backend
    |
    v
 mysql (published on host for local inspection, same as before this phase)
```

nginx is the only browser-facing entry point. The frontend's own code already
calls the backend through relative paths like `/api/resumes/upload` (see
`frontend/src/apiClient.ts`) - nginx serving both the static files and the
`/api/*` proxy from the same origin is what makes those calls resolve
correctly in production, with zero frontend code changes.

## Prerequisites

- Docker and Docker Compose
- A `.env` file at the repository root (copy `.env.example` and fill in real
  values - see that file's own comments, especially `JWT_SECRET`, which has
  no safe default and must be a real random value of at least 32 characters
  for any environment other than a throwaway local test)

## Running it

```bash
docker compose build
docker compose up
```

Bring it down with:

```bash
docker compose down
```

(add `-v` if you also want to drop the MySQL data volume)

## What starts, and in what order

- `mysql` starts first; `backend` waits for its healthcheck (`mysqladmin
  ping`) before starting at all, via `depends_on: condition: service_healthy`.
- `backend` runs Flyway migrations and Hibernate schema validation
  (`ddl-auto=validate`) as part of its normal Spring Boot startup - if either
  fails, the container fails fast rather than serving traffic against a
  broken schema.
- `frontend` (nginx) starts once `backend`'s container exists; it doesn't wait
  for the backend to be fully ready, since nginx itself doesn't need the
  backend up to serve static files, and its `/api/*` proxy will simply return
  an upstream error until the backend is ready.

## Feature flags

Semantic matching (`MATCHING_SEMANTIC_ENABLED`) and AI analysis
(`ANALYSIS_ENABLED`) default to `false`, exactly like `application.properties`
outside of containers. They call paid external providers, so this compose
setup never turns them on by itself - set them in your own `.env` only if you
intend to supply real provider credentials.

## Ports

- `frontend` (nginx): published on host port `8081` (mapped to its internal
  port `80`). This is the one URL to open in a browser:
  `http://localhost:8081`.
- `mysql`: still published on `3306`, unchanged from before this phase, for
  local inspection with a DB client.
- `backend`: **not published to the host at all**. It's reachable only from
  `frontend` over the internal compose network, by service name. This is
  deliberate - nginx is meant to be the only browser-facing entry point.

A real single-VM production deployment would typically put nginx behind port
80/443 with TLS termination in front of it (e.g. a managed certificate or a
separate reverse proxy) - that's outside the scope of this local validation
setup.

## Secrets

Nothing here bakes credentials into an image. `JWT_SECRET`, the database
credentials, and any provider API keys are all supplied at container start
via environment variables sourced from your own `.env` file, which is
gitignored and was never committed (see `.gitignore` and `.env.example`).
