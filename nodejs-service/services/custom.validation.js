const CustomValidation = () => {
    const {VALIDATION} = require('../constants/message.constant');
    const log = require('../utils/log.message');

    return {
        // TODO add test this here
        messageValidation(errorObject) {
            log.logOnRoutes('========= Validator Error Message =========');
            log.logOnRoutes(errorObject ? errorObject.message : errorObject);
            log.logOnRoutes('error code =>',errorObject.code);
            log.logOnRoutes('Full stack trace =>', errorObject);
            log.logOnRoutes('End full stack trace =========');

            const message = VALIDATION[errorObject.code];

            return {
                message,
                name: errorObject.name,
                errors: errorObject.errors
            }
        }
    }
}

module.exports = CustomValidation();