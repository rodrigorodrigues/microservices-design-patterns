package main

import (
	"context"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"github.com/go-playground/validator/v10"
	"github.com/hashicorp/consul/api"
	"github.com/joho/godotenv"
	"github.com/labstack/echo-contrib/jaegertracing"
	"github.com/labstack/echo-contrib/prometheus"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/labstack/gommon/log"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type (
	Post struct {
		ID                 primitive.ObjectID    `bson:"_id" json:"id,omitempty" form:"id" query:"id"`
		Name               string    `json:"name,omitempty" form:"name" query:"name" validate:"required,gte=5,lte=255"`
		CreatedDate        time.Time `json:"createdDate,omitempty" form:"createDate" query:"createDate"`
		LastModifiedDate   time.Time `json:"lastModifiedDate,omitempty"`
		CreatedByUser      string    `json:"createdByUser,omitempty"`
		LastModifiedByUser string    `json:"lastModifiedByUser,omitempty"`
	}

	JsonResponse struct {
		Status	string	 `json:"status"`
	}

	CustomValidator struct {
		validator *validator.Validate
	}
)

var (
	loadEnvFlag = true
	e           = echo.New()
	client      = connectMongo()
	collection  = client.Database(getEnv("MONGODB_DATABASE")).Collection("posts")
)

func connectMongo() *mongo.Client {
	// Mongodb
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(getEnv("MONGODB_URI")))
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
	if err := c.Validate(u); err != nil {
		return err
	}
	u.ID = primitive.NewObjectID()
	u.CreatedDate = time.Now()
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
	if "" == id {
		panic("Id is mandatory!")
	}
	objId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		panic(err)
	}
	post := Post{}
	filter := bson.M{"_id": objId}
	opts := options.FindOne().SetSort(bson.D{{"createdDate", 1}})
	if err := collection.FindOne(context.TODO(), filter, opts).Decode(&post); err != nil {
		return err
	}
	if post.ID.IsZero() {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	}
	return c.JSON(http.StatusOK, post)
}

func updatePost(c echo.Context) error {
	id := c.Param("id")
	if "" == id {
		panic("Id is mandatory!")
	}
	objId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		panic(err)
	}
	post := Post{}
	ctx := context.TODO()
	filter := bson.M{"_id": objId}
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
	if err := c.Validate(u); err != nil {
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
	id := c.Param("id")
	objId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		panic(err)
	}
	filter := bson.M{"_id": objId}
	if _, err := collection.DeleteOne(context.TODO(), filter); err != nil {
		return err
	}
	return c.NoContent(http.StatusNoContent)
}

func healthCheck(c echo.Context) error  {
	json := JsonResponse{Status: "OK"}
	return c.JSON(http.StatusOK, json)
}

func getEnv(key string) string {
	if loadEnvFlag {
		loadEnv()
		loadEnvFlag = false
	}
	value := os.Getenv(key)
	if "" == value {
		valueEnv, exists := os.LookupEnv(key)
		if  !exists {
			panic("Not found variable: " + key)
		}
		value = valueEnv
	}
	fmt.Print(fmt.Sprintf("Env = %+v\tvalue = %v\n", key, value))
	return value
}

func getEnvAsInt(key string) int {
	value, err := strconv.Atoi(getEnv(key))
	if err != nil {
		panic("Not found variable: " + key)
	}

	return value
}

// loads values from .env into the system
func loadEnv()  {
	env := ".env"
	environment := os.Getenv("ENVIRONMENT")
	if environment != "" {
		env += "." + environment
	}

	fmt.Print(fmt.Sprintf("Env = %+v\n", env))
	if err := godotenv.Load(env); err != nil {
		panic(err)
	}
}

func (cv *CustomValidator) Validate(i interface{}) error {
	return cv.validator.Struct(i)
}

func init()  {
	client := processConsulClient()

	middlewareObj := processJwt(client)

	processRestApi(middlewareObj)
}

func processConsulClient() *api.Client {
	client, err := api.NewClient(api.DefaultConfig())
	if err != nil {
		panic(err)
	}
	registration := &api.AgentServiceRegistration{
		ID:   getEnv("APP_ID"),
		Name: getEnv("APP_ID"),
		Port: getEnvAsInt("SERVER_PORT"),
	}
	if err := client.Agent().ServiceRegister(registration); err != nil {
		panic(err)
	}
	return client
}

func processJwt(config *api.Client) echo.MiddlewareFunc {
	//JWT
	middlewareObj := middleware.JWT([]byte("secret"))
	profile := getEnv("SPRING_PROFILES_ACTIVE")
	if strings.Contains(profile, "prod") {
		bytes, err := ioutil.ReadFile(getEnv("PUBLIC_KEY_PATH"))
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
		
		key := yamlMap["security"].(map[interface{}]interface{})["oauth2"].(map[interface{}]interface{})["resource"].(map[interface{}]interface{})["jwt"].(map[interface{}]interface{})["keyValue"]
		if key == nil {
			panic("Not found jwt")
		}
		middlewareObj = middleware.JWT(key)
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
	e.GET("/api/posts", getAllPosts, middlewareObj)
	e.POST("/api/posts", createPost, middlewareObj)
	e.GET("/api/posts/:id", getPost, middlewareObj)
	e.PUT("/api/posts/:id", updatePost, middlewareObj)
	e.DELETE("/api/posts/:id", deletePost, middlewareObj)
	e.GET("/actuator/info", healthCheck)
	e.GET("/actuator/health", healthCheck)

	e.Validator = &CustomValidator{validator: validator.New()}

	// Prometheus
	p := prometheus.NewPrometheus("echo", urlSkipper)
	p.MetricsPath = "/actuator/metrics"
	p.Use(e)
}

func main() {
	createDefaultPosts()

	// Enable tracing middleware
	c := jaegertracing.New(e, urlSkipper)
	defer c.Close()

	// Start server
	e.Logger.Fatal(e.Start(":"+getEnv("SERVER_PORT")))
}
