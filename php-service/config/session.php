<?php

return [

    // Stateless API: requests authenticate with a JWT, nothing is kept between them.
    'driver' => env('SESSION_DRIVER', 'array'),

];
