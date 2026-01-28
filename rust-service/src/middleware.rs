use actix_web::{
    dev::{forward_ready, Service, ServiceRequest, ServiceResponse, Transform},
    Error, HttpMessage, HttpResponse,
};
use futures::future::LocalBoxFuture;
use jsonwebtoken::{decode, decode_header, Algorithm, DecodingKey, Validation};
use log::{debug, error, info};
use std::env;
use std::future::{ready, Ready};
use std::rc::Rc;
use jwks::Jwks;
use crate::models::Claims;

pub struct JwtAuth {
    pub jwks_validation: bool,
    pub jwks_url: String,
    pub secret_token: String,
    pub algorithm: Algorithm
}

impl JwtAuth {
    pub fn new(jwks_validation: bool, jwks_url: String, secret_token: String, algorithm: Algorithm) -> Self {
        Self {jwks_validation, jwks_url, secret_token, algorithm }
    }
}

impl<S, B> Transform<S, ServiceRequest> for JwtAuth
where
    S: Service<ServiceRequest, Response=ServiceResponse<B>, Error=Error> + 'static,
    S::Future: 'static,
    B: 'static,
{
    type Response = ServiceResponse<B>;
    type Error = Error;
    type InitError = ();
    type Transform = JwtAuthMiddleware<S>;
    type Future = Ready<Result<Self::Transform, Self::InitError>>;

    fn new_transform(&self, service: S) -> Self::Future {
        ready(Ok(JwtAuthMiddleware {
            service: Rc::new(service),
            jwks_validation: self.jwks_validation,
            jwks_url: self.jwks_url.clone(),
            secret_token: self.secret_token.clone(),
            algorithm: self.algorithm.clone()
        }))
    }
}

pub struct JwtAuthMiddleware<S> {
    service: Rc<S>,
    jwks_validation: bool,
    jwks_url: String,
    secret_token: String,
    algorithm: Algorithm
}

impl<S, B> Service<ServiceRequest> for JwtAuthMiddleware<S>
where
    S: Service<ServiceRequest, Response=ServiceResponse<B>, Error=Error> + 'static,
    S::Future: 'static,
    B: 'static,
{
    type Response = ServiceResponse<B>;
    type Error = Error;
    type Future = LocalBoxFuture<'static, Result<Self::Response, Self::Error>>;

    forward_ready!(service);

    fn call(&self, req: ServiceRequest) -> Self::Future {
        let path = req.path().to_string();

        // Skip authentication for actuator endpoints
        if path.starts_with("/actuator") || path.starts_with("/swagger-ui") || path.starts_with("/api-docs") || path.starts_with("/actuator") {
            let service = self.service.clone();
            return Box::pin(async move {
                let res = service.call(req).await?;
                Ok(res)
            });
        }

        let auth_header = req.headers().get("Authorization").and_then(|h| h.to_str().ok());

        if let Some(auth_str) = auth_header {
            if let Some(token) = auth_str.strip_prefix("Bearer ") {
                let token = token.to_string();
                let jwks_validation = self.jwks_validation;
                let jwks_url = self.jwks_url.clone();
                let secret_token = self.secret_token.clone();
                let algorithm = self.algorithm.clone();
                let service = self.service.clone();
                return Box::pin(async move {
                    match validate_token(&token, jwks_validation, jwks_url, secret_token, algorithm).await {
                        Ok(claims) => {
                            info!("JWT validated successfully for user: {}", claims.sub);
                            req.extensions_mut().insert(claims);
                            let res = service.call(req).await?;
                            Ok(res)
                        }
                        Err(e) => {
                            error!("JWT validation failed: {}", e);
                            Err(actix_web::error::ErrorUnauthorized(e))
                        }
                    }
                });
            }
        }

        Box::pin(async move {
            Err(actix_web::error::ErrorUnauthorized("Token not found or invalid"))
        })
    }
}

async fn validate_token(token: &str, jwks_validation: bool,
                        jwks_url: String, secret_token: String, algorithm: Algorithm) -> Result<Claims, String> {
    // Step 4: Validate the JWT
    let mut validation = Validation::new(algorithm);

    // Set reasonable validation parameters
    validation.validate_exp = true;
    validation.validate_nbf = true;
    if jwks_validation {
        // Step 2: Fetch JWKS from the provider
        let jwks = match Jwks::from_jwks_url(jwks_url).await {
            Ok(jwks) => {
                jwks
            }
            Err(e) => {
                return Err(format!("Failed to fetch JWKS: {}", e));
            }
        };

        // Step 3: Get the specific JWK for this JWT
        let jwk = match jwks.keys.get("test") {
            Some(jwk) => {
                jwk
            }
            None => {
                return Err("Failed to fetch kid".to_owned());
            }
        };

        let token_data = decode::<Claims>(token, &jwk.decoding_key, &validation)
            .map_err(|e| format!("Failed to decode token: {}", e))?;

        Ok(token_data.claims)
    } else {
        let decoding_key = DecodingKey::from_secret(secret_token.as_bytes());

        let token_data = decode::<Claims>(token, &decoding_key, &validation)
            .map_err(|e| format!("Failed to decode token: {}", e))?;

        Ok(token_data.claims)
    }
}

pub fn check_permissions(claims: &Claims, required_roles: &[&str]) -> bool {
    if let Some(authorities) = &claims.authorities {
        for role in required_roles {
            if authorities.contains(&role.to_string()) {
                return true;
            }
        }
    }
    false
}
