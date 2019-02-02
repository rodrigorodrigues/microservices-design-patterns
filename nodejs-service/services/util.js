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
    }
}
module.exports = UtilService;