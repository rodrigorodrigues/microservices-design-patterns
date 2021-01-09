FROM node:12-buster-slim as builder
ARG PORT

# Create app directory
WORKDIR /usr/src/app

ENV PATH /usr/src/app/node_modules/.bin:$PATH

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

#COPY node_modules/ ./

RUN apt-get update && apt-get install npm nodejs -y && apt-get install netcat-openbsd -y

RUN npm install
RUN npm install react-scripts -g

# Bundle app source
COPY . .

RUN npm run build

### STAGE 2: Production Environment ###
FROM nginx:stable-alpine
ARG PORT

ENV PORT=$PORT


COPY --from=builder /usr/src/app/build /usr/share/nginx/html
COPY --from=builder /usr/src/app/nginx.conf /etc/nginx/conf.d/default.conf

RUN echo "PORT = $PORT"

RUN sed -i 's/PORT/'"$PORT"'/g' /etc/nginx/conf.d/default.conf

RUN echo "daemon off;" >> /etc/nginx/nginx.conf

EXPOSE $PORT

ENV JAVA_CMD="nginx-debug"
CMD ["nginx-debug"]