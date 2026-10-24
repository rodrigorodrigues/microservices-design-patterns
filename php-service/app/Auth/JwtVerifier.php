<?php

namespace App\Auth;

use Firebase\JWT\JWK;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Illuminate\Contracts\Cache\Repository as Cache;
use Illuminate\Http\Client\Factory as Http;
use Psr\Log\LoggerInterface;
use Throwable;

/**
 * Verifies JWTs issued by authentication-service and returns their claims.
 *
 * Verification is chosen in this order, like scala-address-service:
 *  1. JWKS URI: keys fetched from the issuer and looked up by the token's "kid".
 *  2. Verification key value: an HS256 secret, or a PEM public key for RS256.
 *  3. Unverified mode, only when explicitly allowed.
 * With none of them configured, construction fails (ADR-0002).
 */
class JwtVerifier
{
    private const PEM_PREFIX = '-----BEGIN';

    public function __construct(
        private readonly ?string $jwksUri,
        private readonly ?string $keyValue,
        private readonly bool $allowUnverified,
        private readonly Http $http,
        private readonly Cache $cache,
        private readonly LoggerInterface $logger,
        private readonly int $jwksCacheTtl = 300,
        private readonly int $leeway = 30,
    ) {
        if (! $this->hasJwksUri() && ! $this->hasKeyValue() && ! $allowUnverified) {
            throw new JwtConfigurationException(
                'No JWT verification method configured. Set com.microservice.authentication.jwk.key-set-uri or '.
                'com.microservice.authentication.jwt.key-value, or explicitly opt into insecure mode with '.
                'com.microservice.authentication.jwt.allow-unverified=true.'
            );
        }
    }

    public function mode(): string
    {
        return match (true) {
            $this->hasJwksUri() => 'jwks',
            $this->hasKeyValue() => 'key-value',
            default => 'unverified',
        };
    }

    /**
     * @return array<string, mixed> the token's claims
     *
     * @throws InvalidTokenException
     */
    public function verify(string $token): array
    {
        try {
            $claims = match ($this->mode()) {
                'jwks' => $this->decodeWithJwks($token),
                'key-value' => $this->decode($token, $this->keyFromValue()),
                default => $this->decodeUnverified($token),
            };
        } catch (InvalidTokenException $e) {
            throw $e;
        } catch (Throwable $e) {
            throw new InvalidTokenException($e->getMessage(), previous: $e);
        }

        if (! isset($claims['exp'])) {
            throw new InvalidTokenException('Token has no expiration time');
        }

        return $claims;
    }

    /**
     * @return array<string, mixed>
     */
    private function decode(string $token, Key $key): array
    {
        JWT::$leeway = $this->leeway;

        return json_decode(json_encode(JWT::decode($token, $key)), true);
    }

    /**
     * @return array<string, mixed>
     */
    private function decodeWithJwks(string $token): array
    {
        $kid = $this->header($token)['kid'] ?? null;
        $key = $this->jwksKey($kid, refresh: false) ?? $this->jwksKey($kid, refresh: true);

        if ($key === null) {
            throw new InvalidTokenException(sprintf('No JWKS key found for kid "%s"', $kid ?? ''));
        }

        return $this->decode($token, $key);
    }

    /**
     * Finds the key for a "kid". A token without a "kid" can only use a key set
     * holding exactly one key.
     */
    private function jwksKey(?string $kid, bool $refresh): ?Key
    {
        $cacheKey = 'jwks:'.sha1((string) $this->jwksUri);

        if ($refresh) {
            $this->cache->forget($cacheKey);
        }

        $jwks = $this->cache->remember($cacheKey, $this->jwksCacheTtl, function () {
            $this->logger->info('Fetching JWKS', ['uri' => $this->jwksUri]);

            return $this->http->timeout(5)->acceptJson()->get($this->jwksUri)->throw()->json();
        });

        $keys = [];
        foreach ($jwks['keys'] ?? [] as $index => $jwk) {
            $key = JWK::parseKey($jwk, ($jwk['kty'] ?? null) === 'oct' ? 'HS256' : 'RS256');
            if ($key !== null) {
                $keys[$jwk['kid'] ?? (string) $index] = $key;
            }
        }

        if ($kid === null) {
            return count($keys) === 1 ? reset($keys) : null;
        }

        return $keys[$kid] ?? null;
    }

    private function keyFromValue(): Key
    {
        return str_starts_with(trim((string) $this->keyValue), self::PEM_PREFIX)
            ? new Key(trim((string) $this->keyValue), 'RS256')
            : new Key((string) $this->keyValue, 'HS256');
    }

    /**
     * @return array<string, mixed>
     */
    private function decodeUnverified(string $token): array
    {
        $this->logger->warning('Skipping JWT signature verification (UNSAFE): allow-unverified is set');

        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            throw new InvalidTokenException('Wrong number of segments');
        }

        $claims = json_decode(JWT::urlsafeB64Decode($parts[1]), true);
        if (! is_array($claims)) {
            throw new InvalidTokenException('Invalid claims encoding');
        }

        if (isset($claims['exp']) && time() > $claims['exp'] + $this->leeway) {
            throw new InvalidTokenException('Expired token');
        }

        return $claims;
    }

    /**
     * @return array<string, mixed>
     */
    private function header(string $token): array
    {
        $header = json_decode(JWT::urlsafeB64Decode(explode('.', $token)[0]), true);

        if (! is_array($header)) {
            throw new InvalidTokenException('Invalid header encoding');
        }

        return $header;
    }

    private function hasJwksUri(): bool
    {
        return trim((string) $this->jwksUri) !== '';
    }

    private function hasKeyValue(): bool
    {
        return trim((string) $this->keyValue) !== '';
    }
}
