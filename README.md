# Quiz App – Spring Boot REST API

![CI](https://github.com/Raja913239/quiz-app-spring-boot/actions/workflows/ci.yml/badge.svg)
A backend quiz application built with **Spring Boot**, **Spring Data JPA** and **PostgreSQL**.
An admin adds questions and creates quizzes; a user fetches a quiz (without the answers), submits answers, and gets a score.

There is no front end; the APIs are tested with **Postman**, and the logic with **JUnit + Mockito** unit tests.

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
| Containers | Docker, Docker Compose |

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
INSERT INTO question (category, difficultylevel, option1, option2, option3, option4, question_title, right_answer)
VALUES ('Java', 'Easy', '100', '127', '255', '999', 'What is the maximum value of a byte in Java?', '127');
```

---

## Setup & Run

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
## Run with Docker (no Java or PostgreSQL needed)

Requires only [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up --build
```

This starts two containers:

| Container | What it is |
|---|---|
| `db` | PostgreSQL 18, loaded with 15 sample questions (Java and Python) from `db/init.sql` on first start |
| `app` | The quiz app, built with a multi-stage `Dockerfile` (JDK to build, small JRE image to run, non-root user) |

The app waits for the database health check, then starts on **http://localhost:8080**.
Try: http://localhost:8080/question/allQuestions

Stop with `Ctrl+C`, then:
- `docker compose down`: stop and keep the data
- `docker compose down -v`: also delete the database (sample questions are reloaded on the next start)

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
  "difficultylevel": "Easy",
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

---

## My Improvements Over the Tutorial

1. **Scoring by question ID instead of position.** The original code compared answer #1 with question #1, answer #2 with question #2, and so on, so correct answers sent in a different order scored 0. I changed it to look up each question's right answer in a `Map<questionId, rightAnswer>`, so the score is correct in any order.
2. **Unit tests that prove it.** `QuizServiceTest.anyOrder_stillCorrect` failed on the original logic (expected 2, got 0) and passes after the fix.
3. **No secrets in the repository.** The database password comes from the `DB_PASSWORD` environment variable.
4. **Upgraded to Java 21 and Spring Boot 4.1.2** (the tutorial uses Spring Boot 3), including the new modular test starters.

---

## Future Enhancements

- Update and delete question APIs (`@PutMapping`, `@DeleteMapping`)
- Global exception handling with `@RestControllerAdvice`
- Return `404 Not Found` for an unknown quiz ID instead of an error
- Request validation with `@Valid`
- Constructor injection instead of field injection
- GitHub Actions pipeline that runs the tests on every push
- Split into microservices (Question Service + Quiz Service with Eureka, OpenFeign and an API Gateway)

---

## Acknowledgment

Built by following the **Telusko** Spring Boot quiz project on YouTube, then extended with my own fixes, tests and upgrades.