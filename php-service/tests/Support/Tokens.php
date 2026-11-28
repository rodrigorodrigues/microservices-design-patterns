<?php

namespace Tests\Support;

use Firebase\JWT\JWT;

/**
 * Builds JWTs shaped like the ones authentication-service issues.
 */
final class Tokens
{
    public const SECRET = 'test-secret-that-is-long-enough-for-hs256-validation';

    /**
     * @param  list<string>  $authorities
     * @param  array<string, mixed>  $claims
     */
    public static function hs256(array $authorities = ['ROLE_ADMIN'], array $claims = [], string $secret = self::SECRET, ?string $kid = null): string
    {
        return JWT::encode(self::claims($authorities, $claims), $secret, 'HS256', $kid);
    }

    /**
     * @param  list<string>  $authorities
     * @param  array<string, mixed>  $claims
     */
    public static function rs256(string $privateKeyPem, array $authorities = ['ROLE_ADMIN'], array $claims = [], ?string $kid = null): string
    {
        return JWT::encode(self::claims($authorities, $claims), $privateKeyPem, 'RS256', $kid);
    }

    /**
     * @return array{private: string, public: string, jwk: array<string, string>}
     */
    public static function rsaKeyPair(string $kid = 'rsa-key'): array
    {
        $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
        openssl_pkey_export($key, $private);
        $details = openssl_pkey_get_details($key);

        return [
            'private' => $private,
            'public' => $details['key'],
            'jwk' => [
                'kty' => 'RSA',
                'kid' => $kid,
                'alg' => 'RS256',
                'use' => 'sig',
                'n' => self::base64Url($details['rsa']['n']),
                'e' => self::base64Url($details['rsa']['e']),
            ],
        ];
    }

    /**
     * The HMAC key as authentication-service publishes it outside prod.
     *
     * @return array<string, string>
     */
    public static function octJwk(string $secret = self::SECRET, string $kid = 'test'): array
    {
        return ['kty' => 'oct', 'kid' => $kid, 'alg' => 'HS256', 'k' => self::base64Url($secret)];
    }

    public static function unsigned(array $claims): string
    {
        return self::base64Url(json_encode(['alg' => 'none', 'typ' => 'JWT'])).'.'.
            self::base64Url(json_encode($claims)).'.';
    }

    public static function base64Url(string $value): string
    {
        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }

    /**
     * @param  list<string>  $authorities
     * @param  array<string, mixed>  $claims
     * @return array<string, mixed>
     */
    private static function claims(array $authorities, array $claims): array
    {
        return $claims + [
            'sub' => 'admin@gmail.com',
            'iss' => 'authentication-service',
            'iat' => time(),
            'exp' => time() + 3600,
            'authorities' => $authorities,
        ];
    }
}
