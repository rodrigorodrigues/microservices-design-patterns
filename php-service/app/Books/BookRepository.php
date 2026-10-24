<?php

namespace App\Books;

/**
 * Storage for books. Books are returned as their JSON representation:
 * id, title, author, isbn, publishedYear, price, currency and the audit fields.
 */
interface BookRepository
{
    /**
     * @return array{content: list<array<string, mixed>>, totalElements: int}
     */
    public function page(int $page, int $size): array;

    /**
     * @return array<string, mixed>|null
     */
    public function find(string $id): ?array;

    /**
     * @return array<string, mixed>|null
     */
    public function findByIsbn(string $isbn): ?array;

    /**
     * @param  array<string, mixed>  $attributes
     * @return array<string, mixed>
     *
     * @throws DuplicateIsbnException
     */
    public function create(array $attributes): array;

    /**
     * @param  array<string, mixed>  $attributes
     * @return array<string, mixed>|null null when the book does not exist
     *
     * @throws DuplicateIsbnException
     */
    public function update(string $id, array $attributes): ?array;

    public function delete(string $id): bool;

    /**
     * Creates the indexes the repository relies on (unique ISBN).
     */
    public function ensureIndexes(): void;
}
