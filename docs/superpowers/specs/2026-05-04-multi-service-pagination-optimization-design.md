# Service Pagination Performance Optimization

## Goal
Apply the pagination performance improvements (already implemented in `person-service`) to other relevant services, specifically `user-service` and `kotlin-service`.

## Problem Description
1.  **Redundant Count Queries**: Both `user-service` and `kotlin-service` execute manual `count()` queries in their service layer, even though the `findAll()` method from Spring Data already returns a `Page` object containing the total count.
2.  **Mapping Inefficiency**: The current mappers require the count to be passed explicitly, preventing the use of the more idiomatic `Page.map()` method.

## Proposed Solution

### 1. Update Mappers
-   **UserMapper (user-service)**: Add a default method `entityToDto(Page<User> users)` that uses `users.map(this::entityToDto)`.
-   **TaskMapper (kotlin-service)**: Add a method `entityToDto(tasks: Page<Task>): Page<TaskDto>` that uses `tasks.map(this::entityToDto)`.

### 2. Refactor Services
-   **UserServiceImpl (user-service)**:
    -   Update `findAll` and `findAllByCreatedByUser` to call `userRepository.findAll(...).map(userMapper::entityToDto)`.
    -   Remove redundant `userRepository.count(...)` calls.
-   **TaskService (kotlin-service)**:
    -   Update `findAll` methods to call `taskRepository.findAll(...).map(taskMapper::entityToDto)`.
    -   Remove redundant `taskRepository.count(...)` calls.

### 3. Virtual Threads / Parallelization
-   Currently, neither `user-service` nor `kotlin-service` make external serial REST calls in their `findAll` logic. 
-   If such calls are added in the future, we will follow the `person-service` pattern using `AsyncTaskExecutor` and `CompletableFuture`. For now, we follow YAGNI.

## Architecture & Components
-   **Mappers**: MapStruct mappers updated for functional `Page` support.
-   **Services**: Service implementations refactored for efficiency.

## Performance Impact
-   **Database**: Reduces the number of database queries from 2 manual (3 total including Spring's auto-count) to 1 (Spring's automatic count query).
-   **Latency**: Minor reduction in latency due to fewer database roundtrips.

## Testing Strategy
-   **Unit Tests**: Verify that `count()` is no longer called on the repositories using Mockito `verify`.
-   **Integration Tests**: Ensure pagination results (content and metadata) remain correct.
