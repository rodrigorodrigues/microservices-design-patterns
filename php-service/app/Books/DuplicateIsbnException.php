<?php

namespace App\Books;

use RuntimeException;

class DuplicateIsbnException extends RuntimeException
{
    public function __construct(public readonly string $isbn)
    {
        parent::__construct("A book with ISBN $isbn already exists");
    }
}
