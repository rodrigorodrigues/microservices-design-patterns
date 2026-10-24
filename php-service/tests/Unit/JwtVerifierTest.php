<?php

namespace Tests\Unit;

use App\Auth\InvalidTokenException;
use App\Auth\JwtConfigurationException;
use App\Auth\JwtVerifier;
use Firebase\JWT\JWT;
use Illuminate\Http\Client\Factory as Http;
use Illuminate\Support\Facades\Http as HttpFacade;
use Psr\Log\NullLogger;
use Tests\Support\Tokens;
use Tests\TestCase;

class JwtVerifierTest extends TestCase
{
    private const JWKS_URI = 'http://authentication-service:9999/.well-known/jwks.json';

    public function test_refuses_to_build_without_any_verification_method(): void
    {
        $this->expectException(JwtConfigurationException::class);

        $this->verifier();
    }

    public function test_unverified_mode_must_be_opted_into_and_still_checks_expiry(): void
    {
        $verifier = $this->verifier(allowUnverified: true);

        $this->assertSame('unverified', $verifier->mode());
        $this->assertSame('someone', $verifier->verify(Tokens::unsigned(['sub' => 'someone', 'exp' => time() + 60]))['sub']);

        $this->expectException(InvalidTokenException::class);
        $verifier->verify(Tokens::unsigned(['sub' => 'someone', 'exp' => time() - 3600]));
    }

    public function test_key_value_verifies_hs256_tokens(): void
    {
        $claims = $this->verifier(keyValue: Tokens::SECRET)->verify(Tokens::hs256(['ROLE_BOOKS_READ']));

        $this->assertSame('admin@gmail.com', $claims['sub']);
        $this->assertSame(['ROLE_BOOKS_READ'], $claims['authorities']);
    }

    public function test_rejects_token_signed_with_another_secret(): void
    {
        $this->expectException(InvalidTokenException::class);

        $this->verifier(keyValue: Tokens::SECRET)->verify(Tokens::hs256(secret: 'another-secret-that-is-also-long-enough-for-hs256'));
    }

    public function test_rejects_expired_token(): void
    {
        $this->expectException(InvalidTokenException::class);

        $this->verifier(keyValue: Tokens::SECRET)->verify(Tokens::hs256(claims: ['exp' => time() - 3600]));
    }

    public function test_rejects_token_without_expiration(): void
    {
        $token = JWT::encode(['sub' => 'admin@gmail.com', 'authorities' => ['ROLE_ADMIN']], Tokens::SECRET, 'HS256');

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionMessage('no expiration');

        $this->verifier(keyValue: Tokens::SECRET)->verify($token);
    }

    public function test_rejects_malformed_token(): void
    {
        $this->expectException(InvalidTokenException::class);

        $this->verifier(keyValue: Tokens::SECRET)->verify('not-a-jwt');
    }

    public function test_pem_key_value_verifies_rs256_tokens(): void
    {
        $keys = Tokens::rsaKeyPair();

        $claims = $this->verifier(keyValue: $keys['public'])->verify(Tokens::rs256($keys['private']));

        $this->assertSame('admin@gmail.com', $claims['sub']);
    }

    public function test_pem_key_value_rejects_hs256_token_signed_with_the_public_key(): void
    {
        // Algorithm confusion: an attacker signs HS256 using the public key as the secret.
        $keys = Tokens::rsaKeyPair();
        $forged = JWT::encode(['sub' => 'attacker', 'exp' => time() + 60, 'authorities' => ['ROLE_ADMIN']], $keys['public'], 'HS256');

        $this->expectException(InvalidTokenException::class);

        $this->verifier(keyValue: $keys['public'])->verify($forged);
    }

    public function test_jwks_verifies_rs256_token_by_kid(): void
    {
        $first = Tokens::rsaKeyPair('first');
        $second = Tokens::rsaKeyPair('second');
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [$first['jwk'], $second['jwk']]])]);

        $claims = $this->verifier(jwksUri: self::JWKS_URI)->verify(Tokens::rs256($second['private'], kid: 'second'));

        $this->assertSame('admin@gmail.com', $claims['sub']);
    }

    public function test_jwks_verifies_hmac_key_published_by_authentication_service(): void
    {
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [Tokens::octJwk()]])]);

        $claims = $this->verifier(jwksUri: self::JWKS_URI)->verify(Tokens::hs256(kid: 'test'));

        $this->assertSame('admin@gmail.com', $claims['sub']);
    }

    public function test_jwks_token_without_kid_uses_the_only_key(): void
    {
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [Tokens::octJwk()]])]);

        $claims = $this->verifier(jwksUri: self::JWKS_URI)->verify(Tokens::hs256());

        $this->assertSame('admin@gmail.com', $claims['sub']);
    }

    public function test_jwks_unknown_kid_refetches_once_then_rejects(): void
    {
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [Tokens::octJwk()]])]);

        try {
            $this->verifier(jwksUri: self::JWKS_URI)->verify(Tokens::hs256(kid: 'rotated'));
            $this->fail('Expected the token to be rejected');
        } catch (InvalidTokenException $e) {
            $this->assertStringContainsString('rotated', $e->getMessage());
        }

        HttpFacade::assertSentCount(2);
    }

    public function test_jwks_document_is_cached_between_verifications(): void
    {
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [Tokens::octJwk()]])]);
        $verifier = $this->verifier(jwksUri: self::JWKS_URI);

        $verifier->verify(Tokens::hs256(kid: 'test'));
        $verifier->verify(Tokens::hs256(kid: 'test'));

        HttpFacade::assertSentCount(1);
    }

    public function test_jwks_takes_precedence_over_key_value(): void
    {
        $keys = Tokens::rsaKeyPair('rsa');
        HttpFacade::fake([self::JWKS_URI => HttpFacade::response(['keys' => [$keys['jwk']]])]);

        $verifier = $this->verifier(jwksUri: self::JWKS_URI, keyValue: Tokens::SECRET);

        $this->assertSame('jwks', $verifier->mode());
        $this->assertSame('admin@gmail.com', $verifier->verify(Tokens::rs256($keys['private'], kid: 'rsa'))['sub']);

        $this->expectException(InvalidTokenException::class);
        $verifier->verify(Tokens::hs256());
    }

    private function verifier(?string $jwksUri = null, ?string $keyValue = null, bool $allowUnverified = false): JwtVerifier
    {
        return new JwtVerifier(
            $jwksUri,
            $keyValue,
            $allowUnverified,
            $this->app->make(Http::class),
            $this->app->make('cache.store'),
            new NullLogger,
        );
    }
}
