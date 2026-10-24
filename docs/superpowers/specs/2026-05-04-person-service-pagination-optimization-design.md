# Person Service Pagination Performance Optimization

## Goal
Improve the performance of pagination in `person-service` by eliminating redundant database queries and parallelizing slow external REST calls.

## Problem Description
1. **Redundant Count Queries:** The current implementation of `findAll` in `PersonServiceImpl` executes two database queries:
    - `personRepository.findAll(predicate, pageable)` which, for MongoDB, already computes the total count.
    - `personRepository.count(predicate)` which is a second, redundant call to the database.
    - In some cases (e.g., `findAllByCreatedByUser`), the manual count is even potentially incorrect as it doesn't include the same filters.
2. **Serial REST Calls:** `processPost` and `processUser` make external API calls using `RestTemplate` in a serial `for` loop. For a page size of 10, this results in up to 20 serial network calls, creating significant latency.

## Proposed Solution

### 1. Database Optimization
- Use the `Page` result from `personRepository.findAll` to get the total count instead of calling `count()` manually.
- Use `persons.map(personMapper::entityToDto)` to transform the entities, which preserves the pagination information and count.
- Update `PersonMapper` to support direct mapping of `Page<Person>` to `Page<PersonDto>`.

### 2. Parallel REST Calls
- Leverage **Virtual Threads** (already configured in the project via `PersonServiceApplication`).
- Inject the `AsyncTaskExecutor` (which is a virtual thread executor) into `PersonServiceImpl`.
- Refactor `processPost` and `processUser` to use `CompletableFuture` to parallelize the REST calls for each person in the result page.
- Use `CompletableFuture.allOf(...).join()` to wait for all parallel calls to complete before returning the response.

## Architecture & Components
- **PersonServiceImpl:** Modified to handle parallelization and optimized pagination.
- **PersonMapper:** Simplified to use standard `Page.map()`.
- **PersonServiceApplication:** Provides the `AsyncTaskExecutor` (Virtual Threads).

## Performance Impact
- **Database:** Reduces query count from 2 to 1 for the main request.
- **External APIs:** Reduces latency from `O(N)` to `O(1)` (where N is the page size), as all N requests will run in parallel on virtual threads.

## Testing Strategy
- **Unit Tests:** Mock the repository and `RestTemplate` to verify that:
    - `count()` is NOT called on the repository.
    - `RestTemplate.exchange()` is called in parallel (can be verified by checking total time in a test with artificial delays).
- **Integration Tests:** Verify that the end-to-end pagination still returns correct data and total counts.
