const mongoose = require('mongoose');
import { MongoMemoryServer } from 'mongodb-memory-server-core';

let mongoServer = null;
module.exports = async () => {
  if (mongoServer !== null) {
    return mongoServer;
  }

  mongoose.Promise = Promise;

  mongoServer = new MongoMemoryServer({
    binary :{
      systemBinary: 'C:/Users/46456/.embedmongo/win32/mongodb-win32-x86_64-2008plus-ssl-4.0.3/bin/mongod.exe'
    }
  });
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

          fs.writeFileSync(".env", `MONGODB_URI = ${mongoUri}`);

          console.log(`Connected to MongoDb in Memory: ${mongoUri} and created temp .env file with mongo uri`);
        }
      });
    });

    return mongoServer;

};