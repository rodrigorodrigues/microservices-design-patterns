use serde::{Deserialize, Deserializer, Serialize, Serializer};
use mongodb::bson::{oid::ObjectId, DateTime as BsonDateTime};
use utoipa::ToSchema;
use chrono::{DateTime, Utc};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Warehouse {
    #[serde(rename = "_id", skip_serializing_if = "Option::is_none")]
    pub id: Option<ObjectId>,
    pub name: String,
    pub quantity: i32,
    pub category: String,
    #[serde(rename = "createdDate", skip_serializing_if = "Option::is_none")]
    pub created_date: Option<BsonDateTime>,
    #[serde(rename = "lastModifiedDate", skip_serializing_if = "Option::is_none")]
    pub last_modified_date: Option<BsonDateTime>,
    #[serde(rename = "createdByUser", skip_serializing_if = "Option::is_none")]
    pub created_by_user: Option<String>,
    #[serde(rename = "lastModifiedByUser", skip_serializing_if = "Option::is_none")]
    pub last_modified_by_user: Option<String>,
    pub price: f64,
    pub currency: String,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct WarehouseResponse {
    #[serde(rename = "_id", skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    pub name: String,
    pub quantity: i32,
    pub category: String,
    #[serde(rename = "createdDate", skip_serializing_if = "Option::is_none")]
    pub created_date: Option<String>,
    #[serde(rename = "lastModifiedDate", skip_serializing_if = "Option::is_none")]
    pub last_modified_date: Option<String>,
    #[serde(rename = "createdByUser", skip_serializing_if = "Option::is_none")]
    pub created_by_user: Option<String>,
    #[serde(rename = "lastModifiedByUser", skip_serializing_if = "Option::is_none")]
    pub last_modified_by_user: Option<String>,
    pub price: f64,
    pub currency: String,
}

impl From<Warehouse> for WarehouseResponse {
    fn from(warehouse: Warehouse) -> Self {
        WarehouseResponse {
            id: warehouse.id.map(|oid| oid.to_hex()),
            name: warehouse.name,
            quantity: warehouse.quantity,
            category: warehouse.category,
            created_date: warehouse.created_date.map(|dt| {
                let chrono_dt = DateTime::<Utc>::from_timestamp_millis(dt.timestamp_millis())
                    .unwrap_or_else(|| DateTime::<Utc>::from_timestamp(0, 0).unwrap());
                chrono_dt.to_rfc3339()
            }),
            last_modified_date: warehouse.last_modified_date.map(|dt| {
                let chrono_dt = DateTime::<Utc>::from_timestamp_millis(dt.timestamp_millis())
                    .unwrap_or_else(|| DateTime::<Utc>::from_timestamp(0, 0).unwrap());
                chrono_dt.to_rfc3339()
            }),
            created_by_user: warehouse.created_by_user,
            last_modified_by_user: warehouse.last_modified_by_user,
            price: warehouse.price,
            currency: warehouse.currency,
        }
    }
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WarehouseRequest {
    pub name: String,
    pub quantity: i32,
    pub category: String,
    pub price: f64,
    pub currency: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Claims {
    pub sub: String,
    pub exp: usize,
    pub authorities: Option<Vec<String>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ErrorResponse {
    pub error: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct MessageResponse {
    pub msg: String,
}

// Custom serializer to format BsonDateTime as ISO 8601 string (for JSON responses only)
pub fn serialize_bson_datetime<S>(
    date: &Option<BsonDateTime>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    match date {
        Some(dt) => {
            let chrono_dt = DateTime::<Utc>::from_timestamp_millis(dt.timestamp_millis())
                .unwrap_or_else(|| DateTime::<Utc>::from_timestamp(0, 0).unwrap());
            serializer.serialize_str(&chrono_dt.to_rfc3339())
        }
        None => serializer.serialize_none(),
    }
}

// Custom deserializer for BsonDateTime - handles BSON DateTime objects from MongoDB
pub fn deserialize_bson_datetime<'de, D>(
    deserializer: D,
) -> Result<Option<BsonDateTime>, D::Error>
where
    D: Deserializer<'de>,
{
    Option::<BsonDateTime>::deserialize(deserializer)
}
