use jsonwebtoken::Algorithm;
use std::env;
use std::str::FromStr;

#[derive(Clone)]
pub struct AppConfig {
    pub server_port: u16,
    pub mongodb_uri: String,
    pub app_name: String,
    pub secret_token: String,
    pub consul_enabled: bool,
    pub consul_server: String,
    pub consul_port: u16,
    pub host_name: String,
    pub consul_kv_path: String,
    pub profile: String,
    pub jwks_validation: bool,
    pub jwks_url: String,
    pub algorithm: Algorithm
}

impl AppConfig {
    pub fn from_env() -> Self {
        Self {
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8080".to_string())
                .parse()
                .expect("SERVER_PORT must be a valid port number"),
            mongodb_uri: env::var("MONGODB_URI")
                .expect("MONGODB_URI must be set"),
            app_name: env::var("APP_NAME")
                .unwrap_or_else(|_| "rust-service".to_string()),
            secret_token: env::var("SECRET_TOKEN")
                .unwrap_or_else(|_| String::new()),
            consul_enabled: env::var("CONSUL_ENABLED")
                .unwrap_or_else(|_| "false".to_string())
                .parse()
                .unwrap_or(false),
            consul_server: env::var("CONSUL_SERVER")
                .unwrap_or_else(|_| "127.0.0.1".to_string()),
            consul_port: env::var("CONSUL_PORT")
                .unwrap_or_else(|_| "8500".to_string())
                .parse()
                .unwrap_or(8500),
            host_name: env::var("HOST_NAME")
                .unwrap_or_else(|_| "localhost".to_string()),
            profile: env::var("SPRING_PROFILES_ACTIVE")
                .unwrap_or_else(|_| "localhost".to_string()),
            consul_kv_path: env::var("CONSUL_KV_PATH")
                .unwrap_or_else(|_| "config/rust-service".to_string()),
            jwks_validation: env::var("JWKS_VALIDATION")
                .unwrap_or_else(|_| "false".to_string())
                .parse()
                .unwrap_or(false),
            jwks_url: env::var("JWKS_URL")
                .unwrap_or_else(|_| "http://localhost:8080/.well-known/jwks.json".to_string()),
            algorithm: Algorithm::from_str(&env::var("JWKS_ALGORITHM")
                .unwrap_or_else(|_| "HS256".to_string())).unwrap()
        }
    }

    pub fn set_secret_token(&mut self, token: String) {
        env::set_var("SECRET_TOKEN", &token);
        self.secret_token = token;
    }
}
