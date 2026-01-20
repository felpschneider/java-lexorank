# LexoRank Java - Educational Reference Project

## 📚 Project Purpose

This project demonstrates **LexoRank**, a ranking system used for efficient item reordering in Kanban-like applications (similar to Jira boards).

### What is LexoRank?

LexoRank is a lexicographic ranking algorithm that assigns string-based ranks to items. When reordering, only the moved item's rank is updated—avoiding expensive mass updates of sequential numeric IDs.

**Rank Format:** `bucket|value`
- **bucket**: 0, 1, or 2 (used for load balancing)
- **value**: base-36 alphanumeric string (0-9, a-z)
- **Minimalist approach**: Ranks start short and grow only as needed

**Example:**
```
Item A: rank = "0|h"   (initial - middle of range)
Item B: rank = "0|q"   (after A)
Item C: rank = "0|u"   (after B)

Move Item C between A and B:
Item C gets rank = "0|m" (between "0|h" and "0|q")
→ Only 1 UPDATE query, not N queries!
```

### Why LexoRank?

- **Efficient reordering**: Only the moved item is updated
- **No gaps or conflicts**: Works lexicographically
- **Scalable**: Ideal for collaborative, real-time systems

### Implementation Details

This implementation follows **Atlassian's LexoRank design**:

- ✅ **Bucket system** (0, 1, 2) for load distribution
- ✅ **Base-36 encoding** (0-9, a-z) for alphanumeric values
- ✅ **Lexicographic ordering** via string comparison
- ✅ **Dynamic rank generation** between any two existing ranks
- ✅ **Minimalist approach**: Ranks start short (`0|h`) and grow only as needed
  - First item: `0|h` (1 char)
  - Second item: `0|q` (1 char)
  - When needed: `0|hi` (2 chars) - extends automatically
- ⚠️ **Simplified for education** - production systems need:
  - Automatic bucket rebalancing when ranks grow too long
  - Concurrent access control / locking
  - Marker rows (min/max boundaries)

---

## 🧱 Tech Stack

- **Java 21**
- **Spring Boot 4.0.1**
- **Maven**
- **H2 Database** (in-memory)
- **Swagger/OpenAPI** (springdoc-openapi)
- **Lombok** (reduce boilerplate)
- **Docker**

---

## 🏗️ Architecture

This is a **simplified MVP** following clean separation of concerns:

```
com.java.lexorank
├── domain              # Domain models and business logic
│   ├── BoardItem.java
│   └── LexoRankGenerator.java
├── repository          # Data access (Spring Data JPA)
│   └── BoardItemRepository.java
├── service             # Application use cases
│   ├── BoardItemService.java
│   └── NotFoundException.java
├── controller          # REST API endpoints
│   └── BoardItemController.java
├── dto                 # Request/Response DTOs
│   ├── CreateBoardItemRequest.java
│   ├── MoveBoardItemRequest.java
│   └── BoardItemResponse.java
├── exception           # Global exception handling
│   ├── ApiError.java
│   └── RestExceptionHandler.java
└── LexoRankJavaApplication.java
```

### Design Principles

- **SOLID principles**
- **Separation of concerns**: domain, service, controller, repository
- **LexoRank logic is isolated and testable**
- **No framework dependencies in domain logic**

---

## 🚀 Running Locally

### Prerequisites

- Java 21+
- Maven 3.8+

### Steps

1. **Clone the repository**

```bash
git clone <repository-url>
cd LexoRank-Java
```

2. **Build the project**

```bash
mvn clean install
```

3. **Run the application**

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## 🐳 Running with Docker

### Build the Docker image

```bash
docker build -t lexorank-java .
```

### Run the container

```bash
docker run -p 8080:8080 lexorank-java
```

---

## 🌐 Accessing the Application

### Swagger UI (API Documentation)

```
http://localhost:8080/swagger-ui/index.html
```

### H2 Console (Database Admin)

```
http://localhost:8080/h2-console

JDBC URL: jdbc:h2:mem:lexorankdb
Username: sa
Password: (leave blank)
```

---

## 📋 Example Usage Flow

### 1. Create Items

**POST** `/api/items`

```json
{ "title": "Task A" }
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Task A",
  "rank": "0|h",
  "createdAt": "2026-01-20T10:00:00Z"
}
```

Create more items:
```json
{ "title": "Task B" }
{ "title": "Task C" }
```

### 2. List Items (Ordered by Rank)

**GET** `/api/items`

```json
[
  { "id": "...", "title": "Task A", "rank": "0|h", ... },
  { "id": "...", "title": "Task B", "rank": "0|q", ... },
  { "id": "...", "title": "Task C", "rank": "0|u", ... }
]
```

### 3. Reorder an Item

**PUT** `/api/items/{id}/move`

You can move an item to different positions:

**a) Move between two items:**
```json
{
  "leftId": "550e8400-e29b-41d4-a716-446655440000",  // Task A
  "rightId": "660e8400-e29b-41d4-a716-446655440001"  // Task B
}
```

**b) Move to the beginning (before all items):**
```json
{
  "leftId": null,
  "rightId": "550e8400-e29b-41d4-a716-446655440000"  // First item
}
```

**c) Move after a specific item (finds next item automatically):**
```json
{
  "leftId": "550e8400-e29b-41d4-a716-446655440000",  // Task A
  "rightId": null  // Will find next item after Task A
}
```

**Example:** Move **Task C** between **Task A** and **Task B**:

Response:
```json
{
  "id": "...",
  "title": "Task C",
  "rank": "0|m",  // New rank between "0|h" and "0|q"
  "createdAt": "..."
}
```

### 4. Verify Reordering

**GET** `/api/items`

```json
[
  { "id": "...", "title": "Task A", "rank": "0|h", ... },
  { "id": "...", "title": "Task C", "rank": "0|m", ... },  // ← Moved!
  { "id": "...", "title": "Task B", "rank": "0|q", ... }
]
```

✅ **Only Task C's rank was updated** (not Task A or B)

### 5. How Ranks Grow (Minimalist Approach)

Watch how ranks start small and extend only when needed:

| Action | Item A | Item B | Item C | Explanation |
|--------|--------|--------|--------|-------------|
| Create A | `0\|h` | - | - | Initial rank (middle) |
| Create B | `0\|h` | `0\|q` | - | Between h and z |
| Create C | `0\|h` | `0\|q` | `0\|u` | Between q and z |
| Insert between A & B | `0\|h` | `0\|m` | `0\|q` | New rank 'm' (between h and q) |
| Insert between A & new | `0\|h` | `0\|j` | `0\|m` | Chars getting closer... |
| Insert between A & new | `0\|h` | `0\|hi` | `0\|j` | 🎯 Extended to 2 chars! |

**Key insight**: Ranks only grow in length when there's no more space between adjacent characters.

---

## 🧪 Running Tests

```bash
mvn test
```

Tests include:
- **LexoRankGeneratorTest**: Unit tests for rank generation logic

---

## ⚠️ Limitations

This is an **educational reference project**, not production-ready:

- **Simplified LexoRank**: Uses basic string interpolation (real-world systems like Jira use base-36 buckets)
- **No concurrency handling**: Race conditions not addressed
- **In-memory database**: Data lost on restart
- **No authentication/authorization**
- **Minimal validation and error handling**

---

## 📖 Learn More

- [LexoRank Algorithm Explained](https://www.youtube.com/watch?v=OjQv9xMoFbg)
- [Jira's Lexorank Implementation](https://confluence.atlassian.com/jirakb/understand-the-lexorank-management-page-in-jira-server-779159218.html)

---

## 📄 License

This project is open-source and available for educational purposes.
