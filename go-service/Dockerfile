FROM golang:1.15.2-alpine3.12
ARG PORT
RUN mkdir /app
ADD . /app
WORKDIR /app
## Add this go mod download command to pull in any dependencies
RUN go mod download
## Our project will now successfully build with the necessary go libraries included.
RUN go build -o main .
ENV ENVIRONMENT=docker
EXPOSE $PORT
## Our start command which kicks off
## our newly created binary executable
CMD ["/app/main"]
