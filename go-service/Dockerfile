FROM golang:1.15.2-alpine3.12
ARG PORT
ARG ENV
RUN mkdir /app
ADD . /app
WORKDIR /app
COPY go.mod .
## Add this go mod download command to pull in any dependencies
RUN go mod download
#RUN sed 's/github.com\/ericchiang.k8s.watch.versioned.Event/k8s.io.kubernetes.pkg.watch.versioned.Event/g' ${GOPATH}a/pkg/mod/github.com/ericchiang/k8s@v1.2.0/watch/versioned/generated.pb.go
RUN sed 's/github.com\/ericchiang.k8s.watch.versioned.Event/k8s.io.kubernetes.pkg.watch.versioned.Event/g' ${GOPATH}/pkg/mod/github.com/ericchiang/k8s@v1.2.0/watch/versioned/generated.pb.go
COPY . .
## Our project will now successfully build with the necessary go libraries included.
RUN go build -o main ./posts-api
ENV ENVIRONMENT=$ENV
EXPOSE $PORT
ENV JAVA_CMD="/app/main"
## Our start command which kicks off
## our newly created binary executable
CMD ["/app/main"]
