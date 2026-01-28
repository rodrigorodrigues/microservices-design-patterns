use mongodb::{Client, Database, options::ClientOptions};
use log::info;

pub async fn init_db(uri: &str) -> Result<Database, mongodb::error::Error> {
    info!("Connecting to MongoDB at {}", uri);

    let client_options = ClientOptions::parse(uri).await?;
    let client = Client::with_options(client_options)?;

    // Get database name from URI or use default
    let db_name = extract_db_name(uri).unwrap_or("warehouses_db".to_string());

    let database = client.database(&db_name);

    // Ping the database to verify connection
    database.run_command(mongodb::bson::doc! {"ping": 1}, None).await?;

    info!("Successfully connected to MongoDB database: {}", db_name);

    Ok(database)
}

fn extract_db_name(uri: &str) -> Option<String> {
    // Extract database name from MongoDB URI
    uri.split('/')
        .last()
        .and_then(|s| s.split('?').next())
        .filter(|s| !s.is_empty())
        .map(|s| s.to_string())
}
