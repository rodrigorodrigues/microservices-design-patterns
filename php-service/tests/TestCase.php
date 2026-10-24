<?php

namespace Tests;

use App\Books\BookRepository;
use Illuminate\Foundation\Testing\TestCase as BaseTestCase;
use Tests\Support\InMemoryBookRepository;

abstract class TestCase extends BaseTestCase
{
    protected function setUp(): void
    {
        parent::setUp();

        // phpunit.xml sets COM_MICROSERVICE_AUTHENTICATION_JWT_KEY_VALUE to Tokens::SECRET.
        $this->app->instance(BookRepository::class, new InMemoryBookRepository);
    }
}
