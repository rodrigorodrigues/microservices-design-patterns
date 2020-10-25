package model

import (
	"github.com/go-playground/validator/v10"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type (
	Post struct {
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
