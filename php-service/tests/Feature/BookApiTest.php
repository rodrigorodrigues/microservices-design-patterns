<?php

namespace Tests\Feature;

use Tests\Support\Tokens;
use Tests\TestCase;

class BookApiTest extends TestCase
{
    private const BOOK = [
        'title' => 'Domain-Driven Design',
        'author' => 'Eric Evans',
        'isbn' => '978-0321125217',
        'publishedYear' => 2003,
        'price' => 54.99,
        'currency' => 'USD',
    ];

    public function test_requires_a_bearer_token(): void
    {
        $this->getJson('/api/books')
            ->assertUnauthorized()
            ->assertHeader('WWW-Authenticate', 'Bearer')
            ->assertJson(['error' => 'Unauthorized']);
    }

    public function test_rejects_an_invalid_token(): void
    {
        $this->withToken(Tokens::hs256(secret: 'another-secret-that-is-also-long-enough-for-hs256'))
            ->getJson('/api/books')
            ->assertUnauthorized();
    }

    public function test_rejects_a_token_without_a_matching_authority(): void
    {
        $this->withToken(Tokens::hs256(['ROLE_PERSON_READ']))
            ->getJson('/api/books')
            ->assertForbidden()
            ->assertJson(['error' => 'Forbidden']);
    }

    public function test_read_authority_can_list_but_not_create(): void
    {
        $token = Tokens::hs256(['ROLE_BOOKS_READ']);

        $this->withToken($token)->getJson('/api/books')->assertOk();
        $this->withToken($token)->postJson('/api/books', self::BOOK)->assertForbidden();
    }

    public function test_openid_scope_is_accepted(): void
    {
        $this->withToken(Tokens::hs256(['SCOPE_openid']))->postJson('/api/books', self::BOOK)->assertCreated();
    }

    public function test_creates_a_book_and_records_who_created_it(): void
    {
        $response = $this->withToken(Tokens::hs256(['ROLE_BOOKS_CREATE'], ['sub' => 'author@example.com']))
            ->postJson('/api/books', self::BOOK)
            ->assertCreated()
            ->assertJson(self::BOOK + ['createdByUser' => 'author@example.com', 'lastModifiedByUser' => 'author@example.com']);

        $id = $response->json('id');
        $this->assertNotEmpty($id);
        $this->assertNotEmpty($response->json('createdDate'));
        $response->assertHeader('Location', url("/api/books/$id"));
    }

    public function test_validates_the_request_body(): void
    {
        $this->withToken(Tokens::hs256())
            ->postJson('/api/books', ['title' => '', 'isbn' => 'abc', 'publishedYear' => 99999, 'price' => -1, 'currency' => 'DOLLAR'])
            ->assertUnprocessable()
            ->assertJsonValidationErrors(['title', 'author', 'isbn', 'publishedYear', 'price', 'currency']);
    }

    public function test_rejects_a_duplicate_isbn(): void
    {
        $token = Tokens::hs256();
        $this->withToken($token)->postJson('/api/books', self::BOOK)->assertCreated();

        $this->withToken($token)
            ->postJson('/api/books', ['title' => 'Copy'] + self::BOOK)
            ->assertStatus(409)
            ->assertJson(['error' => 'Conflict']);
    }

    public function test_gets_a_book_by_id(): void
    {
        $id = $this->createBook();

        $this->withToken(Tokens::hs256(['ROLE_BOOKS_READ']))
            ->getJson("/api/books/$id")
            ->assertOk()
            ->assertJson(['id' => $id] + self::BOOK);
    }

    public function test_returns_404_for_an_unknown_book(): void
    {
        $token = Tokens::hs256();

        $this->withToken($token)->getJson('/api/books/000000000000000000000000')->assertNotFound();
        $this->withToken($token)->putJson('/api/books/000000000000000000000000', self::BOOK)->assertNotFound();
        $this->withToken($token)->deleteJson('/api/books/000000000000000000000000')->assertNotFound();
    }

    public function test_updates_a_book_and_records_who_changed_it(): void
    {
        $id = $this->createBook();

        $this->withToken(Tokens::hs256(['ROLE_BOOKS_SAVE'], ['sub' => 'editor@example.com']))
            ->putJson("/api/books/$id", ['price' => 39.9] + self::BOOK)
            ->assertOk()
            ->assertJson(['id' => $id, 'price' => 39.9, 'createdByUser' => 'admin@gmail.com', 'lastModifiedByUser' => 'editor@example.com']);
    }

    public function test_update_keeps_its_own_isbn_but_cannot_take_another_books(): void
    {
        $id = $this->createBook();
        $this->createBook(['isbn' => '978-0134434421']);
        $token = Tokens::hs256();

        $this->withToken($token)->putJson("/api/books/$id", self::BOOK)->assertOk();
        $this->withToken($token)->putJson("/api/books/$id", ['isbn' => '978-0134434421'] + self::BOOK)->assertStatus(409);
    }

    public function test_deletes_a_book(): void
    {
        $id = $this->createBook();
        $token = Tokens::hs256(['ROLE_BOOKS_DELETE']);

        $this->withToken($token)->deleteJson("/api/books/$id")->assertOk();
        $this->withToken(Tokens::hs256())->getJson("/api/books/$id")->assertNotFound();
    }

    public function test_lists_books_one_page_at_a_time(): void
    {
        $this->createBook(['isbn' => '978-0000000001']);
        $this->createBook(['isbn' => '978-0000000002']);
        $this->createBook(['isbn' => '978-0000000003']);

        $this->withToken(Tokens::hs256(['ROLE_BOOKS_READ']))
            ->getJson('/api/books?page=1&size=2')
            ->assertOk()
            ->assertJsonCount(1, 'content')
            ->assertJson([
                'number' => 1,
                'size' => 2,
                'totalPages' => 2,
                'totalElements' => 3,
                'first' => false,
                'last' => true,
            ]);
    }

    public function test_lists_an_empty_collection(): void
    {
        $this->withToken(Tokens::hs256())
            ->getJson('/api/books')
            ->assertOk()
            ->assertJson(['content' => [], 'number' => 0, 'size' => 10, 'totalPages' => 0, 'totalElements' => 0, 'first' => true, 'last' => true]);
    }

    public function test_rejects_page_sizes_over_100(): void
    {
        $this->withToken(Tokens::hs256())->getJson('/api/books?size=101')->assertUnprocessable();
    }

    /**
     * @param  array<string, mixed>  $overrides
     */
    private function createBook(array $overrides = []): string
    {
        return $this->withToken(Tokens::hs256())
            ->postJson('/api/books', $overrides + self::BOOK)
            ->assertCreated()
            ->json('id');
    }
}
