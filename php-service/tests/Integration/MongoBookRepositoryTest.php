<?php

namespace Tests\Integration;

use App\Books\DuplicateIsbnException;
use App\Books\MongoBookRepository;
use App\Models\Book;
use Tests\TestCase;

/**
 * Runs against a real MongoDB. Skipped unless the mongodb extension is loaded
 * and MONGODB_TEST_URI is set, e.g. mongodb://localhost:27017/books_test.
 * The collection is emptied before each test.
 */
class MongoBookRepositoryTest extends TestCase
{
    private const BOOK = [
        'title' => 'Refactoring',
        'author' => 'Martin Fowler',
        'isbn' => '978-0134757599',
        'publishedYear' => 2018,
        'price' => 47.99,
        'currency' => 'USD',
    ];

    private MongoBookRepository $repository;

    protected function setUp(): void
    {
        parent::setUp();

        $uri = getenv('MONGODB_TEST_URI');
        if (! extension_loaded('mongodb') || ! $uri) {
            $this->markTestSkipped('Set MONGODB_TEST_URI and load the mongodb extension to run MongoDB tests');
        }

        config([
            'database.connections.mongodb.dsn' => $uri,
            'database.connections.mongodb.database' => trim((string) parse_url($uri, PHP_URL_PATH), '/') ?: 'books_test',
        ]);

        Book::query()->delete();
        $this->repository = new MongoBookRepository;
        $this->repository->ensureIndexes();
    }

    public function test_creates_and_finds_a_book(): void
    {
        $created = $this->repository->create(self::BOOK + ['createdByUser' => 'admin@gmail.com']);

        $this->assertMatchesRegularExpression('/^[0-9a-f]{24}$/', $created['id']);
        $this->assertMatchesRegularExpression('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/', $created['createdDate']);
        $this->assertSame(self::BOOK + ['createdByUser' => 'admin@gmail.com'], array_intersect_key($created, self::BOOK + ['createdByUser' => null]));
        $this->assertSame($created, $this->repository->find($created['id']));
        $this->assertSame($created['id'], $this->repository->findByIsbn(self::BOOK['isbn'])['id']);
    }

    public function test_unknown_or_malformed_ids_are_not_found(): void
    {
        $this->assertNull($this->repository->find('000000000000000000000000'));
        $this->assertNull($this->repository->find('not-an-object-id'));
        $this->assertNull($this->repository->update('000000000000000000000000', self::BOOK));
        $this->assertFalse($this->repository->delete('000000000000000000000000'));
    }

    public function test_unique_index_rejects_a_duplicate_isbn(): void
    {
        $this->repository->create(self::BOOK);

        $this->expectException(DuplicateIsbnException::class);
        $this->repository->create(['title' => 'Copy'] + self::BOOK);
    }

    public function test_updates_and_deletes(): void
    {
        $id = $this->repository->create(self::BOOK)['id'];

        $updated = $this->repository->update($id, ['price' => 30.5, 'lastModifiedByUser' => 'editor']);
        $this->assertSame(30.5, $updated['price']);
        $this->assertSame('editor', $updated['lastModifiedByUser']);

        $this->assertTrue($this->repository->delete($id));
        $this->assertNull($this->repository->find($id));
    }

    public function test_pages_through_books_in_insertion_order(): void
    {
        foreach (['978-0000000001', '978-0000000002', '978-0000000003'] as $isbn) {
            $this->repository->create(['isbn' => $isbn] + self::BOOK);
        }

        $page = $this->repository->page(1, 2);

        $this->assertSame(3, $page['totalElements']);
        $this->assertCount(1, $page['content']);
        $this->assertSame('978-0000000003', $page['content'][0]['isbn']);
    }
}
