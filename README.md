# Library App API

A Spring Boot REST API with two-factor authentication, role-based access
control, article management and an audit trail.

Everything runs in Docker. **You do not need Java, Maven, Postgres, Redis or an
email account installed** — the build compiles inside a container and the
services come up alongside it.

## Notes for reviewers

**There is no `.env` file and nothing to fill in.** Every setting is a plain
value in `src/main/resources/application.properties`, already correct for the
compose stack. The cost is that the JWT signing key is committed to this
repository — a demo key that anyone reading this can use to mint a valid
`SUPER_ADMIN` token. That is an acceptable trade for a local review and unfit
for anything reachable, so nothing here reads a secret from a file that a
deployment would need to supply differently. Real values go in environment
variables, which win over the properties file without editing it; see
[Configuration](#configuration).

**OTP emails go to Mailpit, not a real mailbox.** Logging in needs a second
factor, so testing it needs somewhere for the code to arrive. Mailpit is a fake
SMTP server in the compose file with a web inbox at http://localhost:8025 — no
account to register, no credentials to configure, and **nothing can reach a real
inbox** even if an address is mistyped. Read the code there and carry on; see
[Email](#email) for pointing it at a real relay instead.

Neither choice is load-bearing for the application itself. Mail sits behind
`EmailSenderPort` and the configuration behind Spring's environment, so both
swap out with no code change.

---

## Architecture

**Hexagonal — ports and adapters.** The rules live in the middle, every piece of
technology lives on the edge, and the dependencies only ever point inwards.

```
adapter/in/web ──▶ port/in ──▶ application ──▶ port/out ──▶ adapter/out
  controllers      use cases     services      interfaces    jpa · redis
  DTOs, HTTP                     + policies                  jjwt · smtp
                                      │
                                      ▼
                                   domain
                          records, enums, exceptions
```

| Package | What lives there |
|---|---|
| `domain/model` | `User`, `Article`, `Actor`, `Role`, `Permission` — records and enums |
| `domain/port/in` | One interface per use case: `LoginUseCase`, `ArticleCommandUseCase`, … |
| `domain/port/out` | What the app needs from the world: `UserRepositoryPort`, `OtpStorePort`, `TokenPort`, `EmailSenderPort`, `LoginAttemptPort`, `PasswordHasherPort`, `AuditTrailPort` |
| `application` | The services that implement the inbound ports, plus `policy/` — the authorisation rules |
| `adapter/in/web` | Controllers, request/response DTOs, the exception handler |
| `adapter/out` | JPA entities and mappers, Redis, JJWT, Bcrypt, SMTP |
| `audit` | The `@Auditable` aspect and the request-scoped context it reads |
| `security` | Spring Security wiring: the JWT filter and `SecurityConfig` |

`domain/` imports nothing from Spring, JPA or any library in the build — that is
checked, not hoped for: `grep -r org.springframework domain/` returns nothing.

---

## How To Run

### Prerequisites

Docker Desktop (or Docker Engine + Compose v2). Nothing else.

Images are published for `amd64` and `arm64`, so Intel, Apple Silicon and
Windows all work unchanged.

### One command

```bash
docker compose up --build
```

There is no `.env` to copy and nothing to fill in. Every setting has a working
default in `src/main/resources/application.properties`, and `docker-compose.yml`
overrides only the three values that cannot be right in both places at once —
the Postgres, Redis and SMTP hostnames, which are service names inside the
compose network and `localhost` on a laptop.

> **The bundled `JWT_SECRET` is a demo key and it is committed to this
> repository.** That is the trade: a reviewer runs one command instead of
> generating a key first. Anyone reading this repo can mint a valid
> `SUPER_ADMIN` token with it, so it is fine for a local demo and unfit for
> anything reachable. A real deployment overrides it:
>
> ```bash
> JWT_SECRET=$(openssl rand -base64 32) docker compose up
> ```
>
> Same for `SECURITY_BOOTSTRAP_PASSWORD`. An environment variable wins over
> anything in `application.properties`, so overriding either one needs no file
> edit — see [Configuration](#configuration).

First build takes a few minutes — Maven downloads the dependency tree inside the
container. Rebuilds are much faster; the local repository is kept in a Docker
cache.

### Where things are

| | URL |
|---|---|
| **Swagger UI** | http://localhost:9210/swagger-ui.html |
| OpenAPI spec | http://localhost:9210/v3/api-docs |
| **Mail inbox** (all OTP emails) | http://localhost:8025 |

A `SUPER_ADMIN` is seeded on first start:

```
username: superadmin
password: superadmin321
```

### Stopping

```bash
docker compose down       # stop, keep the database
docker compose down -v    # stop and wipe the database
```

---

## Logging in

Authentication is **two steps**. `POST /auth/login` checks the password and
returns a challenge; the JWT is only issued once the emailed code is verified.
No token exists before the second factor, so MFA is not decorative.

**1 — password**

```bash
curl -X POST http://localhost:9210/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"credential":"superadmin","password":"superadmin321"}'
```

```json
{ "success": true,
  "data": { "mfaRequired": true, "challengeId": "d86d3d6a-...", "expiresInSeconds": 300 } }
```

`credential` accepts either the username or the email address.

**2 — get the code, then exchange it**

Open **http://localhost:8025** and read the 6-digit code from the email.

```bash
curl -X POST http://localhost:9210/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"challengeId":"d86d3d6a-...","code":"481923"}'
```

```json
{ "success": true, "data": { "token": "eyJhbGciOi...", "tokenType": "Bearer" } }
```

**3 — use it**

```bash
curl http://localhost:9210/articles -H "Authorization: Bearer <token>"
```

Or click **Authorize** in Swagger UI and paste the token — the `Bearer ` prefix
is added for you.

### Notes on the login flow

- The code expires in 5 minutes and is single-use.
- 5 wrong passwords within 10 minutes locks the account for 30 minutes, keyed to
  the account rather than the string typed, so switching between username and
  email does not reset the counter.
- Wrong password, unknown user and locked-out all reveal as little as possible
  about which one it was.

---

## Endpoints

All responses use the envelope `{ "success": bool, "data": ..., "error": ... }`.

### Public

| Method | Path | |
|---|---|---|
| POST | `/auth/register` | `fullname`, `username`, `email`, `password` — always creates a `VIEWER` |
| POST | `/auth/login` | `credential`, `password` → OTP challenge |
| POST | `/auth/verify-otp` | `challengeId`, `code` → JWT |

### Articles — requires a token

| Method | Path | |
|---|---|---|
| POST | `/articles` | `title`, `content`, `visibility` (`PUBLIC` / `PRIVATE`) |
| GET | `/articles?page=&size=` | paged, newest first |
| GET | `/articles/{id}` | |
| PUT | `/articles/{id}` | |
| DELETE | `/articles/{id}` | |

New articles default to **`PRIVATE`**: forgetting to set visibility hides an
article rather than leaking it.

A private article you are not allowed to see returns **404, not 403** — a 403
would confirm the id exists.

### Users — `SUPER_ADMIN` only

| Method | Path | |
|---|---|---|
| POST | `/users` | `fullname`, `username`, `email`, `password`, `role` |
| GET | `/users?page=&size=` | |
| GET | `/users/{id}` | |
| PUT | `/users/{id}` | profile and password — **no role field** |
| PATCH | `/users/{id}/role` | `role` — the only path that moves privileges |
| DELETE | `/users/{id}` | deletes the account **and its articles** |

An administrator cannot change their own role or delete their own account,
which is what stops the last `SUPER_ADMIN` from locking everyone out.

### Audit log — `SUPER_ADMIN` only

| Method | Path |
|---|---|
| GET | `/audit-logs` |

Filters, all optional, combined with AND:

```
?actorId=1
&action=LOGIN            # REGISTER, LOGIN, OTP_VERIFY, ARTICLE_*, USER_*
&targetType=ARTICLE      # ARTICLE, USER, AUTH
&outcome=FAILURE         # SUCCESS, FAILURE
&from=2026-09-12T00:00:00Z
&to=2026-09-13T00:00:00Z
&page=0&size=20
```

14 actions are recorded, **on failure as well as success** — a denied delete and
a refused login are the entries worth having. Each row carries the actor and the
role they held at the time, the client address, the raw `User-Agent` and the
browser / OS / device parsed from it.

---

## Roles

| | Articles | Users | Audit log |
|---|---|---|---|
| **SUPER_ADMIN** | full control over any | full CRUD | read |
| **EDITOR** | create; edit and delete own; read all | — | — |
| **CONTRIBUTOR** | create and edit own; **no delete** | — | — |
| **VIEWER** | read public, plus own drafts | — | — |

`/auth/register` always creates a `VIEWER`. Use the seeded administrator to
promote anyone.

---

## Email

Mail goes to **Mailpit**, a fake SMTP server in the compose file. Every message
is visible at http://localhost:8025 and **nothing can reach a real inbox**, even
if an address is mistyped.

Running outside Docker with no mail configured, the app logs the code instead:

```
WARN ... No mail sender configured; OTP for alice@example.com is 481923 (valid 5 minutes)
```

To use a real SMTP server, set these and restart — no code change:

```
SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD,
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH, SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE
```

---

## Tests

Running the suite is the one task that needs a **JDK 17 on your machine**;
everything else happens in containers.

```bash
./mvnw test
```

197 tests. They run entirely on in-memory H2 with **no Postgres, no Redis and no
SMTP** — stopping every service on your machine does not affect them. That is
enforced rather than assumed: `LibraryappApplicationTests` fails the build if the
live datasource is anything but H2, so a config change that reconnects the suite
to a real database breaks the build instead of quietly writing to it.

`src/test/resources/application.properties` *shadows* the main one rather than
layering on top of it, so a `@SpringBootTest` added later cannot reach real
infrastructure even if whoever writes it never thinks about this.

---

## Configuration

Every setting is a plain value in `src/main/resources/application.properties`,
and together they are a complete, runnable configuration. That is why there is
no `.env` and nothing to fill in before the first run.

Two exceptions are written as `${VAR:default}`, because they are the only
settings that cannot be correct in both places at once — inside the compose
network the database and cache answer to a service name, on a laptop they
answer to `localhost`:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/libraryapp}
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}
```

`docker-compose.yml` supplies those two plus `SPRING_MAIL_HOST`, and nothing
else.

### Overriding without editing the file

**Environment variables beat this file whether or not a setting is written as a
placeholder.** Spring matches an env var to a property by uppercasing it and
turning `.` and `-` into `_`, so every line here is overridable by name:

| Setting | Environment variable |
|---|---|
| `jwt.secret` | `JWT_SECRET` |
| `jwt.expiration-ms` | `JWT_EXPIRATION_MS` |
| `server.port` | `SERVER_PORT` |
| `security.bootstrap.password` | `SECURITY_BOOTSTRAP_PASSWORD` |
| `security.bootstrap.enabled` | `SECURITY_BOOTSTRAP_ENABLED` |
| `security.lockout.max-attempts` | `SECURITY_LOCKOUT_MAX_ATTEMPTS` |
| `audit.trust-forwarded-for` | `AUDIT_TRUST_FORWARDED_FOR` |

```bash
JWT_SECRET=$(openssl rand -base64 32) \
SECURITY_BOOTSTRAP_PASSWORD=something-better \
  docker compose up
```

Host port mappings are literals in `docker-compose.yml` rather than variables.
Change them there if one collides.

---

## Ports

| Service | Host | Container |
|---|---|---|
| app | 9210 | 9210 |
| postgres | **5433** | 5432 |
| redis | **6380** | 6379 |
| mailpit UI | 8025 | 8025 |
| mailpit SMTP | 1025 | 1025 |

Postgres and Redis are deliberately not on their usual host ports, so the stack
does not collide with services already installed on the machine.

---

## Troubleshooting

**`port is already allocated`**
Something already holds 9210, 8025, 1025, 5433 or 6380. Edit the `ports:` line
of the offending service in `docker-compose.yml` — change the left-hand number
only; the right-hand one is inside the container.

**The build fails part-way through downloading**
Maven Central dropped the connection. Re-run `docker compose up --build`;
completed downloads are cached and it picks up where it stopped.

**Login says the account is locked**
Five failed attempts locks it for 30 minutes. `docker compose restart redis`
clears every counter — the lock lives in Redis, not the database.

---

## Notes on the implementation

Decisions about *behaviour*; the structural ones are under
[Architecture](#architecture).

- **The JWT is issued after OTP verification**, not after the password check.
  Issuing it earlier would make the second factor decorative.
- **`visibility` is an addition to the specified article fields.** The brief
  restricts `VIEWER` to public articles but lists no field that says which are
  public, so the rule is unenforceable without it.
- **The audit log records what happened, not which row it happened to.** It
  answers "who deleted an article, from where, and when", not "who deleted
  article 42".
- **`ddl-auto=update` builds the schema.** It adds tables and columns but never
  alters an existing constraint, so a new value on a persisted enum needs a
  manual `ALTER` on a database that already exists. Flyway is the proper fix and
  is not yet in place.
