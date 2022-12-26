<?php

use Jenssegers\Mongodb\Eloquent\Model;
use Jenssegers\Mongodb\Eloquent\SoftDeletes;

class Book extends Model
{
    use SoftDeletes;
    protected $collection = 'books';
    protected $primaryKey = 'id';
    protected $dates = ['deleted_at', 'created_date', 'last_modified_date'];
}
