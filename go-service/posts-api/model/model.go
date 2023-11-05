package model

import (
	"github.com/go-playground/validator/v10"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type (
	Pagination struct {
		Page          int64     `json:"page"`
		Size          int64     `json:"size"`
		TotalPages    int64     `json:"totalPages"`
		TotalElements int64     `json:"totalElements"`
		Post          []PostDto `json:"content"`
	}

	PageTaskDto struct {
		Page          int64     `json:"page"`
		Size          int64     `json:"size"`
		TotalPages    int64     `json:"totalPages"`
		TotalElements int64     `json:"totalElements"`
		Task          []TaskDto `json:"content"`
	}

	PostDto struct {
		ID                 string    `json:"id,omitempty" form:"id" query:"id"`
		Name               string    `json:"name,omitempty" form:"name" query:"name" validate:"required,gte=5,lte=255"`
		CreatedDate        string    `json:"createdDate,omitempty" form:"createDate" query:"createDate"`
		LastModifiedDate   string    `json:"lastModifiedDate,omitempty"`
		CreatedByUser      string    `json:"createdByUser,omitempty"`
		LastModifiedByUser string    `json:"lastModifiedByUser,omitempty"`
		Tasks              []TaskDto `json:"tasks,omitempty"`
		PersonId           string    `json:"personId,omitempty" form:"personId" query:"personId"`
	}

	Post struct {
		ID                 primitive.ObjectID `bson:"_id" json:"id,omitempty" form:"id" query:"id"`
		Name               string             `json:"name,omitempty" form:"name" query:"name" validate:"required,gte=5,lte=255"`
		CreatedDate        time.Time          `json:"createdDate,omitempty" form:"createDate" query:"createDate"`
		LastModifiedDate   *time.Time         `json:"lastModifiedDate,omitempty"`
		CreatedByUser      string             `json:"createdByUser,omitempty"`
		LastModifiedByUser string             `json:"lastModifiedByUser,omitempty"`
		Tasks              []TaskDto          `json:"tasks,omitempty"`
		PersonId           string             `json:"personId,omitempty" form:"personId" query:"personId"`
	}

	TaskDto struct {
		ID                 primitive.ObjectID `bson:"_id" json:"id,omitempty" form:"id" query:"id"`
		Name               string             `json:"name,omitempty" form:"name" query:"name" validate:"required,gte=5,lte=255"`
		CreatedDate        time.Time          `json:"createdDate,omitempty" form:"createDate" query:"createDate"`
		LastModifiedDate   time.Time          `json:"lastModifiedDate,omitempty"`
		CreatedByUser      string             `json:"createdByUser,omitempty"`
		LastModifiedByUser string             `json:"lastModifiedByUser,omitempty"`
	}

	JsonResponse struct {
		Status string `json:"status"`
	}

	CustomValidator struct {
		Validator *validator.Validate
	}
)

func (cv *CustomValidator) Validate(i interface{}) error {
	return cv.Validator.Struct(i)
}
