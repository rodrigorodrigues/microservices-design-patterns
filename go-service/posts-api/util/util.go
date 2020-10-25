package util

import (
	"fmt"
	"github.com/joho/godotenv"
	"os"
	"strconv"
)

var loadEnvFlag = true

func GetEnv(key string) string {
	if loadEnvFlag {
		loadEnv()
		loadEnvFlag = false
	}
	value := os.Getenv(key)
	if "" == value {
		valueEnv, exists := os.LookupEnv(key)
		if !exists {
			panic("Not found variable: " + key)
		}
		value = valueEnv
	}
	fmt.Print(fmt.Sprintf("Env = %+v\tvalue = %v\n", key, value))
	return value
}

func GetEnvAsInt(key string) int {
	value, err := strconv.Atoi(GetEnv(key))
	if err != nil {
		panic("Not found variable: " + key)
	}

	return value
}

// loads values from .env into the system
func loadEnv() {
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