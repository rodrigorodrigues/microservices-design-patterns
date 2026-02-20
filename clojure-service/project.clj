(defproject clojure-service "0.0.1-SNAPSHOT"
  :description "Clojure Microservice for Product Management"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.0"]
                 [cheshire "5.11.0"]                   ; JSON parsing
                 [com.novemberain/monger "3.5.0"]      ; MongoDB client
                 [buddy/buddy-auth "3.0.323"]          ; JWT & Auth
                 [clj-http "3.12.3"]                   ; HTTP client for Consul
                 [environ "1.2.0"]]                    ; Environment variables
  :main ^:skip-aot clojure-service.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})