<?php

use App\Http\Controllers\ActuatorController;
use Illuminate\Support\Facades\Route;

Route::prefix('actuator')->group(function () {
    Route::get('/', [ActuatorController::class, 'index']);
    Route::get('/health', [ActuatorController::class, 'health']);
    Route::get('/info', [ActuatorController::class, 'info']);
    Route::get('/prometheus', [ActuatorController::class, 'prometheus']);
});

Route::get('/swagger-ui', function () {
    return response(<<<'HTML'
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <title>php-service API</title>
          <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
        </head>
        <body>
          <div id="swagger-ui"></div>
          <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
          <script>SwaggerUIBundle({ url: 'api-docs/openapi.yaml', dom_id: '#swagger-ui' });</script>
        </body>
        </html>
        HTML);
});
