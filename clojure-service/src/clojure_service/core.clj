(ns clojure-service.core
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE context]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :refer [jws]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [clojure-service.consul :as consul]
            [environ.core :refer [env]])
  (:gen-class))

;; --- Database Setup ---
(let [uri (or (env :mongodb-uri) "mongodb://localhost:27017/docker")
      {:keys [conn db]} (mg/connect-via-uri uri)]
  (def db-conn conn)
  (def mongodb db))

;; --- Auth Middleware ---
(def secret (or (env :jwt-secret-key) "my-safe-secret"))
(def auth-backend (jws {:secret secret}))

(defn admin-required [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (-> (response {:error "Unauthorized"})
          (status 401)))))

;; --- Routes ---
(defroutes product-routes
  (context "/api/products" []
    (GET "/" []
      (response (mc/find-maps mongodb "products")))
    
    (POST "/" {body :body}
      (let [new-product (assoc body :created_at (java.util.Date.))]
        (mc/insert mongodb "products" new-product)
        (status (response new-product) 201)))

    (context "/:id" [id]
      (GET "/" []
        (response (mc/find-one-as-map mongodb "products" {:_id (org.bson.types.ObjectId. id)})))
      (DELETE "/" []
        (mc/remove-by-id mongodb "products" (org.bson.types.ObjectId. id))
        (response {:msg (str "Deleted product id: " id)})))))

(defroutes app-routes
  product-routes
  (GET "/actuator/health" [] (response {:status "UP"}))
  (GET "/actuator/info" [] (response {}))
  (route/not-found (response {:error "Not Found"})))

(def app
  (-> app-routes
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      wrap-json-response
      (wrap-json-body {:keywords? true})))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :server-port) "8087"))]
    (println "Starting Clojure service on port" port)
    ;; Consul registration logic would be called here
    (consul/register-service)
    (run-jetty app {:port port :join? false})))