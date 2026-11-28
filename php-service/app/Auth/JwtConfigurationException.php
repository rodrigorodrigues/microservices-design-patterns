<?php

namespace App\Auth;

use RuntimeException;

/**
 * No JWT verification method is configured, so the service must not serve requests.
 */
class JwtConfigurationException extends RuntimeException {}
