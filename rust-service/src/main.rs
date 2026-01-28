
mod models;
mod handlers;
mod middleware;
mod config;
mod database;
mod consul;

use actix_web::{web, App, HttpServer, middleware::Logger};
use actix_cors::Cors;
use actix_web_prometheus::PrometheusMetricsBuilder;
use chrono::format;
use dotenv::dotenv;
use std::env;
use log::{info, warn};
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv().ok();
    env_logger::init();

    let mut cfg = config::AppConfig::from_env();

    info!("Starting {} on port {}", cfg.app_name, cfg.server_port);

    // Initialize MongoDB connection
    let db_client = database::init_db(&cfg.mongodb_uri).await
        .expect("Failed to connect to MongoDB");

    // Initialize Prometheus metrics
    let prometheus = PrometheusMetricsBuilder::new("api")
        .endpoint("/actuator/prometheus")
        .build()
        .unwrap();

    // Register with Consul if enabled and read SECRET_TOKEN from Consul KV
    if cfg.consul_enabled {
        match consul::ConsulClient::new(&cfg.consul_server, cfg.consul_port) {
            Ok(consul_client) => {
                // Read SECRET_TOKEN from Consul KV if not set in environment
                if !cfg.jwks_validation && cfg.secret_token.is_empty() {
                    info!("SECRET_TOKEN not found in environment, reading from Consul KV");
                    match consul_client.read_kv(&format!("{}/{}", cfg.consul_kv_path, cfg.profile)).await {
                        Ok(token) => {
                            cfg.set_secret_token(token);
                            info!("Successfully loaded SECRET_TOKEN from Consul KV");
                        }
                        Err(e) => warn!("Failed to read SECRET_TOKEN from Consul: {}. Using empty token.", e),
                    }
                }

                let service_id = format!("{}-{}", cfg.app_name, cfg.server_port);
                match consul_client.register_service(
                    &cfg.app_name.to_uppercase(),
                    &service_id,
                    &cfg.host_name,
                    cfg.server_port,
                ).await {
                    Ok(_) => info!("Successfully registered with Consul"),
                    Err(e) => warn!("Failed to register with Consul: {}", e),
                }
            }
            Err(e) => warn!("Failed to create Consul client: {}", e),
        }
    } else {
        info!("Consul registration is disabled");
    }

    let openapi = handlers::ApiDoc::openapi();

    HttpServer::new(move || {
        let cors = Cors::permissive();

        App::new()
            .app_data(web::Data::new(db_client.clone()))
            .wrap(cors)
            .wrap(Logger::default())
            .wrap(prometheus.clone())
            .service(
                SwaggerUi::new("/swagger-ui/{_:.*}")
                    .url("/api-docs/openapi.json", openapi.clone())
            )
            .wrap(middleware::JwtAuth::new(
                cfg.jwks_validation.clone(),
                cfg.jwks_url.clone(),
                cfg.secret_token.clone(),
                cfg.algorithm.clone()
            ))
            .configure(handlers::config_routes)
            .configure(handlers::config_actuator)
    })
    .bind(("0.0.0.0", cfg.server_port))?
    .run()
    .await
}
