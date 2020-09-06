const log = require('../utils/log.message');

const UtilService = {
    // TODO need to add these fields 
    addProps(paramObject) {
        const result = Object.assign({}, paramObject);
        result.insertDate = new Date();
        result.updateDate = new Date();
        return result;
    },
    sortAllProductCategory(categories) {
        // problem sorting with mongoose
        const result = categories.map(category => {
            category.products = 
                category.products
                    .sort( (prodA, prodB) => prodA.name > prodB.name ? 1 : -1)
            return category;        
        })
        return result;
    },
    handleResponse(response, doc, status) {
        log.logOnRoutes("Response Route ----");
        log.logOnRoutes("Response doc", doc);
        log.logOnRoutes("Response status", status);
    
        return response
            .status(status)
            .json(doc)
            .end();
    },
    wmHandleError(res, reason) {
        log.logOnRoutes("Response Bad Request Route ----");
        log.logOnRoutes("Response reason", reason);

        var errorResponse = {
            message : reason.message,
            name: reason.name,
            errors: reason.errors
        };
    
        return res
            .status(400) //bad request
            .send(errorResponse)
            .end();
    }
}
module.exports = UtilService;