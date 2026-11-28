<?php

use App\Http\Controllers\BookController;
use Illuminate\Support\Facades\Route;

Route::middleware('jwt')->prefix('books')->group(function () {
    Route::get('/', [BookController::class, 'index'])
        ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_READ,ROLE_BOOKS_CREATE,ROLE_BOOKS_SAVE,ROLE_BOOKS_DELETE,SCOPE_openid');
    Route::get('/{id}', [BookController::class, 'show'])
        ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_READ,ROLE_BOOKS_SAVE,SCOPE_openid');
    Route::post('/', [BookController::class, 'store'])
        ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_CREATE,SCOPE_openid');
    Route::put('/{id}', [BookController::class, 'update'])
        ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_SAVE,SCOPE_openid');
    Route::delete('/{id}', [BookController::class, 'destroy'])
        ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_DELETE,SCOPE_openid');
});
