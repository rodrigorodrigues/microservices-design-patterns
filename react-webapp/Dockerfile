FROM node:10.15.3-slim
ARG SERVER_PORT

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

RUN apt-get update && apt-get install netcat-openbsd -y

RUN npm install
# If you are building your code for production
# RUN npm install --only=production

# Bundle app source
COPY . .

ENV JAVA_CMD="npm start"

ENV PORT=$SERVER_PORT

EXPOSE $SERVER_PORT

CMD [ "npm", "start" ]
