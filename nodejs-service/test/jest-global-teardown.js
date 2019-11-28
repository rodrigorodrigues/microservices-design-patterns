const mongoMemory = require('./mongodb-memory');

module.exports = async () => {
    const mongoMemoryServer = await mongoMemory();
    mongoMemoryServer.stop();
};