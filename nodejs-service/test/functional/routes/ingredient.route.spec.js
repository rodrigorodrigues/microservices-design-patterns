/**
 *  Legacy code, very messy
 */

const request = require("supertest");
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Ingredient} = require('../../../models/ingredient.model.js');
const {Category} = require('../../../models/category.model.js');
const {Recipe} = require('../../../models/recipe.model.js');
const {IngredientRecipeAttributes} = require('../../../models/ingredient.recipe.attributes.model.js');

const categoryNames = [
    "from_Ingr_categoryTest0",
    "from_Ingr_categoryTest1"
];

const ingredientNames = [
    "from_Ingr_ingredietTest0",
    "from_Ingr_ingredietTest1",
    "from_Ingr_ingredietTest2",
    "from_Ingr_ingredietTest3",
    "from_Ingr_ingredietTest4",
    "from_Ingr_ingredietTest5",
    "from_Ingr_ingredietTest6",
    "from_Ingr_ingredietTest7",
    "from_Ingr_ingredietTest8"
];

const categories = [
    {name : categoryNames[0]},
    {name : categoryNames[1]}
];

const recipeName = 'global_recipe_from_ingredients';

const attributeName = 'global_attribute_name';

const nock = require('nock')

import sleep from 'await-sleep';

import 'jest-extended';

nock('http://localhost:8761')
    .post('/')
    .reply(200, {
        'status' : 'ok'
    });

nock('http://localhost:8888')
  .get('/week-menu-api/dev%2C%3FX-Encrypt-Key%3Db7fc7cec8e7aab24648723258da87a8d09ad7cef7b0a2842738884496a9fbb53')
  .reply(200, {
    response: {
      configuration: {
          jwt: {
            'base64-secret': 'Test'
          }
      }
    },
  });

const Q = require('q');

describe("Ingredient", () => {
    beforeAll(async done => {
        console.log('Waiting for 5 secs');
    
        await sleep(5000);
    
        done();
    });

    it("should get ingredient list", async done => {

        await request(app)
            .get('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .expect((res) => {
                expect(res.body.length > 1).toBe(true);
            })
            .end(done);
    });

    it('should load ingredient by passing an Id', async done => {

        const ingredient = new Ingredient({
            name : ingredientNames[2],
        });

        await ingredient.save()
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

    it('should load ingredient/attributes', async done => {

        await loadIdsToRecAttributes()
            .then( result => {

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

    it("should save a ingredient", async done => {

        await Category.find({})
            .then( (docs) => {

                let categoryId = docs[0]._id;

                findRecipe()
                    .then( recipeId => {

                        let ingredientCommand = {
                            ingredient : {
                                name : ingredientNames[2],
                                _creator : categoryId
                            },
                            ingredientRecipeAttributes : {
                                labelQuantity: 'kg',
                                recipeId: recipeId,
                            }
                        };

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

                    Recipe.find({})
                        .then(recipes => {

                            let recipeId = recipes[0]._id;

                            deferred.resolve(recipeId);

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

    it("should fail to save/post a ingredient", async done => {

        await request(app)
            .post('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(400)
            .end((err, res) => {

                if(err)
                    return done(err);

                expect(res.body).toIncludeKeys(['message', 'errors', 'name']);

                Ingredient.find({})
                    .then((docs) => {

                        expect(docs.length).toBe(2);
                        done();

                    }).catch((reason) => {
                        return reason
                    });
            });

    });

    it("should fail to save/post a duplicate ingredient", (done) => {

        Ingredient.findOne({name: ingredientNames[1]})
            .then(ingredient => {

                getAttribute(ingredient);
            });

        function getAttribute(ingredient) {

            IngredientRecipeAttributes.findOne({name: attributeName})
                .then(attribute => {
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

        request(app)
            .post('/ingredient')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .send({ingredient: {name : ingredientNames[4]}})
            .expect(400)
            .expect((res) => {
                expect(res.body.message).toInclude('Missing category id')
            }).end(done)
    });


    it("should UPDATE a ingredient", (done) => {

       Ingredient.findOne({name: ingredientNames[1]})
           .then(ingredient => {
               IngredientRecipeAttributes.findOne({name: attributeName})
               .then(attribute => {

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

    it("should update ingredient and save recipes attributes", (done) => {

        //save first to make sure it will update

        Ingredient.find({})
            .then((docs) => {

                let ingredient = docs[0];

                Recipe.find({})
                    .then((recs) => {

                        let recipeId = recs[0]._id;

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

        IngredientRecipeAttributes.findOne({name: attributeName})
            .then(attribute => {

                request(app)
                    .put('/ingredient/attribute')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({_id: attribute._id, itemSelectedForShopping: false})
                    .expect(204)
                    .end( (err, res) => {

                        if(err) return done(err);

                        IngredientRecipeAttributes.findOne({name: attributeName})
                            .then(attrUpdate => {

                                expect(attrUpdate.itemSelectedForShopping).toBe(false);
                                done();
                            });
                    });
            });
    });

    it("should update attribute list of a ingredient/recipe", done =>{

        IngredientRecipeAttributes.findOne({name: attributeName})
            .then(attribute => {

                request(app)
                    .put('/ingredient/attribute/many')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send([{_id: attribute._id, itemSelectedForShopping: false}])
                    .expect(204)
                    .end( (err, res) => {

                        if(err) return done(err);

                        IngredientRecipeAttributes.findOne({name: attributeName})
                            .then(attrUpdate => {

                                expect(attrUpdate.itemSelectedForShopping).toBe(false);
                                done();
                            });
                    });
            });
    });

    it("should delete a ingredient", (done) => {

        let name = ingredientNames[0];

        Ingredient.findOne({name})
            .then((doc) => {
                request(app)
                    .delete('/ingredient')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({_id : doc._id})
                    .expect(204)
                    .expect((res) => {

                        Ingredient.findOne({name})
                            .then((doc) => {
                                expect(doc).toBe(null);
                            }).catch((reason) => {
                                done(reason);
                            });
                    })
                    .end(done)
            });
    });

    function loadIdsToRecAttributes() {

        let promise = Q.defer();

        let result = {
            recipeId: '',
            ingredientId: ''
        }

        IngredientRecipeAttributes.find({})
            .then( recs => {
                result.recipeId = recs[0].recipeId;
                result.ingredientId = recs[0].ingredientId

                promise.resolve(result);

            }).catch((reason) => {
                console.error(reason)
                promise.reject(reason);
            })

        return promise.promise;
    }
});
