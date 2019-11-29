/**
 * Legacy code, very messy
 */

const request = require("supertest");
const expect = require("expect");
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Category} = require('../../../models/category.model.js');
const {Ingredient} = require('../../../models/ingredient.model.js');
const {Recipe} = require('../../../models/recipe.model.js');
const {IngredientRecipeAttributes} = require('../../../models/ingredient.recipe.attributes.model.js');

const categoryNames = [
    "from_cat_categoryTest0",
    "from_cat_categoryTest1",
];

const ingredientNames = [
    "from_cat_ingredient0",
    "from_cat_ingredient1"
];

let recipeName = 'recipe_test_cat_spec';

import sleep from 'await-sleep';

beforeAll(async () => {
    console.log('Waiting for 2 secs');

    await sleep(2000);
/*    
    await request(app)
        .get('/actuator/health')
        .expect(200); */
});

test("should get category list", async () => {
    /*
    nock('http://localhost:8888')
    .get(/\/week-menu-api\/(.*?)/)
    .reply(200, {
        response: {
        configuration: {
            jwt: {
                'base64-secret': 'VGVzdAo='
            }
        }
        },
    });*/

    await request.agent(app)
        .get('/v2/category')
        .set('Authorization', jwt.sign({authorities: ['ROLE_ADMIN']}, 'VGVzdAo='))
        .expect(200)
        .expect((res) => expect(res.body.length >= 2).toBe(true));
});

    it.skip("should get category list and ingredient marked", (done) => {

        let testPassed = false;

        IngredientRecipeAttributes.findOne({name: recipeName})
            .then((attr) => {

                Recipe.findOne({name: recipeName}).then(recipe => {

                    request(app)
                        .get('/category/check/'+recipe._id)
                        .expect(200)
                        .end((err, res) => {

                            if (err) return done(err);

                            let categories = res.body;

                            categories.forEach(category => {

                                if(category.ingredients.length > 0) {

                                    category.ingredients.forEach(ing => {
                                        if(attr.ingredientId.toString() === ing._id.toString()) {
                                            expect(ing.tempRecipeLinkIndicator).toBe(true);
                                            testPassed = true;
                                        }

                                    });
                                }
                            });

                            expect(testPassed).toBe(true);

                            done();

                            if (err) return done("didn't find recipe");
                        });
                });
            });
    });

    it.skip('should load category by passing an Id', (done) => {

        Category.findOne({name: categoryNames[0]})
            .then(doc => {

                request(app)
                    .get('/category/' + doc._id)
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(doc._id.toString())
                    }).end(done)
            });

    });

    it.skip("should save/post a category", (done) => {

        let name = 'new cat';

        Recipe.findOne({name: recipeName})
            .then(recipe => {

                request(app)
                    .post('/category')
                    .send({'name' : name, recipeId: recipe._id})
                    .expect(201)
                    .end( (err, res) => {

                        if(err) return done;

                        expect(res.body).toIncludeKey('_id');

                        let id = res.body._id;

                        Category.findOne({_id: id})
                            .populate('recipes')
                            .then((doc) => {

                                expect(doc.name !== undefined).toBe(true);
                                expect(doc.recipes.length > 0).toBe(true)

                                done();

                            }).catch((reason) => {
                                return done(reason);
                            });
                    });
            });
    });

    it.skip("should fail to save/post a category", (done) => {

        request(app)
            .post('/category')
            .expect(400)
            .expect((res) => {
                expect(res.body).toIncludeKeys(['message', 'errors', 'name']);

                Category.find({})
                    .then((docs) => {

                        expect(docs.length).toBe(2);

                    }).catch((reason) => {
                    return reason
                });
            })
            .end(done);

    });

    it.skip("should fail to save/post a duplicate category", (done) => {

        request(app)
            .post('/category')
            .send({name : categoryNames[0]})
            .expect(400)
            .expect((res) => {
                expect(res.body.message).toInclude('duplicate key error')
            }).end(done)
    });

    it.skip("should update a category", (done) => {

        //update date
        let nameTestUpdate = categoryNames[0];

        Category.findOne({name: nameTestUpdate})
            .then(doc => {

                nameTestUpdate += 'updateName';

                request(app)
                    .put('/category')
                    .send({name: nameTestUpdate, _id: doc._id})
                    .expect(204)
                    .end(err => {

                        if (err) return done(err);

                        Category.findOne({_id: doc._id})
                            .then((doc) => {

                                expect(doc.name).toBe(nameTestUpdate);
                                done();

                            }).catch((reason) => {
                            done(reason)
                        });
                    });
            });
    });

    it.skip("should delete a category", (done) => {

        Category.find({})
            .then((docs) => {

                let category = docs[0];

                request(app)
                    .delete('/category')
                    .send({_id : category._id})
                    .expect(204)
                    .end((err, res) => {

                        if(err) {
                            return done(err)
                        }

                        Category.findOne({_id : category._id})
                            .then((doc) => {
                                expect(doc).toBe(null);
                                done();
                            }).catch((reason) => {
                                done(reason)
                            });

                    })

            });

    });

    it.skip("should get category along ingredient populated", (done) => {

        //FIXME review test
        request(app)
            .get('/category')
            .expect(200)
            .end((err, res) => {

                if(err) throw err

                let categories = res.body;

                let temp = 0;
                categories.forEach( (cat) => {

                    if(cat.ingredients.length > 0) {
                        temp++;
                    }

                });
                //for now the category its messy
                expect(temp > 0).toBe(true);

                done();
            });
    });

    it.skip("should get category/ingredient for the shopping week", done => {

        request(app)
            .get('/category/week/shopping')
            .expect(200)
            .end((err, res) => {

                if(err) throw err

                let categories  = res.body;

                let contLoopEnd = categories.length;

                categories.forEach( (cat) => {

                    cat.ingredients.forEach(ingredient => {

                        ingredient.attributes.forEach(attr => {
                           expect(attr.itemSelectedForShopping).toBe(true);
                        });
                        //console.log("ingredient", ingredient.attributes)
                    });

                    if(--contLoopEnd === 0) {
                        done();
                    }
                });

            });
    });

    it.skip("SHOULD JUST BE A TEST", done => {

        let promisess = [];

        let arrouu = ['ID_1','ID2', 'ID_4']

        arrouu.forEach(item => {
            let promise = Promise.resolve(item);
            promisess.push(promise)
        })


        Promise.all(promisess).then(values => {
            //console.log("***************************", values); // [true, 3]

            done()
        });

    });
