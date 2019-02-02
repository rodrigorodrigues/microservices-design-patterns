const CustomValidation = () => {
    const {VALIDATION} = require('../constants/message.constant');
    const log = require('../utils/log.message');

    return {
        // TODO add test this here
        messageValidation(errorObject) {
            log.logExceptOnTest('========= Validator Error Message =========');
            log.logExceptOnTest(errorObject ? errorObject.message : errorObject);
            log.logExceptOnTest('error code =>',errorObject.code);
            log.logExceptOnTest('Full stack trace =>', errorObject);
            log.logExceptOnTest('End full stack trace =========');

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