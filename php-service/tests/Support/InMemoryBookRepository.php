<?php

namespace Tests\Support;

use App\Books\BookRepository;
use App\Books\DuplicateIsbnException;

class InMemoryBookRepository implements BookRepository
{
    /** @var array<string, array<string, mixed>> */
    private array $books = [];

    private int $sequence = 0;

    public function page(int $page, int $size): array
    {
        return [
            'content' => array_values(array_slice($this->books, $page * $size, $size)),
            'totalElements' => count($this->books),
        ];
    }

    public function find(string $id): ?array
    {
        return $this->books[$id] ?? null;
    }

    public function findByIsbn(string $isbn): ?array
    {
        foreach ($this->books as $book) {
            if ($book['isbn'] === $isbn) {
                return $book;
            }
        }

        return null;
    }

    public function create(array $attributes): array
    {
        $this->assertIsbnFree($attributes['isbn'], null);

        $id = sprintf('%024x', ++$this->sequence);
        $now = gmdate('Y-m-d\TH:i:s.000\Z');

        return $this->books[$id] = $this->normalize(['id' => $id] + $attributes + [
            'createdDate' => $now,
            'lastModifiedDate' => $now,
        ]);
    }

    public function update(string $id, array $attributes): ?array
    {
        if (! isset($this->books[$id])) {
            return null;
        }

        $this->assertIsbnFree($attributes['isbn'], $id);

        return $this->books[$id] = $this->normalize(
            array_merge($this->books[$id], $attributes, ['lastModifiedDate' => gmdate('Y-m-d\TH:i:s.000\Z')])
        );
    }

    public function delete(string $id): bool
    {
        if (! isset($this->books[$id])) {
            return false;
        }

        unset($this->books[$id]);

        return true;
    }

    public function ensureIndexes(): void {}

    private function assertIsbnFree(string $isbn, ?string $exceptId): void
    {
        $existing = $this->findByIsbn($isbn);
        if ($existing !== null && $existing['id'] !== $exceptId) {
            throw new DuplicateIsbnException($isbn);
        }
    }

    /**
     * @param  array<string, mixed>  $book
     * @return array<string, mixed>
     */
    private function normalize(array $book): array
    {
        $book['publishedYear'] = (int) $book['publishedYear'];
        $book['price'] = (float) $book['price'];

        return $book;
    }
}
