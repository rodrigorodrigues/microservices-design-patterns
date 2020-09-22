package main

import (
	"fmt"
	"github.com/ArthurHlt/go-eureka-client/eureka"
	"github.com/Piszmog/cloudconfigclient"
	"github.com/dgrijalva/jwt-go"
	"github.com/joho/godotenv"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/labstack/gommon/log"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
)

type (
	post struct {
		ID   int    `json:"id"`
		Name string `json:"name"`
	}
)

var (
	posts = map[int]*post{}
	echoRestApi = echo.New()
)

//----------
// Handlers
//----------
func createDefaultPosts() {
	u := &post{
		ID: 1,
		Name: "Golang",
	}
	posts[u.ID] = u
	u = &post{
		ID:   2,
		Name: "Test",
	}
	posts[u.ID] = u
}

func getAllPosts(c echo.Context) error {
	return c.JSON(http.StatusOK, posts)
}

func createPost(c echo.Context) error {
	seq := len(posts)+1
	u := &post{
		ID: seq,
		Name: c.FormValue("name"),
	}
	if err := c.Bind(u); err != nil {
		return err
	}
	posts[u.ID] = u
	return c.JSON(http.StatusCreated, u)
}

func getPost(c echo.Context) error {
	id, _ := strconv.Atoi(c.Param("id"))
	return c.JSON(http.StatusOK, posts[id])
}

func updatePost(c echo.Context) error {
	u := new(post)
	if err := c.Bind(u); err != nil {
		return err
	}
	id, _ := strconv.Atoi(c.Param("id"))
	posts[id].Name = u.Name
	return c.JSON(http.StatusOK, posts[id])
}

func deletePost(c echo.Context) error {
	id, _ := strconv.Atoi(c.Param("id"))
	delete(posts, id)
	return c.NoContent(http.StatusNoContent)
}

func getEnv(key string, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}

	return defaultVal
}

func getEnvAsInt(name string, defaultVal int) int {
	valueStr := getEnv(name, "")
	if value, err := strconv.Atoi(valueStr); err == nil {
		return value
	}

	return defaultVal
}

func init()  {
	// loads values from .env into the system
	if err := godotenv.Load("./go-service/.env"); err != nil {
		panic(err)
	}

	client := eureka.NewClient([]string{
		getEnv("EUREKA_SERVER", "http://127.0.0.1:8761/eureka"), //From a spring boot based eureka server
		// add others servers here
	})
	appId := getEnv("APP_ID", "go-service")
	instance := eureka.NewInstanceInfo(getEnv("HOSTNAME", "localhost"),
		appId,
		getEnv("IP_ADDRESS", "0.0.0.0"),
		getEnvAsInt("SERVER_PORT", 9091), 30, false) //Create a new instance to register
	instance.Metadata = &eureka.MetaData{
		Map: make(map[string]string),
	}
	client.RegisterInstance(appId, instance)

	springConfigUrl := getEnv("SPRING_CLOUD_CONFIG_URI", "http://localhost:8888")
	springConfigUrl = fmt.Sprintf("%v?X-Encrypt-Key=%v", springConfigUrl, getEnv("X_ENCRYPT_KEY", "test"))
	configClient, err := cloudconfigclient.NewLocalClient(&http.Client{}, []string{springConfigUrl})

	if err != nil {
		panic(err)
	}

	profiles := getEnv("SPRING_PROFILES_ACTIVE", "dev")
	config, err := configClient.GetConfiguration(appId, []string{profiles})
	log.Infof("CONFIG Client = %+v", config)

	//JWT
	if strings.Contains(profiles, "prod") {
		bytes, err := ioutil.ReadFile(getEnv("PUBLIC_KEY_PATH", "/tmp/public.key"))
		if err != nil {
			panic(err)
		}
		pem, err := jwt.ParseRSAPublicKeyFromPEM(bytes)
		if err != nil {
			panic(err)
		}
		middleware.JWTWithConfig(middleware.JWTConfig{
			SigningKey:    pem,
			SigningMethod: "RS256",
		})
	} else {
		//echoRestApi.Use(middleware.JWT([]byte(config.PropertySources[0].Source(map[string]interface{})["foo"])))
	}

	// Middleware
	echoRestApi.Use(middleware.Recover(),
		middleware.Logger(),
		middleware.CSRF())

	// Routes
	echoRestApi.Logger.SetLevel(log.DEBUG)
	echoRestApi.GET("/posts", getAllPosts)
	echoRestApi.POST("/posts", createPost)
	echoRestApi.GET("/posts/:id", getPost)
	echoRestApi.PUT("/posts/:id", updatePost)
	echoRestApi.DELETE("/posts/:id", deletePost)
}

func main() {
	createDefaultPosts()

	// Start server
	echoRestApi.Logger.Fatal(echoRestApi.Start(":"+getEnv("SERVER_PORT", "9091")))
}