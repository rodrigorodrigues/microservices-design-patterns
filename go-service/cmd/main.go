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
	"gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type Post struct {
	ID   bson.ObjectId    `json:"id"`
	Name string `json:"name"`
	CreatedDate time.Time `json:"createdDate"`
	LastModifiedDate time.Time `json:"lastModifiedDate"`
	CreatedByUser string `json:"createdByUser"`
	LastModifiedByUser string `json:"lastModifiedByUser"`
}

var (
	echoRestApi = echo.New()
	session = connectMongo()
	collection = session.DB(getEnv("MONGODB_DATABASE", "docker")).C("Posts")
)

func connectMongo() *mgo.Session {
	// Mongodb
	session, err := mgo.Dial(getEnv("MONGODB_URI", ""))
	if err != nil {
		panic(err)
	}
	return session
}



//----------
// Handlers
//----------
func createDefaultPosts() {
	count, _ := collection.Count()
	if count > 0 {
		return
	}
	var posts = []Post{
		{
			ID: bson.NewObjectId(),
			Name:               "Golang",
			CreatedDate:        time.Time{},
			LastModifiedDate:   time.Time{},
			CreatedByUser:      "",
			LastModifiedByUser: "",
		},
		{
			ID: bson.NewObjectId(),
			Name:               "Test",
			CreatedDate:        time.Time{},
			LastModifiedDate:   time.Time{},
			CreatedByUser:      "",
			LastModifiedByUser: "",
		},
	}

	for _, post := range posts {
		err := collection.Insert(post)
		if err != nil {
			log.Fatal(err)
		}
	}
}

func getAllPosts(c echo.Context) error {
	var posts []Post
	if err := collection.Find(nil).All(&posts); err != nil {
		return err
	}
	return c.JSON(http.StatusOK, posts)
}

func createPost(c echo.Context) error {
	u := new(Post)
	if err := c.Bind(u); err != nil {
		return err
	}
	if err := collection.Insert(u); err != nil {
		return err
	}
	return c.JSON(http.StatusCreated, u)
}

func getPost(c echo.Context) error {
	id, _ := strconv.Atoi(c.Param("id"))
	post := Post{}
	if err := collection.Find(bson.M{"ID": id}).One(post); err != nil {
		return err
	}
	if post.ID == "" {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found id: %v", id))
	}
	return c.JSON(http.StatusOK, post)
}

func updatePost(c echo.Context) error {
	id, _ := strconv.Atoi(c.Param("id"))
	post := Post{}
	if err := collection.Find(bson.M{"ID": id}).One(post); err != nil {
		return err
	}
	if post.ID == "" {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found id: %v", id))
	}

	u := new(Post)
	if err := c.Bind(u); err != nil {
		return err
	}

	if err := collection.UpdateId(id, u); err != nil {
		return err
	}
	return c.JSON(http.StatusOK, u)
}

func deletePost(c echo.Context) error {
	id, _ := strconv.Atoi(c.Param("id"))
	if err := collection.RemoveId(id); err != nil {
		return err
	}
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
		secretKey := config.PropertySources[0].Source["security.oauth2.resource.jwt.keyValue"]
		if secretKey == nil {
			secretKey = config.PropertySources[1].Source["security.oauth2.resource.jwt.keyValue"]
		}
		if secretKey == nil {
			panic("Not found secretKey")
		}
		echoRestApi.Use(middleware.JWT([]byte(secretKey.(string))))
	}

	// Middleware
	echoRestApi.Use(middleware.Recover(),
		middleware.Logger())
		//middleware.CSRF())

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