const prepare = require('mocha-prepare');
import { MongoMemoryServer } from 'mongodb-memory-server';

console.log("Call prepare first!");
prepare(done => new MongoMemoryServer()
 .then(mongod => {
   const uri = await mongod.getConnectionString();
   const port = await mongod.getPort();
   const dbPath = await mongod.getDbPath();
   const dbName = await mongod.getDbName();
   console.log("uri", uri);

   process.env.MONGODB_URI = uri;
 }));