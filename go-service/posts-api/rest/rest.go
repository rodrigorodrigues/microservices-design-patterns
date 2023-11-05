package rest

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/golang-jwt/jwt/v5"
	"github.com/labstack/echo/v4"
	"github.com/labstack/gommon/log"
	"go-service/posts-api/model"
	"go-service/posts-api/util"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"math"
	"net/http"
	"strconv"
	"strings"
	"time"
)

var (
	client     = connectMongo()
	collection = client.Database(util.GetEnv("MONGODB_DATABASE")).Collection("posts")
	httpClient = &http.Client{Timeout: 2 * time.Second}
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

// ----------
// Handlers
// ----------
func getAuthUser(c echo.Context) jwt.MapClaims {
	user := c.Get("user").(*jwt.Token)
	claims := user.Claims.(jwt.MapClaims)
	return claims
}

func HasAdminPermission(next echo.HandlerFunc) echo.HandlerFunc {
	return hasValidPermission(next, []string{"ROLE_ADMIN"})
}

func HasValidReadPermission(next echo.HandlerFunc) echo.HandlerFunc {
	permissions := []string{"ROLE_ADMIN", "ROLE_POSTS_READ", "ROLE_POSTS_CREATE", "ROLE_POSTS_SAVE", "ROLE_POSTS_DELETE", "SCOPE_openid"}
	return hasValidPermission(next, permissions)
}

func HasValidCreatePermission(next echo.HandlerFunc) echo.HandlerFunc {
	permissions := []string{"ROLE_ADMIN", "ROLE_POSTS_CREATE", "SCOPE_openid"}
	return hasValidPermission(next, permissions)
}

func HasValidSavePermission(next echo.HandlerFunc) echo.HandlerFunc {
	permissions := []string{"ROLE_ADMIN", "ROLE_POSTS_SAVE", "SCOPE_openid"}
	return hasValidPermission(next, permissions)
}

func HasValidDeletePermission(next echo.HandlerFunc) echo.HandlerFunc {
	permissions := []string{"ROLE_ADMIN", "ROLE_POSTS_DELETE", "SCOPE_openid"}
	return hasValidPermission(next, permissions)
}

func isAdmin(c echo.Context) bool {
	claimPermissions := getAuthUser(c)["authorities"].([]interface{})

	var result = false
	for _, x := range claimPermissions {
		if x == "ROLE_ADMIN" {
			result = true
			break
		}
	}

	return result
}

func hasValidPermission(next echo.HandlerFunc, permissions []string) echo.HandlerFunc {
	return func(c echo.Context) error {
		user := c.Get("user").(*jwt.Token)
		claims := user.Claims.(jwt.MapClaims)
		claimPermissions := claims["authorities"].([]interface{})

		var result = false
		for _, x := range claimPermissions {
			for _, y := range permissions {
				if x == y {
					result = true
					break
				}
			}
		}

		if result {
			return next(c)
		} else {
			return echo.ErrUnauthorized
		}
	}
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

func GetTasksApi(c echo.Context, id string) []model.TaskDto {
	req, err := http.NewRequest("GET", util.GetEnv("TASKS_API_URL")+"?postId="+id, nil)
	req.Header = c.Request().Header

	resp, err := httpClient.Do(req)
	if err != nil {
		log.Warn(fmt.Sprintf("Error Task Api Connection = %v", err))
		return nil
	}
	defer resp.Body.Close()
	var taskDto model.PageTaskDto
	err = json.NewDecoder(resp.Body).Decode(&taskDto)
	if err != nil {
		log.Warn(fmt.Sprintf("Error Read Task Api = %v", err))
		return nil
	}
	return taskDto.Task
}

func GetAllPostsByName(c echo.Context) error {
	log.Info(fmt.Sprintf("Searching by GetAllPostsByName = %v", c.QueryString()))
	return getAllPosts(c, false)
}

func GetAllPosts(c echo.Context) error {
	return getAllPosts(c, true)
}

func getAllPosts(c echo.Context, callTaskApi bool) error {
	traceId := c.Request().Header.Get("uber-trace-id")
	log.Info(fmt.Sprintf("Processing getAllPosts: uber-trace-id = %v", traceId))
	page := 0
	size := 10
	if c := c.QueryParam("page"); c != "" {
		page, _ = strconv.Atoi(c)
	}
	if c := c.QueryParam("size"); c != "" {
		size, _ = strconv.Atoi(c)
	}
	var searchByText string
	var searchByPersonId string
	if !callTaskApi || strings.HasPrefix(c.QueryString(), "&") {
		searchByText = c.QueryString()
		if callTaskApi {
			searchByText = searchByText[0:strings.Index(searchByText, "&")]
		}
	} else if strings.Contains(c.QueryString(), "personId=") {
		searchByPersonId = c.QueryParam("personId")
	}
	log.Info(fmt.Sprintf("QueryString = %v", searchByText))
	log.Info(fmt.Sprintf("SearchByPersonId = %v", searchByPersonId))

	pageInt64 := int64(page)
	sizeInt64 := int64(size)
	ctx := context.TODO()
	pagination := new(model.Pagination)
	pagination.Page = pageInt64
	pagination.Size = sizeInt64
	var posts []model.PostDto
	var cur *mongo.Cursor
	opts := options.Find().SetSort(bson.D{{"createdDate", 1}})
	if callTaskApi {
		opts.SetSkip(pageInt64).SetLimit(sizeInt64)
	}

	if isAdmin(c) {
		filter := bson.M{}
		if searchByText != "" {
			filter = bson.M{
				"name": bson.D{{"$all", bson.A{searchByText}}},
			}
		}
		if searchByPersonId != "" {
			filter = bson.M{
				"personid": bson.D{{"$all", bson.A{searchByPersonId}}},
			}
		}
		count, err2 := collection.CountDocuments(ctx, filter)
		if err2 != nil {
			return err2
		}
		pagination.TotalElements = count
		cursor, err := collection.Find(ctx, filter, opts)
		if err != nil {
			return err
		}
		cur = cursor
	} else {
		createdByUser := getAuthUser(c)["sub"].(string)
		filter := bson.M{"createdbyuser": createdByUser}
		if searchByText != "" {
			filter = bson.M{
				"createdbyuser": createdByUser,
				"name":          bson.D{{"$all", bson.A{searchByText}}},
			}
		}
		if searchByPersonId != "" {
			filter = bson.M{
				"personid": bson.D{{"$all", bson.A{searchByPersonId}}},
			}
		}
		count, err2 := collection.CountDocuments(ctx, filter)
		if err2 != nil {
			return err2
		}
		pagination.TotalElements = count

		cursor, err := collection.Find(ctx, filter, opts)

		if err != nil {
			return err
		}
		cur = cursor
	}

	for cur.Next(ctx) {
		var post model.Post
		if err := cur.Decode(&post); err != nil {
			return err
		}
		if post.LastModifiedDate != nil && post.LastModifiedDate.IsZero() {
			post.LastModifiedDate = nil
		}
		postDto := model.PostDto{
			ID:                 post.ID.Hex(),
			Name:               post.Name,
			CreatedDate:        post.CreatedDate.Format(time.DateTime),
			CreatedByUser:      post.CreatedByUser,
			LastModifiedByUser: post.LastModifiedByUser,
		}
		if callTaskApi && util.GetEnvAsBool("CALL_TASK_API") {
			postDto.Tasks = GetTasksApi(c, postDto.ID)
		}
		if post.LastModifiedDate != nil && !post.LastModifiedDate.IsZero() {
			postDto.LastModifiedDate = post.LastModifiedDate.Format(time.DateTime)
		}
		posts = append(posts, postDto)
	}
	pagination.Post = append(posts)
	if pagination.Post == nil {
		pagination.Post = []model.PostDto{}
	}

	if pagination.TotalPages > sizeInt64 {
		pagination.TotalPages = int64(math.Ceil(float64(pagination.TotalElements / sizeInt64)))
	} else {
		pagination.TotalPages = 1
	}

	return c.JSON(http.StatusOK, pagination)
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

// GetPostById   godoc
// @Summary      Show an account
// @Description  get string by ID
// @Tags         accounts
// @Accept       json
// @Produce      json
// @Param        id   path      int  true  "Account ID"
// @Success      200  {object}  model.Post
// @Failure      400  {object}  string
// @Failure      404  {object}  string
// @Failure      500  {object}  string
// @Router       /posts/{id} [get]
func GetPostById(c echo.Context) error {
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
	if err := collection.FindOne(context.TODO(), filter).Decode(&post); err != nil {
		return err
	}
	user := getAuthUser(c)["sub"].(string)
	if post.ID.IsZero() {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	} else if isAdmin(c) || post.CreatedByUser == user {
		return c.JSON(http.StatusOK, post)
	} else {
		return echo.NewHTTPError(http.StatusForbidden, fmt.Sprintf("User(%v) does not have access to this resource", user))
	}
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

	u := new(model.PostDto)
	if err := c.Bind(u); err != nil {
		return err
	}
	if err := c.Validate(u); err != nil {
		return err
	}

	var t = time.Now()
	post.Name = u.Name
	post.LastModifiedDate = &t
	post.LastModifiedByUser = getAuthUser(c)["sub"].(string)
	update := bson.M{
		"$set": post,
	}
	if _, err := collection.UpdateOne(ctx, filter, update); err != nil {
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

	post := model.Post{}
	filter := bson.M{"_id": objId}
	user := getAuthUser(c)["sub"].(string)

	if err := collection.FindOne(context.TODO(), filter).Decode(&post); err != nil {
		return echo.NewHTTPError(http.StatusNotFound, fmt.Sprintf("Not found ID: %v", id))
	} else if isAdmin(c) || post.CreatedByUser == user {
		if _, err := collection.DeleteOne(context.TODO(), filter); err != nil {
			return err
		}
		return c.NoContent(http.StatusNoContent)
	} else {
		return echo.NewHTTPError(http.StatusForbidden, fmt.Sprintf("User(%v) does not have access to this resource", user))
	}
}
