<?php

namespace App\Auth;

use RuntimeException;

/**
 * The JWT is missing, malformed, expired, or its signature does not verify.
 */
class InvalidTokenException extends RuntimeException {}
