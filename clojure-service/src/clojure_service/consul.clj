(ns clojure-service.consul
  (:require [clj-http.client :as client]
            [environ.core :refer [env]]
            [cheshire.core :as json]))

(defn register-service []
  (let [consul-url (or (env :consul-url) "http://localhost:8500")
        app-name "clojure-service"
        port (Integer/parseInt (or (env :server-port) "8087"))]
    (try
      (client/put (str consul-url "/v1/agent/service/register")
                  {:body (json/generate-string
                          {:ID app-name
                           :Name app-name
                           :Address "localhost"
                           :Port port
                           :Check {:HTTP (str "http://localhost:" port "/actuator/health")
                                   :Interval "30s"}})
                   :content-type :json})
      (println "Registered with Consul at" consul-url)
      (catch Exception e
        (println "Failed to register with Consul:" (.getMessage e))))))