<?php

namespace App\Health;

use Illuminate\Support\Facades\DB;
use Throwable;

class MongoHealthIndicator
{
    public function isUp(): bool
    {
        try {
            DB::connection('mongodb')->getDatabase()->command(['ping' => 1]);

            return true;
        } catch (Throwable) {
            return false;
        }
    }
}
