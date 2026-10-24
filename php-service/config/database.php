<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Default Database Connection Name
    |--------------------------------------------------------------------------
    |
    | Books are stored in MongoDB, like the data of every other service in this
    | system. The database is taken from the MONGODB_URI path, else
    | MONGODB_DATABASE.
    |
    */

    'default' => env('DB_CONNECTION', 'mongodb'),

    'connections' => [

        'mongodb' => [
            'driver' => 'mongodb',
            'dsn' => env('MONGODB_URI', 'mongodb://localhost:27017/docker'),
            'database' => env('MONGODB_DATABASE')
                ?: (trim((string) parse_url((string) env('MONGODB_URI', ''), PHP_URL_PATH), '/') ?: 'docker'),
        ],

    ],

    'migrations' => [
        'table' => 'migrations',
        'update_date_on_publish' => true,
    ],

];
