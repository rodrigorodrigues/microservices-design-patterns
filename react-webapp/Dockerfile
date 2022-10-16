FROM node:12-buster-slim as builder
ARG PORT
ARG NODE_ENV

# Create app directory
WORKDIR /usr/src/app

ENV PATH /usr/src/app/node_modules/.bin:$PATH
ENV NODE_ENV=${NODE_ENV}

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

RUN if ["$NODE_ENV" = "development"]; then mv .env .env.production & echo .env.production; fi

RUN npm run build

### STAGE 2: Production Environment ###
FROM nginx:stable-alpine
ARG PORT
ARG NODE_ENV

ENV PORT=${PORT}
ENV NODE_ENV=${NODE_ENV}

COPY --from=builder /usr/src/app/build /usr/share/nginx/html
COPY --from=builder /usr/src/app/nginx.conf /etc/nginx/conf.d/default.conf

RUN echo "PORT = $PORT"

RUN echo "NODE_ENV = $NODE_ENV"

RUN sed -i 's/PORT/'"$PORT"'/g' /etc/nginx/conf.d/default.conf

RUN echo "daemon off;" >> /etc/nginx/nginx.conf

EXPOSE $PORT

ENV JAVA_CMD="nginx-debug"
CMD ["nginx-debug"]