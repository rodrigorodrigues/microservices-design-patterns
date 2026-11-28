<?php

namespace App\Models;

use MongoDB\Laravel\Eloquent\Model;

/**
 * A book stored in the MongoDB "books" collection. Audit fields follow the
 * names used by the Spring services (createdDate, createdByUser, ...).
 */
class Book extends Model
{
    public const CREATED_AT = 'createdDate';

    public const UPDATED_AT = 'lastModifiedDate';

    protected $connection = 'mongodb';

    protected $table = 'books';

    protected $fillable = [
        'title',
        'author',
        'isbn',
        'publishedYear',
        'price',
        'currency',
        'createdByUser',
        'lastModifiedByUser',
    ];

    protected function casts(): array
    {
        return [
            'publishedYear' => 'integer',
            'price' => 'float',
        ];
    }
}
