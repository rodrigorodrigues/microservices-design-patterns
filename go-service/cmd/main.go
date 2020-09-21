package main

import (
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

func main() {
	e := echo.New()
	e.Logger.SetLevel(log.DEBUG)

	// Middleware
	e.Use(middleware.Logger())
	e.Use(middleware.Recover())
	bytes, err := ioutil.ReadFile(os.Getenv("PUBLIC_KEY_PATH"))
	if err != nil {
		panic(err)
	}
	publicKey := strings.Replace(string(bytes), "-----BEGIN PUBLIC KEY-----", "", -1)
	publicKey = strings.Replace(publicKey, "-----END PUBLIC KEY-----", "", -1)
	e.Use(middleware.JWTWithConfig(middleware.JWTConfig{
		SigningKey:    []byte(strings.TrimSpace(publicKey)),
		SigningMethod: "RS256",
	}))

	createDefaultPosts()

	// Routes
	e.GET("/posts", getAllPosts)
	e.POST("/posts", createPost)
	e.GET("/posts/:id", getPost)
	e.PUT("/posts/:id", updatePost)
	e.DELETE("/posts/:id", deletePost)

	// Start server
	e.Logger.Fatal(e.Start(":1323"))
}