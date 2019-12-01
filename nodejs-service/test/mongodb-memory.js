const mongoose = require('mongoose');
import { MongoMemoryServer } from 'mongodb-memory-server-core';

let mongoServer = null;
module.exports = async () => {
  if (mongoServer !== null) {
    return mongoServer;
  }

  mongoose.Promise = Promise;

  mongoServer = new MongoMemoryServer();
  await mongoServer
    .getConnectionString()
    .then((mongoUri) => {
      const opts = {
        autoReconnect: true,
        reconnectTries: Number.MAX_VALUE,
        reconnectInterval: 1000
      };
      console.log(`Using MongoDb Uri: ${mongoUri}`);
      return mongoose.connect(mongoUri, opts, (err) => {
        if (err) {
          done(err);
        } else {
          var fs = require('fs');

          const secretKey = 'YTMwOTIwODE1MGMzOGExM2E4NDc5ZjhjMmQwMTdkNDJlZWZkOTE0YTMwNWUxMTgxMTFhZTI1ZDI3M2QyMTRmMjI5Yzg0ODBjYTUxYjVkY2I5ZmY0YmRkMzBlZjRjNDM2Y2NiYzhlZjQ0ODRjMWZlNzVmZjdjM2JiMjdkMjdmMjk=';

          fs.writeFileSync(".env", `MONGODB_URI = ${mongoUri}\nSECRET_TOKEN = ${secretKey}`);

          console.log(`Connected to MongoDb in Memory: ${mongoUri} and created temp .env file with mongo uri`);
        }
      });
    });

    return mongoServer;

};