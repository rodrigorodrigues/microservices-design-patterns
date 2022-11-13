package main

import (
	"context"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"github.com/go-playground/validator/v10"
	"github.com/hashicorp/consul/api"
	"github.com/labstack/echo-contrib/jaegertracing"
	"github.com/labstack/echo-contrib/prometheus"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/labstack/gommon/log"
	"go-service/posts-api/model"
	"go-service/posts-api/rest"
	"go-service/posts-api/util"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	restK8s "k8s.io/client-go/rest"
	"net/http"
	"strings"
)

var (
	e = echo.New()
)

func healthCheck(c echo.Context) error {
	json := model.JsonResponse{Status: "OK"}
	return c.JSON(http.StatusOK, json)
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
	if !strings.Contains(util.GetEnv("SPRING_PROFILES_ACTIVE"), "kubernetes") {
		client := processConsulClient()

		middlewareObj = processJwt(client)
	} else {
		middlewareObj = processKubernetes()
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

		yamlMap := make(map[interface{}]interface{})
		if err = yaml.Unmarshal([]byte(get.String()), yamlMap); err != nil {
			panic(err)
		}
		log.Debug(fmt.Sprintf("yaml confiMap = %v", yamlMap))
		key := yamlMap["security"].(map[interface{}]interface{})["oauth2"].(map[interface{}]interface{})["resource"].(map[interface{}]interface{})["jwt"].(map[interface{}]interface{})["keyValue"]
		if key == nil {
			panic("Not found jwt")
		}
		return middleware.JWT([]byte(fmt.Sprintf("%v", key)))
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
	middlewareObj := middleware.JWT([]byte("secret"))
	profile := util.GetEnv("SPRING_PROFILES_ACTIVE")
	if strings.Contains(profile, "prod") {
		bytes, err := ioutil.ReadFile(util.GetEnv("JWT_PUBLIC_KEY"))
		if err != nil {
			panic(err)
		}
		pem, err := jwt.ParseRSAPublicKeyFromPEM(bytes)
		if err != nil {
			panic(err)
		}
		middlewareObj = middleware.JWTWithConfig(middleware.JWTConfig{
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
		yamlMap := make(map[interface{}]interface{})
		if err = yaml.Unmarshal(pair[0].Value, yamlMap); err != nil {
			panic(err)
		}

		key := yamlMap["com"].(map[interface{}]interface{})["microservice"].(map[interface{}]interface{})["authentication"].(map[interface{}]interface{})["jwt"].(map[interface{}]interface{})["keyValue"]
		if key == nil {
			panic("Not found jwt")
		}
		middlewareObj = middleware.JWT([]byte(fmt.Sprintf("%v", key)))
	}
	return middlewareObj
}

// urlSkipper ignores metrics route on some middleware
func urlSkipper(c echo.Context) bool {
	if strings.HasPrefix(c.Path(), "/actuator") {
		return true
	}
	return false
}

func processRestApi(middlewareObj echo.MiddlewareFunc) {
	// Middleware
	e.Use(middleware.Recover(),
		middleware.Logger())
	//middleware.CSRF())

	// Routes
	e.Logger.SetLevel(log.DEBUG)
	e.GET("/api/posts", rest.GetAllPosts, middlewareObj, rest.HasValidReadPermission)
	e.POST("/api/posts", rest.CreatePost, middlewareObj, rest.HasValidCreatePermission)
	e.GET("/api/posts/:id", rest.GetPost, middlewareObj, rest.HasValidReadPermission)
	e.PUT("/api/posts/:id", rest.UpdatePost, middlewareObj, rest.HasValidSavePermission)
	e.DELETE("/api/posts/:id", rest.DeletePost, middlewareObj, rest.HasValidDeletePermission)
	e.GET("/actuator/info", healthCheck)
	e.GET("/actuator/health", healthCheck)
	e.GET("/actuator", actuator)

	e.Validator = &model.CustomValidator{Validator: validator.New()}

	// Prometheus
	p := prometheus.NewPrometheus("echo", urlSkipper)
	p.MetricsPath = "/actuator/metrics"
	p.Use(e)
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
