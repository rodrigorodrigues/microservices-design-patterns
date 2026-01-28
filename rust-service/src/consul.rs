use consulrs::api::check::common::AgentServiceCheckBuilder;
use consulrs::api::service::requests::RegisterServiceRequest;
use consulrs::client::{ConsulClient as ConsulrsClient, ConsulClientSettingsBuilder};
use consulrs::service;
use consulrs::kv;
use log::{error, info};
use base64::{Engine as _, engine::general_purpose};
use yaml_rust::{YamlLoader, YamlEmitter};

pub struct ConsulClient {
    client: ConsulrsClient,
}

impl ConsulClient {
    pub fn new(host: &str, port: u16) -> Result<Self, Box<dyn std::error::Error>> {
        let address = format!("http://{}:{}", host, port);

        let settings = ConsulClientSettingsBuilder::default()
            .address(&address)
            .build()?;

        let client = ConsulrsClient::new(settings)?;

        Ok(ConsulClient { client })
    }

    pub async fn register_service(
        &self,
        service_name: &str,
        service_id: &str,
        host: &str,
        port: u16,
    ) -> Result<(), Box<dyn std::error::Error>> {
        info!("Attempting to register service {} with Consul", service_name);

        let health_check_url = format!("http://{}:{}/actuator/health", host, port);

        let check = AgentServiceCheckBuilder::default()
            .name("health_check")
            .http(&health_check_url)
            .interval("30s")
            .timeout("10s")
            .status("passing")
            .build()?;

        let mut registration = RegisterServiceRequest::builder();
        registration.id(service_id);
        registration.address(host);
        registration.port(port as u64);
        registration.check(check);

        match service::register(&self.client, service_name, Some(&mut registration)).await {
            Ok(_) => {
                info!("Service {} successfully registered with Consul", service_name);
                Ok(())
            }
            Err(e) => {
                error!("Failed to register service with Consul: {}", e);
                Err(Box::new(e))
            }
        }
    }

    pub async fn read_kv(&self, key: &str) -> Result<String, Box<dyn std::error::Error>> {
        info!("Reading key '{}' from Consul KV store", key);

        match kv::read(&self.client, key, None).await {
            Ok(mut response) => {
                let value: String = response.response.pop().unwrap().value.unwrap().try_into().unwrap();
                let yaml = YamlLoader::load_from_str(value.as_str()).unwrap();
                let doc = &yaml[0];
                let secret_token = &doc["com"]["microservice"]["authentication"]["jwt"]["keyValue"];

                if !secret_token.is_badvalue() {
                    info!("Successfully read key '{}' from Consul", key);
                    Ok(secret_token.as_str().unwrap().to_string())
                } else {
                    error!("Key '{}' not found in Consul", key);
                    Err(format!("Key '{}' not found", key).into())
                }
            }
            Err(e) => {
                error!("Failed to read key '{}' from Consul: {}", key, e);
                Err(Box::new(e))
            }
        }
    }
}
