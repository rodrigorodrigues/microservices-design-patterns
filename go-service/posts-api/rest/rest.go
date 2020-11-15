package rest

import (
	"context"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"github.com/labstack/echo/v4"
	"go-service/posts-api/model"
	"go-service/posts-api/util"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"net/http"
	"time"
)

var (
	client      = connectMongo()
	collection  = client.Database(util.GetEnv("MONGODB_DATABASE")).Collection("posts")
)

func connectMongo() *mongo.Client {
	// Mongodb
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(util.GetEnv("MONGODB_URI")))
	if err != nil {
		panic(err)
	}
	return client
}


//----------
// Handlers
//----------
func getAuthUser(c echo.Context) jwt.MapClaims {
	user := c.Get("user").(*jwt.Token)
	claims := user.Claims.(jwt.MapClaims)
	return claims
}

func CreateDefaultPosts() {
	count, _ := collection.CountDocuments(context.TODO(), bson.D{})
	if count > 0 {
		return
	}
	var posts = []model.Post{
		{
			ID:            primitive.NewObjectID(),
			Name:          "Golang",
			CreatedDate:   time.Now(),
			CreatedByUser: "default@admin.com",
		},
		{
			ID:            primitive.NewObjectID(),
			Name:          "Test",
			CreatedDate:   time.Now(),
			CreatedByUser: "default@admin.com",
		},
	}

	for _, post := range posts {
		_, err := collection.InsertOne(context.TODO(), post)
		if err != nil {
			panic(err)
		}
	}
}

func GetAllPosts(c echo.Context) error {
	ctx := context.TODO()
	cur, err := collection.Find(ctx, bson.D{})
	if err != nil {
		return err
	}
	var posts []*model.Post
	for cur.Next(ctx) {
		var post model.Post
		if err := cur.Decode(&post); err != nil {
			return err
		}
		posts = append(posts, &post)
	}
	return c.JSON(http.StatusOK, posts)
}

func CreatePost(c echo.Context) error {
	u := new(model.Post)
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

func GetPost(c echo.Context) error {
	id := c.Param("id")
	if "" == id {
		panic("Id is mandatory!")
	}
	objId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		panic(err)
	}
	post := model.Post{}
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

func UpdatePost(c echo.Context) error {
	id := c.Param("id")
	if "" == id {
		panic("Id is mandatory!")
	}
	objId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		panic(err)
	}
	post := model.Post{}
	ctx := context.TODO()
	filter := bson.M{"_id": objId}
	if err := collection.FindOne(ctx, filter).Decode(&post); err != nil {
		return err
	}
	if post.ID.IsZero() {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	}

	u := new(model.Post)
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

func DeletePost(c echo.Context) error {
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