use crate::middleware;
use crate::middleware::check_permissions;
use crate::models::{Claims, ErrorResponse, MessageResponse, Warehouse, WarehouseRequest, WarehouseResponse};
use actix_web::{web, HttpMessage, HttpRequest, HttpResponse, Responder};
use log::{debug, error};
use mongodb::{bson::{doc, oid::ObjectId, DateTime as BsonDateTime, to_document}, Database};
use std::env;
use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        get_warehouses,
        create_warehouse,
        get_warehouse,
        update_warehouse,
        delete_warehouse,
        actuator_health,
        actuator_info
    ),
    components(
        schemas(WarehouseResponse, WarehouseRequest, ErrorResponse, MessageResponse)
    ),
    modifiers(&SecurityAddon),
    tags(
        (name = "warehouses", description = "Warehouse management endpoints"),
        (name = "actuator", description = "Health and monitoring endpoints")
    ),
    info(
        title = "Rust Microservice API",
        version = "1.0.0",
        description = "A microservice for managing warehouses built with Actix-web and MongoDB"
    )
)]
pub struct ApiDoc;

struct SecurityAddon;

impl utoipa::Modify for SecurityAddon {
    fn modify(&self, openapi: &mut utoipa::openapi::OpenApi) {
        if let Some(components) = openapi.components.as_mut() {
            components.add_security_scheme(
                "bearer_auth",
                utoipa::openapi::security::SecurityScheme::Http(
                    utoipa::openapi::security::HttpBuilder::new()
                        .scheme(utoipa::openapi::security::HttpAuthScheme::Bearer)
                        .bearer_format("JWT")
                        .build(),
                ),
            )
        }
    }
}

pub fn config_routes(cfg: &mut web::ServiceConfig) {
    cfg.service(
        web::scope("/api/warehouses")
            .route("", web::get().to(get_warehouses))
            .route("", web::post().to(create_warehouse))
            .route("/{id}", web::get().to(get_warehouse))
            .route("/{id}", web::put().to(update_warehouse))
            .route("/{id}", web::delete().to(delete_warehouse))
    );
}

pub fn config_actuator(cfg: &mut web::ServiceConfig) {
    cfg.service(
        web::scope("/actuator")
            .route("", web::get().to(actuator_index))
            .route("/health", web::get().to(actuator_health))
            .route("/info", web::get().to(actuator_info))
    );
}

#[utoipa::path(
    get,
    path = "/api/warehouses",
    tag = "warehouses",
    params(
        ("page" = Option<u64>, Query, description = "Page number"),
        ("size" = Option<u64>, Query, description = "Page size")
    ),
    responses(
        (status = 200, description = "List of warehouses", body = Vec<WarehouseResponse>),
        (status = 401, description = "Unauthorized", body = ErrorResponse),
        (status = 403, description = "Forbidden", body = ErrorResponse)
    ),
    security(
        ("bearer_auth" = [])
    )
)]
async fn get_warehouses(
    db: web::Data<Database>,
    req: HttpRequest,
    query: web::Query<PaginationParams>,
) -> impl Responder {
    let claims: Claims = match req.extensions().get::<Claims>() {
        Some(c) => c.clone(),
        None => return HttpResponse::Unauthorized().json(ErrorResponse {
            error: "Unauthorized".to_string(),
        }),
    };

    let required_roles = ["ROLE_ADMIN", "ROLE_WAREHOUSES_READ", "ROLE_WAREHOUSES_CREATE",
                          "ROLE_WAREHOUSES_SAVE", "ROLE_WAREHOUSES_DELETE", "SCOPE_openid"];

    if !check_permissions(&claims, &required_roles) {
        return HttpResponse::Forbidden().json(ErrorResponse {
            error: "Insufficient permissions".to_string(),
        });
    }

    let collection = db.collection::<Warehouse>("warehouses");

    let page = query.page.unwrap_or(0);
    let size = query.size.unwrap_or(10);
    let skip = page * size;

    debug!("Get all warehouses - page: {}\t size: {}", page, size);

    match collection.find(None, None).await {
        Ok(mut cursor) => {
            use futures::stream::StreamExt;
            let mut warehouses = Vec::new();
            let mut count = 0;

            while let Some(result) = cursor.next().await {
                if count >= skip && warehouses.len() < size as usize {
                    if let Ok(warehouse) = result {
                        warehouses.push(warehouse);
                    }
                }
                count += 1;
            }

            let response: Vec<WarehouseResponse> = warehouses.into_iter().map(|w| w.into()).collect();
            HttpResponse::Ok().json(response)
        }
        Err(e) => {
            error!("Error fetching warehouses: {}", e);
            HttpResponse::InternalServerError().json(ErrorResponse {
                error: format!("Database error: {}", e),
            })
        }
    }
}

#[utoipa::path(
    post,
    path = "/api/warehouses",
    tag = "warehouses",
    request_body = WarehouseRequest,
    responses(
        (status = 201, description = "Warehouse created", body = WarehouseResponse),
        (status = 401, description = "Unauthorized", body = ErrorResponse),
        (status = 403, description = "Forbidden", body = ErrorResponse)
    ),
    security(
        ("bearer_auth" = [])
    )
)]
async fn create_warehouse(
    db: web::Data<Database>,
    req: HttpRequest,
    warehouse_req: web::Json<WarehouseRequest>,
) -> impl Responder {
    let claims: Claims = match req.extensions().get::<Claims>() {
        Some(c) => c.clone(),
        None => return HttpResponse::Unauthorized().json(ErrorResponse {
            error: "Unauthorized".to_string(),
        }),
    };

    let required_roles = ["ROLE_ADMIN", "ROLE_WAREHOUSES_CREATE", "SCOPE_openid"];

    if !check_permissions(&claims, &required_roles) {
        return HttpResponse::Forbidden().json(ErrorResponse {
            error: "Insufficient permissions".to_string(),
        });
    }

    let collection = db.collection::<Warehouse>("warehouses");

    let now = BsonDateTime::now();
    let warehouse = Warehouse {
        id: None,
        name: warehouse_req.name.clone(),
        quantity: warehouse_req.quantity,
        category: warehouse_req.category.clone(),
        created_date: Some(now),
        last_modified_date: None,
        created_by_user: Some(claims.sub.clone()),
        last_modified_by_user: None,
        price: warehouse_req.price,
        currency: warehouse_req.currency.clone(),
    };

    match collection.insert_one(warehouse, None).await {
        Ok(result) => {
            match collection.find_one(doc! {"_id": result.inserted_id}, None).await {
                Ok(Some(inserted_warehouse)) => HttpResponse::Created().json(WarehouseResponse::from(inserted_warehouse)),
                Ok(None) => HttpResponse::InternalServerError().json(ErrorResponse {
                    error: "Warehouse created but not found".to_string(),
                }),
                Err(e) => HttpResponse::InternalServerError().json(ErrorResponse {
                    error: format!("Error fetching created warehouse: {}", e),
                }),
            }
        }
        Err(e) => {
            error!("Error creating warehouse: {}", e);
            HttpResponse::InternalServerError().json(ErrorResponse {
                error: format!("Database error: {}", e),
            })
        }
    }
}

#[utoipa::path(
    get,
    path = "/api/warehouses/{id}",
    tag = "warehouses",
    params(
        ("id" = String, Path, description = "Warehouse ID")
    ),
    responses(
        (status = 200, description = "Warehouse found", body = WarehouseResponse),
        (status = 400, description = "Invalid ID format", body = ErrorResponse),
        (status = 401, description = "Unauthorized", body = ErrorResponse),
        (status = 403, description = "Forbidden", body = ErrorResponse),
        (status = 404, description = "Warehouse not found", body = ErrorResponse)
    ),
    security(
        ("bearer_auth" = [])
    )
)]
async fn get_warehouse(
    db: web::Data<Database>,
    req: HttpRequest,
    path: web::Path<String>,
) -> impl Responder {
    let claims: Claims = match req.extensions().get::<Claims>() {
        Some(c) => c.clone(),
        None => return HttpResponse::Unauthorized().json(ErrorResponse {
            error: "Unauthorized".to_string(),
        }),
    };

    let required_roles = ["ROLE_ADMIN", "ROLE_WAREHOUSES_READ", "ROLE_WAREHOUSES_SAVE", "SCOPE_openid"];

    if !check_permissions(&claims, &required_roles) {
        return HttpResponse::Forbidden().json(ErrorResponse {
            error: "Insufficient permissions".to_string(),
        });
    }

    let id = path.into_inner();
    let object_id = match ObjectId::parse_str(&id) {
        Ok(oid) => oid,
        Err(_) => return HttpResponse::BadRequest().json(ErrorResponse {
            error: "Invalid ID format".to_string(),
        }),
    };

    let collection = db.collection::<Warehouse>("warehouses");

    match collection.find_one(doc! {"_id": object_id}, None).await {
        Ok(Some(warehouse)) => HttpResponse::Ok().json(WarehouseResponse::from(warehouse)),
        Ok(None) => HttpResponse::NotFound().json(ErrorResponse {
            error: "Warehouse not found".to_string(),
        }),
        Err(e) => {
            error!("Error fetching warehouse: {}", e);
            HttpResponse::InternalServerError().json(ErrorResponse {
                error: format!("Database error: {}", e),
            })
        }
    }
}

#[utoipa::path(
    put,
    path = "/api/warehouses/{id}",
    tag = "warehouses",
    params(
        ("id" = String, Path, description = "Warehouse ID")
    ),
    request_body = WarehouseRequest,
    responses(
        (status = 200, description = "Warehouse updated", body = Warehouse),
        (status = 400, description = "Invalid ID format", body = ErrorResponse),
        (status = 401, description = "Unauthorized", body = ErrorResponse),
        (status = 403, description = "Forbidden", body = ErrorResponse),
        (status = 404, description = "Warehouse not found", body = ErrorResponse)
    ),
    security(
        ("bearer_auth" = [])
    )
)]
async fn update_warehouse(
    db: web::Data<Database>,
    req: HttpRequest,
    path: web::Path<String>,
    warehouse_req: web::Json<WarehouseRequest>,
) -> impl Responder {
    let claims: Claims = match req.extensions().get::<Claims>() {
        Some(c) => c.clone(),
        None => return HttpResponse::Unauthorized().json(ErrorResponse {
            error: "Unauthorized".to_string(),
        }),
    };

    let required_roles = ["ROLE_ADMIN", "ROLE_WAREHOUSES_SAVE", "SCOPE_openid"];

    if !check_permissions(&claims, &required_roles) {
        return HttpResponse::Forbidden().json(ErrorResponse {
            error: "Insufficient permissions".to_string(),
        });
    }

    let id = path.into_inner();
    let object_id = match ObjectId::parse_str(&id) {
        Ok(oid) => oid,
        Err(_) => return HttpResponse::BadRequest().json(ErrorResponse {
            error: "Invalid ID format".to_string(),
        }),
    };

    let collection = db.collection::<Warehouse>("warehouses");

    let update_doc = doc! {
        "$set": {
            "name": &warehouse_req.name,
            "quantity": warehouse_req.quantity,
            "category": &warehouse_req.category,
            "price": warehouse_req.price,
            "currency": &warehouse_req.currency,
            "lastModifiedDate": BsonDateTime::now(),
            "lastModifiedByUser": &claims.sub,
        }
    };

    match collection.update_one(doc! {"_id": object_id}, update_doc, None).await {
        Ok(result) => {
            if result.matched_count == 0 {
                return HttpResponse::NotFound().json(ErrorResponse {
                    error: "Warehouse not found".to_string(),
                });
            }

            match collection.find_one(doc! {"_id": object_id}, None).await {
                Ok(Some(warehouse)) => HttpResponse::Ok().json(WarehouseResponse::from(warehouse)),
                Ok(None) => HttpResponse::InternalServerError().json(ErrorResponse {
                    error: "Warehouse updated but not found".to_string(),
                }),
                Err(e) => HttpResponse::InternalServerError().json(ErrorResponse {
                    error: format!("Error fetching updated warehouse: {}", e),
                }),
            }
        }
        Err(e) => {
            error!("Error updating warehouse: {}", e);
            HttpResponse::InternalServerError().json(ErrorResponse {
                error: format!("Database error: {}", e),
            })
        }
    }
}

#[utoipa::path(
    delete,
    path = "/api/warehouses/{id}",
    tag = "warehouses",
    params(
        ("id" = String, Path, description = "Warehouse ID")
    ),
    responses(
        (status = 200, description = "Warehouse deleted", body = MessageResponse),
        (status = 400, description = "Invalid ID format", body = ErrorResponse),
        (status = 401, description = "Unauthorized", body = ErrorResponse),
        (status = 403, description = "Forbidden", body = ErrorResponse),
        (status = 404, description = "Warehouse not found", body = ErrorResponse)
    ),
    security(
        ("bearer_auth" = [])
    )
)]
async fn delete_warehouse(
    db: web::Data<Database>,
    req: HttpRequest,
    path: web::Path<String>,
) -> impl Responder {
    let claims: Claims = match req.extensions().get::<Claims>() {
        Some(c) => c.clone(),
        None => return HttpResponse::Unauthorized().json(ErrorResponse {
            error: "Unauthorized".to_string(),
        }),
    };

    let required_roles = ["ROLE_ADMIN", "ROLE_WAREHOUSES_DELETE", "SCOPE_openid"];

    if !check_permissions(&claims, &required_roles) {
        return HttpResponse::Forbidden().json(ErrorResponse {
            error: "Insufficient permissions".to_string(),
        });
    }

    let id = path.into_inner();
    let object_id = match ObjectId::parse_str(&id) {
        Ok(oid) => oid,
        Err(_) => return HttpResponse::BadRequest().json(ErrorResponse {
            error: "Invalid ID format".to_string(),
        }),
    };

    let collection = db.collection::<Warehouse>("warehouses");

    match collection.delete_one(doc! {"_id": object_id}, None).await {
        Ok(result) => {
            if result.deleted_count == 0 {
                return HttpResponse::NotFound().json(ErrorResponse {
                    error: "Warehouse not found".to_string(),
                });
            }

            HttpResponse::Ok().json(MessageResponse {
                msg: format!("Deleted warehouse id: {}", id),
            })
        }
        Err(e) => {
            error!("Error deleting warehouse: {}", e);
            HttpResponse::InternalServerError().json(ErrorResponse {
                error: format!("Database error: {}", e),
            })
        }
    }
}

async fn actuator_index() -> impl Responder {
    let port = env::var("SERVER_PORT").unwrap_or_else(|_| "8080".to_string());

    let actuator = serde_json::json!({
        "_links": {
            "self": {
                "href": format!("http://localhost:{}/actuator", port),
                "templated": false
            },
            "health": {
                "href": format!("http://localhost:{}/actuator/health", port),
                "templated": false
            },
            "info": {
                "href": format!("http://localhost:{}/actuator/info", port),
                "templated": false
            },
            "prometheus": {
                "href": format!("http://localhost:{}/actuator/prometheus", port),
                "templated": false
            },
            "metrics": {
                "href": format!("http://localhost:{}/actuator/metrics", port),
                "templated": false
            }
        }
    });

    HttpResponse::Ok().json(actuator)
}

#[utoipa::path(
    get,
    path = "/actuator/health",
    tag = "actuator",
    responses(
        (status = 200, description = "Health status")
    )
)]
async fn actuator_health() -> impl Responder {
    HttpResponse::Ok().json(serde_json::json!({"status": "UP"}))
}

#[utoipa::path(
    get,
    path = "/actuator/info",
    tag = "actuator",
    responses(
        (status = 200, description = "Application info")
    )
)]
async fn actuator_info() -> impl Responder {
    HttpResponse::Ok().json(serde_json::json!({}))
}

#[derive(serde::Deserialize)]
struct PaginationParams {
    page: Option<u64>,
    size: Option<u64>,
}
