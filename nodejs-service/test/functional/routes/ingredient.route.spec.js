/**
 *  Legacy code, very messy
 */

const request = require("supertest");
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Ingredient} = require('../../../models/ingredient.model.js');
const {Category} = require('../../../models/category.model.js');
const {Recipe2} = require('../../../models/recipe2.model');
const {IngredientRecipeAttributes} = require('../../../models/ingredient.recipe.attributes.model.js');

const attributeName = 'global_attribute_name';

import sleep from 'await-sleep';

import 'jest-extended';

const Q = require('q');

describe("Ingredient", () => {
    beforeAll(async done => {
        console.log('Waiting for 5 secs');
    
        await sleep(5000);
    
        done();
    });

    afterAll(async done => {
        app.emit('close');

        done();
    });

    it("should get ingredient list", (done) => {

        return request(app)
            .get('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .expect((res) => {
                expect(res.body).toBeArray();
            })
            .end(done);
    });

    it('should load ingredient by passing an Id', (done) => {

        const ingredient = new Ingredient({
            name : 'Test Ingredient ' + new Date().getTime(),
        });

        return ingredient.save()
            .then((doc) => {
                request(app)
                    .get('/ingredient/' + doc._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(doc._id.toString());
                    }).end(done)
            }).catch(err => done(err));
    });

    it('should load ingredient/attributes', (done) => {

        return loadIdsToRecAttributes(done)
            .then( result => {
                if (!result) {
                    return done();
                }

                request(app)
                    .get('/ingredient/recipe/'+result.ingredientId+"/"+result.recipeId)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {

                        let ingredientRecipeAttributes = res.body;

                        expect(ingredientRecipeAttributes.ingredientId).toBe(result.ingredientId.toString());
                        expect(ingredientRecipeAttributes.recipeId).toBe(result.recipeId.toString());
                    }).end(done)

            });
    });

    it.skip("should save a ingredient", (done) => {

        return Category.findOne()
            .then( (docs) => {

                let categoryId = docs._id;

                findRecipe()
                    .then( recipeId => {

                        let ingredientCommand = {
                            ingredient : {
                                name : 'Test Ingredient ' + new Date().getTime(),
                                _creator : categoryId
                            },
                            ingredientRecipeAttributes : {
                                labelQuantity: 'kg',
                                recipeId: recipeId,
                            }
                        };

                        console.log(`Ingredient resource for save: ${JSON.stringify(ingredientCommand)}`)

                        request(app)
                            .post('/ingredient')
                            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                            .send(ingredientCommand)
                            .expect(201)
                            .end((err, res) => {

                                if (err) return done(err);

                                findIngredientTestIt(res, categoryId, recipeId)
                                    .then(findAttributesAndTestIt)
                            });

                    }).catch((reason) => {
                        done(reason);
                    });

                function findRecipe() {
                    let deferred = Q.defer();

                    Recipe2.findOne()
                        .then(recipes => {
                            deferred.resolve(recipes._id);

                        }).catch((reason) => {
                            deferred.reject(reason);
                            done(reason);
                        });

                    return deferred.promise;
                }

                function findIngredientTestIt(res, categoryId, recipeId) {

                    let deferred = Q.defer();

                    let id = res.body._id;

                    Ingredient.findOne({_id: id})
                        .then(ingred => {

                            expect(res.body).toIncludeKey('_id');
                            expect(ingred._creator).toEqual(categoryId);
                            expect(ingred.attributes.length > 0).toBe(true);

                            let result = {
                                recipeId : recipeId,
                                ingredientId: id
                            };

                            deferred.resolve(result);

                        }).catch((reason) => {
                            deferred.reject(reason);
                            done(reason);
                        });

                    return deferred.promise;
                }

                function findAttributesAndTestIt(result) {

                    IngredientRecipeAttributes
                        .findOne({ingredientId: result.ingredientId, recipeId: result.recipeId})
                            .then(attr => {

                                expect(attr.recipeId.toString()).toBe(result.recipeId.toString());

                                done();

                            }).catch((reason) => {
                                done(reason);
                            });
                }

            });
    });

    it.skip("should fail to save/post a ingredient", (done) => {

        return request(app)
            .post('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(400)
            .end((err, res) => {

                if(err)
                    return done(err);

                expect(res.body).toContainKeys(['message', 'errors', 'name']);

                Ingredient.findOne()
                    .then((docs) => {

                        expect(docs.length).toBe(2);
                        done();

                    }).catch((reason) => {
                        return reason
                    });
            });

    });

    it("should fail to save/post a duplicate ingredient", (done) => {

        return Ingredient.findOne()
            .then(ingredient => {

                getAttribute(ingredient);
            });

        function getAttribute(ingredient) {

            IngredientRecipeAttributes.findOne({name: ingredient.name})
                .then(attribute => {
                    if (!attribute) {
                        return done();
                    }

                    let result = {
                        attribute,
                        ingredient
                    };

                    sendRequest(result);
                });
        }


        function sendRequest(result) {

            let ingredient = result.ingredient;

            ingredient.save()
                .then(() => {

                    let param = {
                        ingredient,
                        ingredientRecipeAttributes: result.attribute
                    };

                    request(app)
                        .post('/ingredient')
                        .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                        .send(param)
                        .expect(400)
                        .expect((res) => {
                            expect(res.body.message).toInclude('duplicate key error')
                        }).end(done)
                });

        }

    });

    it("should fail to save/post missing category ID", (done) => {

        return request(app)
            .post('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .send({ingredient: {name : 'Test Ingredient'}})
            .expect(400)
            .expect((res) => {
                expect(res.body.message).toInclude('Missing category id')
            }).end(done)
    });


    it("should UPDATE a ingredient", (done) => {

       return Ingredient.findOne()
           .then(ingredient => {
               IngredientRecipeAttributes.findOne()
               .then(attribute => {
                   if (!attribute) {
                       return done();
                   }

                   let updatedName = "new Name from ingredient";
                   ingredient.name = updatedName;

                   let ingredientCommand = {
                       ingredient,
                       ingredientRecipeAttributes: attribute
                   };

                   request(app)
                       .put('/ingredient')
                       .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                       .send(ingredientCommand)
                       .expect(204)
                       .end((err, res) => {
                           if (err) return done(err);

                           Ingredient.findOne({_id: ingredient._id})
                               .then((doc) => {

                                   expect(doc.name).toBe(updatedName);
                                   done();

                               }).catch((reason) => {
                                    done(reason)
                                });

                       });
               });
           });
    });

    it.skip("should update ingredient and save recipes attributes", (done) => {

        //save first to make sure it will update

        return Ingredient.findOne()
            .then((ingredient) => {

                Recipe2.findOne()
                    .then((recs) => {

                        let recipeId = recs._id;

                        let ingredientRecipeCommand = {
                            labelQuantity: 'kg',
                            recipeId: recipeId,
                            ingredientId: ingredient._id
                        };

                        let ingCommand = {
                            ingredient: {
                                name : ingredient.name,
                                _id:ingredient._id,
                                _creator: ingredient._creator,
                                expiryDate: ingredient.expiryDate,
                                updateCheckDate: ingredient.updateCheckDate,
                                itemSelectedForShopping: ingredient.itemSelectedForShopping,
                                checkedInCartShopping: ingredient.checkedInCartShopping,
                            },

                            ingredientRecipeAttributes: ingredientRecipeCommand
                        };

                        request(app)
                            .put('/ingredient')
                            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                            .send(ingCommand)
                            .expect(204)
                            .end((err, res) => {

                                if (err) return done(err);

                                IngredientRecipeAttributes.findOne({recipeId: recipeId, ingredientId:ingredient._id})
                                    .then((doc) => {

                                        expect(doc.labelQuantity).toBe(ingredientRecipeCommand.labelQuantity);
                                        done();

                                    }).catch((reason) => {
                                        done(reason)
                                    });

                            });
                    })
                    .catch(done)
            }).catch(done);
    });

    it("should update attribute of a ingredient/recipe", done =>{

        return IngredientRecipeAttributes.findOne()
            .then(attribute => {

                if (!attribute) {
                    return done();
                }

                request(app)
                    .put('/ingredient/attribute')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({_id: attribute._id, itemSelectedForShopping: false})
                    .expect(204)
                    .end( (err, res) => {

                        if(err) return done(err);

                        IngredientRecipeAttributes.findOne({name: attributeName})
                            .then(attrUpdate => {

                                if (attrUpdate) {
                                    expect(attrUpdate.itemSelectedForShopping).toBe(false);
                                }
                                done();
                            });
                    });
            });
    });

    it("should update attribute list of a ingredient/recipe", done =>{

        return IngredientRecipeAttributes.findOne()
            .then(attribute => {

                if (!attribute) {
                    return done();
                }

                request(app)
                    .put('/ingredient/attribute/many')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send([{_id: attribute._id, itemSelectedForShopping: false}])
                    .expect(204)
                    .end( (err, res) => {

                        if(err) return done(err);

                        IngredientRecipeAttributes.findOne({name: attributeName})
                            .then(attrUpdate => {

                                if (attrUpdate) {
                                    expect(attrUpdate.itemSelectedForShopping).toBe(false);
                                }
                                done();
                            });
                    });
            });
    });

    it("should delete a ingredient", (done) => {

        return Ingredient.findOne()
            .then((doc) => {
                request(app)
                    .delete('/ingredient/'+doc._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(204)
                    .expect((res) => {

                        Ingredient.findOne()
                            .then((doc) => {
                                expect(doc).toBe(null);
                            }).catch((reason) => {
                                done(reason);
                            });
                    })
                    .end(done)
            });
    });

    function loadIdsToRecAttributes(done) {

        let promise = Q.defer();

        let result = {
            recipeId: '',
            ingredientId: ''
        }

        IngredientRecipeAttributes.findOne()
            .then( recs => {
                if (recs) {
                    result.recipeId = recs[0].recipeId;
                    result.ingredientId = recs[0].ingredientId

                    promise.resolve(result);
                } else {
                    done();
                }

            }).catch((reason) => {
                console.error(reason)
                promise.reject(reason);
            })

        return promise.promise;
    }
});
