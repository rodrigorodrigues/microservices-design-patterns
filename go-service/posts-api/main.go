package main

import (
	"context"
	"crypto/rsa"
	"crypto/tls"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/go-playground/validator/v10"
	"github.com/golang-jwt/jwt/v5"
	"github.com/hashicorp/consul/api"
	"github.com/labstack/echo-contrib/echoprometheus"
	"github.com/labstack/echo-contrib/jaegertracing"
	echojwt "github.com/labstack/echo-jwt/v4"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/labstack/gommon/log"
	echoSwagger "github.com/swaggo/echo-swagger"
	_ "github.com/swaggo/echo-swagger/example/docs" // docs are generated by Swag CLI, you have to import it.
	"go-service/posts-api/model"
	"go-service/posts-api/rest"
	"go-service/posts-api/util"
	"gopkg.in/yaml.v2"
	"io"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	restK8s "k8s.io/client-go/rest"
	"math/big"
	"net/http"
	"os"
	"strings"
)

var (
	e = echo.New()

	DefaultLoggerConfig = middleware.LoggerConfig{
		Skipper: middleware.DefaultSkipper,
		Format: `{"b3":"${header:b3}","uber-trace-id":"${header:uber-trace-id}","X-B3-TraceId":"${header:X-B3-TraceId}","X-B3-SpanId":"${header:X-B3-SpanId}","time":"${time_rfc3339_nano}","id":"${id}","remote_ip":"${remote_ip}",` +
			`"host":"${host}","method":"${method}","uri":"${uri}","user_agent":"${user_agent}",` +
			`"status":${status},"error":"${error}","latency":${latency},"latency_human":"${latency_human}"` +
			`,"bytes_in":${bytes_in},"bytes_out":${bytes_out}}` + "\n",
		CustomTimeFormat: "2006-01-02 15:04:05.00000",
	}
)

func healthCheck(c echo.Context) error {
	jsonResponse := model.JsonResponse{Status: "OK"}
	return c.JSON(http.StatusOK, jsonResponse)
}

func actuator(c echo.Context) error {
	serverPort := util.GetEnv("SERVER_PORT")
	jsonMap := make(map[string]interface{})
	jsonMap["_links"] = map[string]interface{}{
		"self": map[string]interface{}{
			"href":      fmt.Sprintf("http://localhost:%v/actuator", serverPort),
			"templated": "False",
		},
		"health": map[string]interface{}{
			"href":      fmt.Sprintf("http://localhost:%v/actuator/health", serverPort),
			"templated": "False",
		},
		"info": map[string]interface{}{
			"href":      fmt.Sprintf("http://localhost:%v/actuator/info", serverPort),
			"templated": "False",
		},
		"metrics": map[string]interface{}{
			"href":      fmt.Sprintf("http://localhost:%v/actuator/metrics", serverPort),
			"templated": "False",
		},
	}
	return c.JSON(http.StatusOK, jsonMap)
}

func init() {
	var middlewareObj echo.MiddlewareFunc
	if strings.Contains(util.GetEnv("SPRING_PROFILES_ACTIVE"), "kubernetes") {
		middlewareObj = processKubernetes()
	} else {
		client := processConsulClient()

		middlewareObj = processJwt(client)
	}

	processRestApi(middlewareObj)
}

func processKubernetes() echo.MiddlewareFunc {
	//JWT
	profile := util.GetEnv("SPRING_PROFILES_ACTIVE")
	if strings.Contains(profile, "prod") {
		return processJwt(nil)
	} else {
		// creates the in-cluster config
		config, err := restK8s.InClusterConfig()
		if err != nil {
			panic(err.Error())
		}

		// creates the client
		client, err := kubernetes.NewForConfig(config)
		if err != nil {
			panic(err.Error())
		}

		get, err := client.CoreV1().ConfigMaps("default").Get(context.TODO(), "go-service", metav1.GetOptions{})
		if err != nil {
			panic(err.Error())
		}

		var cmFile string
		if value, ok := get.Data["go-service-configmap.yml"]; ok {
			cmFile = value
		}

		log.Info(fmt.Sprintf("raw confiMap = %v", cmFile))

		yamlMap := make(map[string]interface{})
		if err = yaml.Unmarshal([]byte(cmFile), yamlMap); err != nil {
			panic(err)
		}

		log.Info(fmt.Sprintf("yaml confiMap = %v", yamlMap))
		key := yamlMap["data"].(map[interface{}]interface{})["keyValue"]
		if key == nil {
			panic("Not found jwt")
		}
		log.Info(fmt.Sprintf("jwt key = %v", key))
		return echojwt.JWT([]byte(fmt.Sprintf("%v", key)))
	}
}

func decodeBase64BigInt(s string) *big.Int {
	buffer, err := base64.URLEncoding.WithPadding(base64.NoPadding).DecodeString(s)
	if err != nil {
		log.Fatalf("failed to decode base64: %v", err)
	}

	return big.NewInt(0).SetBytes(buffer)
}

// Get Key from JWKS endpoint
func getKey(jwksEndpoint string) *rsa.PublicKey {
	customTransport := http.DefaultTransport.(*http.Transport).Clone()
	customTransport.TLSClientConfig = &tls.Config{InsecureSkipVerify: true} //For demo only to handle self-sign certs
	client := &http.Client{Transport: customTransport}
	log.Info(fmt.Sprintf("jwksEndpoint: %v", jwksEndpoint))
	resp, err := client.Get(jwksEndpoint)

	if err != nil {
		log.Fatalf("failed to connect: %v", err)
	}

	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)

	if err != nil {
		log.Fatalf("failed to read: %v", err)
	}

	type Key struct {
		N string `json:"n"`
		E string `json:"e"`
	}

	var Keys struct {
		Key []Key `json:"keys"`
	}

	if err := json.Unmarshal(body, &Keys); err != nil {
		log.Fatalf("failed to bind: %v", err)
	}

	//TODO: Handle multiple keys
	N := decodeBase64BigInt(Keys.Key[0].N)
	E := int(decodeBase64BigInt(Keys.Key[0].E).Int64())

	return &rsa.PublicKey{
		N: N,
		E: E,
	}
}

func processConsulClient() *api.Client {
	client, err := api.NewClient(api.DefaultConfig())
	if err != nil {
		panic(err)
	}
	registration := &api.AgentServiceRegistration{
		ID:      util.GetEnv("APP_ID"),
		Name:    util.GetEnv("APP_ID"),
		Address: util.GetEnv("HOSTNAME"),
		Port:    util.GetEnvAsInt("SERVER_PORT"),
	}
	if err := client.Agent().ServiceRegister(registration); err != nil {
		panic(err)
	}
	return client
}

func processJwt(config *api.Client) echo.MiddlewareFunc {
	//JWT
	middlewareObj := echojwt.JWT([]byte("secret"))
	profile := util.GetEnv("SPRING_PROFILES_ACTIVE")
	if strings.Contains(profile, "jwks") {
		issuers := map[string]string{"http://localhost:8080": "http://localhost:8080/.well-known/jwks.json", "https://spendingbetter.com": "https://spendingbetter.com/.well-known/jwks.json"}

		funcK := func(token *jwt.Token) (interface{}, error) {
			issuer, err := token.Claims.GetIssuer()
			if err != nil {
				panic(err)
			}

			return getKey(issuers[issuer]), nil
		}

		middlewareObj = echojwt.WithConfig(echojwt.Config{
			KeyFunc:       funcK,
			SigningMethod: "RS256",
		})
	} else if strings.Contains(profile, "prod") {
		bytes, err := os.ReadFile(util.GetEnv("JWT_PUBLIC_KEY"))
		if err != nil {
			panic(err)
		}
		pem, err := jwt.ParseRSAPublicKeyFromPEM(bytes)
		if err != nil {
			panic(err)
		}
		middlewareObj = echojwt.WithConfig(echojwt.Config{
			SigningKey:    pem,
			SigningMethod: "RS256",
		})
	} else {
		pair, _, err := config.KV().List(fmt.Sprintf("config/application,%v/data", profile), nil)
		if err != nil {
			panic(err)
		}
		if pair == nil {
			panic("Not found consul configuration")
		}
		log.Info(fmt.Sprintf("raw consul config = %v", string(pair[0].Value)))
		yamlMap := make(map[interface{}]interface{})
		if err = yaml.Unmarshal(pair[0].Value, yamlMap); err != nil {
			panic(err)
		}

		log.Info(fmt.Sprintf("yaml confiMap = %v", yamlMap))
		key := yamlMap["com"].(map[interface{}]interface{})["microservice"].(map[interface{}]interface{})["authentication"].(map[interface{}]interface{})["jwt"].(map[interface{}]interface{})["keyValue"]
		if key == nil {
			panic("Not found jwt")
		}
		middlewareObj = echojwt.JWT([]byte(fmt.Sprintf("%v", key)))
	}
	return middlewareObj
}

// urlSkipper ignores metrics route on some middleware
func urlSkipper(c echo.Context) bool {
	if strings.HasPrefix(c.Path(), "/actuator") || strings.HasPrefix(c.Path(), "/swagger/") {
		return true
	}
	return false
}

// @title Swagger Post API
// @version 1.0
// @description This is a sample server Post server.
// @termsOfService http://swagger.io/terms/

// @contact.name API Support
// @contact.url http://www.swagger.io/support
// @contact.email support@swagger.io

// @license.name Apache 2.0
// @license.url http://www.apache.org/licenses/LICENSE-2.0.html

// @host localhost:9091
// @BasePath /api
func processRestApi(middlewareObj echo.MiddlewareFunc) {
	// Middleware
	e.Use(middleware.Recover(),
		middleware.LoggerWithConfig(DefaultLoggerConfig))
	//middleware.CSRF())

	// Routes
	e.Logger.SetLevel(log.DEBUG)
	e.GET("/swagger/*", echoSwagger.WrapHandler)
	e.GET("/api/postsByName", rest.GetAllPostsByName, middlewareObj, rest.HasAdminPermission)
	e.GET("/api/posts", rest.GetAllPosts, middlewareObj, rest.HasValidReadPermission)
	e.POST("/api/posts", rest.CreatePost, middlewareObj, rest.HasValidCreatePermission)
	e.GET("/api/posts/:id", rest.GetPostById, middlewareObj, rest.HasValidReadPermission)
	e.PUT("/api/posts/:id", rest.UpdatePost, middlewareObj, rest.HasValidSavePermission)
	e.DELETE("/api/posts/:id", rest.DeletePost, middlewareObj, rest.HasValidDeletePermission)
	e.GET("/actuator/info", healthCheck)
	e.GET("/actuator/health", healthCheck)
	e.GET("/actuator", actuator)

	e.Validator = &model.CustomValidator{Validator: validator.New()}

	// Prometheus
	e.Use(echoprometheus.NewMiddlewareWithConfig(echoprometheus.MiddlewareConfig{
		Skipper: urlSkipper,
	}))
	e.GET("/actuator/metrics", echoprometheus.NewHandler())
}

func main() {
	if util.GetEnvAsBool("LOAD_DEFAULT_VALUES") {
		rest.CreateDefaultPosts()
	}

	// Enable tracing middleware
	c := jaegertracing.New(e, urlSkipper)
	defer c.Close()

	// Start server
	e.Logger.Fatal(e.Start(":" + util.GetEnv("SERVER_PORT")))
}
