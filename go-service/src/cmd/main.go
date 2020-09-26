package main

import (
	"context"
	"fmt"
	"github.com/ArthurHlt/go-eureka-client/eureka"
	"github.com/Piszmog/cloudconfigclient"
	"github.com/dgrijalva/jwt-go"
	"github.com/joho/godotenv"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/labstack/gommon/log"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type Post struct {
	ID                 primitive.ObjectID    `bson:"_id" json:"id" form:"id" query:"id"`
	Name               string    `json:"name" form:"name" query:"name"`
	CreatedDate        time.Time `json:"createdDate" form:"createDate" query:"createDate"`
	LastModifiedDate   time.Time `json:"lastModifiedDate"`
	CreatedByUser      string    `json:"createdByUser"`
	LastModifiedByUser string    `json:"lastModifiedByUser"`
}

var (
	echoRestApi = echo.New()
	client      = connectMongo()
	collection  = client.Database(getEnv("MONGODB_DATABASE", "docker")).Collection("posts")
)

func connectMongo() *mongo.Client {
	// Mongodb
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(getEnv("MONGODB_URI", "mongodb://localhost:27017")))
	if err != nil {
		panic(err)
	}
	return client
}



//----------
// Handlers
//----------
func createDefaultPosts() {
	count, _ := collection.CountDocuments(context.TODO(), bson.D{})
	if count > 0 {
		return
	}
	var posts = []Post{
		{
			ID: primitive.NewObjectID(),
			Name:               "Golang",
			CreatedDate:        time.Now(),
			CreatedByUser:      "default@admin.com",
		},
		{
			ID: primitive.NewObjectID(),
			Name:               "Test",
			CreatedDate:        time.Now(),
			CreatedByUser:      "default@admin.com",
		},
	}

	for _, post := range posts {
		_, err := collection.InsertOne(context.TODO(), post)
		if err != nil {
			log.Fatal(err)
		}
	}
}

func getAllPosts(c echo.Context) error {
	ctx := context.TODO()
	cur, err := collection.Find(ctx, bson.D{})
	if err != nil {
		return err
	}
	var posts []*Post
	for cur.Next(ctx) {
		var post Post
		if err := cur.Decode(&post); err != nil {
			return err
		}
		posts = append(posts, &post)
	}
	return c.JSON(http.StatusOK, posts)
}

func createPost(c echo.Context) error {
	u := new(Post)
	if err := c.Bind(u); err != nil {
		return err
	}
	u.CreatedDate = time.Now()
	u.LastModifiedByUser = ""
	claims := getAuthUser(c)
	u.CreatedByUser = claims["sub"].(string)
	if _, err := collection.InsertOne(context.TODO(), u); err != nil {
		return err
	}
	return c.JSON(http.StatusCreated, u)
}

func getAuthUser(c echo.Context) jwt.MapClaims {
	user := c.Get("user").(*jwt.Token)
	claims := user.Claims.(jwt.MapClaims)
	return claims
}

func getPost(c echo.Context) error {
	id := c.Param("id")
	post := Post{}
	filter := bson.D{{"_id", id}}
	if err := collection.FindOne(context.TODO(), filter).Decode(&post); err != nil {
		return err
	}
	if post.ID.IsZero() {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	}
	return c.JSON(http.StatusOK, post)
}

func updatePost(c echo.Context) error {
	id := c.Param("id")
	post := Post{}
	ctx := context.TODO()
	filter := bson.D{{"_id", id}}
	if err := collection.FindOne(ctx, filter).Decode(&post); err != nil {
		return err
	}
	if post.ID.IsZero() {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	}

	u := new(Post)
	if err := c.Bind(u); err != nil {
		return err
	}

	u.LastModifiedDate = time.Now()
	u.LastModifiedByUser = getAuthUser(c)["sub"].(string)
	if _, err := collection.UpdateOne(ctx, filter, u); err != nil {
		return err
	}
	return c.JSON(http.StatusOK, u)
}

func deletePost(c echo.Context) error {
	id := c.Param("ID")
	if _, err := collection.DeleteOne(context.TODO(), id); err != nil {
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