<?php

namespace App\Books;

use App\Models\Book;
use DateTimeImmutable;
use DateTimeInterface;
use DateTimeZone;
use MongoDB\Driver\Exception\BulkWriteException;

class MongoBookRepository implements BookRepository
{
    private const DUPLICATE_KEY = 11000;

    public function page(int $page, int $size): array
    {
        $books = Book::query()->orderBy('_id')->skip($page * $size)->take($size)->get();

        return [
            'content' => $books->map(fn (Book $book) => $this->toArray($book))->values()->all(),
            'totalElements' => Book::query()->count(),
        ];
    }

    public function find(string $id): ?array
    {
        $book = Book::find($id);

        return $book ? $this->toArray($book) : null;
    }

    public function findByIsbn(string $isbn): ?array
    {
        $book = Book::where('isbn', $isbn)->first();

        return $book ? $this->toArray($book) : null;
    }

    public function create(array $attributes): array
    {
        $book = new Book($attributes);
        $this->save($book);

        return $this->toArray($book);
    }

    public function update(string $id, array $attributes): ?array
    {
        $book = Book::find($id);

        if ($book === null) {
            return null;
        }

        $book->fill($attributes);
        $this->save($book);

        return $this->toArray($book);
    }

    public function delete(string $id): bool
    {
        return (bool) Book::find($id)?->delete();
    }

    public function ensureIndexes(): void
    {
        Book::raw(fn ($collection) => $collection->createIndex(['isbn' => 1], ['unique' => true, 'name' => 'isbn_unique']));
    }

    private function save(Book $book): void
    {
        try {
            $book->save();
        } catch (BulkWriteException $e) {
            if ($e->getCode() === self::DUPLICATE_KEY) {
                throw new DuplicateIsbnException((string) $book->isbn);
            }
            throw $e;
        }
    }

    /**
     * @return array<string, mixed>
     */
    private function toArray(Book $book): array
    {
        return [
            'id' => (string) $book->getKey(),
            'title' => $book->title,
            'author' => $book->author,
            'isbn' => $book->isbn,
            'publishedYear' => $book->publishedYear,
            'price' => $book->price,
            'currency' => $book->currency,
            'createdByUser' => $book->createdByUser,
            'createdDate' => $this->formatDate($book->createdDate),
            'lastModifiedByUser' => $book->lastModifiedByUser,
            'lastModifiedDate' => $this->formatDate($book->lastModifiedDate),
        ];
    }

    private function formatDate(mixed $date): ?string
    {
        return $date instanceof DateTimeInterface
            ? DateTimeImmutable::createFromInterface($date)->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d\TH:i:s.v\Z')
            : null;
    }
}
