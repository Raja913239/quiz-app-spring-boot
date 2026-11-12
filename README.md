# Quiz App – Spring Boot REST API

![CI](https://github.com/Raja913239/quiz-app-spring-boot/actions/workflows/ci.yml/badge.svg)

**Live demo:** https://quiz-app-latest-hspc.onrender.com/question/allQuestions
(free hosting: the first request after 15 minutes idle can take about a minute while the app wakes up)

A backend quiz application built with **Spring Boot**, **Spring Data JPA** and **PostgreSQL**.
An admin adds questions and creates quizzes; a user fetches a quiz (without the answers), submits answers, and gets a score.

There is no front end; the APIs are tested with **Postman**, and the logic with **JUnit + Mockito** tests that run automatically in a **GitHub Actions** CI/CD pipeline.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 (Spring Web MVC, Spring Data JPA) |
| ORM | Hibernate 7 |
| Database | PostgreSQL |
| Build tool | Maven |
| Other | Lombok |
| Testing | JUnit, Mockito, MockMvc, Postman |
| Containers | Docker (multi-stage build), Docker Compose |
| CI/CD | GitHub Actions, GitHub Container Registry (ghcr.io) |
| Hosting | Render (app container), Neon (managed PostgreSQL) |

---

## Features

- Get all questions, or filter questions by category
- Add new questions
- Create a quiz with **random questions** from a chosen category
- Fetch a quiz **without exposing the right answers** (DTO pattern)
- Submit answers and get a **score**, calculated by question ID so the answer order doesn't matter
- HTTP status codes with `ResponseEntity` (200, 201, 400)

---

## Project Structure

```
quizapp
├── .github/workflows/ci.yml        # CI/CD pipeline: build, test, build and publish Docker image
├── db/init.sql                     # Sample questions for the Docker database
├── Dockerfile                      # Multi-stage image build
├── docker-compose.yml              # App + PostgreSQL in one command
├── pom.xml
└── src

src/main/java/com/telusko/quizapp
├── controller
│   ├── QuestionController.java
│   └── QuizController.java
├── service
│   ├── QuestionService.java
│   └── QuizService.java
├── dao
│   ├── QuestionDao.java
│   └── QuizDao.java
├── Entity
│   ├── Question.java          # Entity → question table
│   ├── Quiz.java              # Entity → quiz table (+ quiz_questions join table)
│   ├── QuestionWrapper.java   # DTO – question without the right answer
│   └── Response.java          # DTO – user's answer (id + response)
└── QuizappApplication.java

src/test/java/com/telusko/quizapp
├── controller
│   └── QuizControllerTest.java     # MockMvc test of POST /quiz/submit/{id}
├── service
│   └── QuizServiceTest.java        # Mockito unit tests of the scoring logic
└── QuizappApplicationTests.java    # Spring context loads (needs PostgreSQL)
```

**Request flow:** `Controller → Service → DAO → PostgreSQL`

---

## Database

| Table | Purpose |
|---|---|
| `question` | Questions with four options, right answer, category and difficulty level |
| `quiz` | Quiz ID and title |
| `quiz_questions` | Join table linking quizzes to questions (`@ManyToMany`) |

`quiz` and `quiz_questions` are created automatically by JPA.

### Sample question

```sql
INSERT INTO question (category, difficulty_level, option1, option2, option3, option4, question_title, right_answer)
VALUES ('Java', 'Easy', '100', '127', '255', '999', 'What is the maximum value of a byte in Java?', '127');
```

---

## Run with Docker (fastest: no Java or PostgreSQL needed)

Requires only [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up
```

This downloads the ready-made image `ghcr.io/raja913239/quiz-app:latest`, built and published by GitHub Actions after every successful test run, and starts two containers:

| Container | What it is |
|---|---|
| `db` | PostgreSQL 18, loaded with 15 sample questions (Java and Python) from `db/init.sql` on first start |
| `app` | The quiz app (multi-stage `Dockerfile`: JDK to build, small JRE image to run, non-root user) |

The app waits for the database health check, then starts on **http://localhost:8080**.
Try: http://localhost:8080/question/allQuestions

To build the image from your local code instead: `docker compose up --build`

Stop with `Ctrl+C`, then:
- `docker compose down`: stop and keep the data
- `docker compose down -v`: also delete the database (sample questions are reloaded on the next start)

You can also pull the image on its own:
```bash
docker pull ghcr.io/raja913239/quiz-app:latest
```

---

## Run locally (Java + PostgreSQL)

### 1. Prerequisites
- Java 21
- Maven
- PostgreSQL (pgAdmin optional)
- Postman

### 2. Create the database
```sql
CREATE DATABASE questiondb;
```

### 3. Set the database password
The password is **not stored in the code**. `application.properties` reads it from an environment variable:

```properties
spring.datasource.password=${DB_PASSWORD}
```

Set `DB_PASSWORD` to your PostgreSQL password before running:

- **Windows:** Start → "Edit environment variables for your account" → New → `DB_PASSWORD`, then restart IntelliJ
- **macOS / Linux:** `export DB_PASSWORD=your_password`

### 4. Run
```bash
mvn spring-boot:run
```
Or run `QuizappApplication` from IntelliJ. The app starts on **http://localhost:8080**.

---

## API Endpoints

### Questions

| Method | Endpoint | Description |
|---|---|---|
| GET | `/question/allQuestions` | Get all questions |
| GET | `/question/category/{category}` | Get questions by category (e.g. `Java`) |
| POST | `/question/add` | Add a new question |

**Add question – request body:**
```json
{
  "questionTitle": "What is the maximum value of a byte in Java?",
  "option1": "100",
  "option2": "127",
  "option3": "255",
  "option4": "999",
  "rightAnswer": "127",
  "difficultyLevel": "Easy",
  "category": "Java"
}
```

### Quiz

| Method | Endpoint | Description |
|---|---|---|
| POST | `/quiz/create?category=Java&numQ=5&title=JQuiz` | Create a quiz with random questions |
| GET | `/quiz/get/{id}` | Get quiz questions (without answers) |
| POST | `/quiz/submit/{id}` | Submit answers and get the score |

**Get quiz – sample response:**
```json
[
  {
    "id": 2,
    "questionTitle": "What is the maximum value of a byte in Java?",
    "option1": "100",
    "option2": "127",
    "option3": "255",
    "option4": "999"
  }
]
```

**Submit quiz – request body (any order):**
```json
[
  { "id": 12, "response": "int" },
  { "id": 2,  "response": "127" }
]
```

**Submit quiz – response:** the number of correct answers, e.g. `2`

---

## Testing

```bash
mvn test
```

| Test class | Type | What it checks |
|---|---|---|
| `QuizServiceTest` | Unit test (JUnit + Mockito, no database) | All answers correct → full score; **answers in any order → still scored correctly**; wrong answer → not counted |
| `QuizControllerTest` | Web layer test (`@WebMvcTest` + MockMvc) | `POST /quiz/submit/{id}` with a JSON body returns 200 and the score |
| `QuizappApplicationTests` | Integration (`@SpringBootTest`) | The whole application starts; needs PostgreSQL and `DB_PASSWORD` |

The `anyOrder_stillCorrect` test caught a real bug in the original tutorial logic, which compared answers by position (see below).

---

## CI/CD Pipeline (GitHub Actions)

Every push to `main` runs `.github/workflows/ci.yml`:

```
push to main
   │
   ▼
Job 1: build-and-test
   - start a temporary PostgreSQL 18 service container
   - set up Java 21 (Maven dependencies cached)
   - ./mvnw verify  → compile + unit, web-layer and integration tests
   │  (only if all tests pass)
   ▼
Job 2: docker
   - log in to GitHub Container Registry with the built-in GITHUB_TOKEN
   - build the image with the multi-stage Dockerfile (layer cache)
   - push ghcr.io/raja913239/quiz-app:latest
     and   ghcr.io/raja913239/quiz-app:<commit-sha>
   │
   ▼
Job 3: deploy
   - call the Render deploy hook (stored as a GitHub secret)
   - Render pulls the new image and restarts the app
   - Render checks /actuator/health before switching traffic
```

- If any test fails, **no image is published and nothing is deployed**.
- Pull requests run the tests only; images are published and deployed only from `main`.
- Every image is tagged with the commit it was built from, so any version can be traced and rolled back.
- Secrets (database password, deploy hook) live in Render and GitHub settings, never in the code.

- If any test fails, **no image is published**.
- Pull requests run the tests only; images are published only from `main`.
- Every image is tagged with the commit it was built from, so any version can be traced and rolled back.

---
## Health Checks (Spring Boot Actuator)

| URL | What it tells you |
|---|---|
| `/actuator/health` | Overall status plus each component: `db`, `diskSpace`, `ping` |
| `/actuator/health/liveness` | Is the app alive? (used for restarts) |
| `/actuator/health/readiness` | Is the app ready for traffic? |

If the database is unreachable, `db` turns `DOWN` and the endpoint returns HTTP 503.
Docker Compose calls `/actuator/health` every 10 seconds and marks the `app` container `healthy` or `unhealthy`.
Only `health` and `info` are exposed over the web.

---

## Key Concepts Used

- **Layered architecture**: Controller, Service, DAO
- **Spring Data JPA**: `JpaRepository`, derived query (`findByCategory`), native query with `@Query`
- **Random question selection**: `ORDER BY RANDOM() LIMIT :numQ` (native SQL)
- **`@ManyToMany`** mapping between Quiz and Question
- **DTO pattern**: `QuestionWrapper` hides `rightAnswer` from the client
- **`ResponseEntity`** with HTTP status codes
- **Dependency Injection**
- **Lombok**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Unit testing** with Mockito mocks and **web layer testing** with MockMvc
- **Containers**: multi-stage Docker build, non-root user, Docker Compose networking and health checks
- **CI/CD**: GitHub Actions with a service container, job dependencies and container registry publishing

---

## My Improvements Over the Tutorial

1. **Scoring by question ID instead of position.** The original code compared answer #1 with question #1, answer #2 with question #2, and so on, so correct answers sent in a different order scored 0. I changed it to look up each question's right answer in a `Map<questionId, rightAnswer>`, so the score is correct in any order.
2. **Unit tests that prove it.** `QuizServiceTest.anyOrder_stillCorrect` failed on the original logic (expected 2, got 0) and passes after the fix.
3. **No secrets in the repository.** The database password comes from the `DB_PASSWORD` environment variable.
4. **Upgraded to Java 21 and Spring Boot 4.1.2** (the tutorial uses Spring Boot 3), including the new modular test starters.
5. **CI/CD pipeline with GitHub Actions:** every push to `main` builds the app and runs all tests (including an integration test against a PostgreSQL service container). If the tests pass, a Docker image is built and published to GitHub Container Registry, tagged `latest` and with the commit ID.
6. **Containerized:** multi-stage Dockerfile (non-root user) and Docker Compose that starts the app with its own PostgreSQL and sample data in one command.
7. **Health checks:** Spring Boot Actuator exposes liveness and readiness endpoints, including a database check, and Docker Compose uses them to report container health.
8. **Deployed with continuous deployment:** the app runs on Render with a managed Neon PostgreSQL database. Every green push to `main` publishes a new image and redeploys automatically.

---

## Future Enhancements

- Update and delete question APIs (`@PutMapping`, `@DeleteMapping`)
- Global exception handling with `@RestControllerAdvice`
- Return `404 Not Found` for an unknown quiz ID instead of an error
- Request validation with `@Valid`
- Constructor injection instead of field injection
- Health checks with Spring Boot Actuator
- Deploy the published image to the cloud or to Kubernetes (continuous deployment)
- Split into microservices (Question Service + Quiz Service with Eureka, OpenFeign and an API Gateway)
- Health checks with Spring Boot Actuator
- Deploy the published image to the cloud or to Kubernetes (continuous deployment)
- - Kubernetes manifests (Deployment, Service, liveness/readiness probes)
---

## Acknowledgment

Built by following the **Telusko** Spring Boot quiz project on YouTube, then extended with my own fixes, tests, containerization and CI/CD.