/**
 * Legacy code, very messy
 */
require('dotenv').config();
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

describe("Category", () => {
    beforeAll(async done => {
        console.log('Waiting for 5 secs');
    
        await sleep(5000);
    
        done();
    });

    test("should return 403 when calling api with not valid role", (done) => {
        request.agent(app)
            .get('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_TEST'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(403)
            .end(done);
    });
    

    test("should get category list", async done => {
        /* // How to use nock?
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
        const res = await request.agent(app)
            .get('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }));

        expect(res.status).toBe(200);
        //expect(res.body.message).toBe("Ok"); TODO How to test response?
        done();
    });

    it("should get category list and ingredient marked", async done => {

        let testPassed = false;

        IngredientRecipeAttributes.findOne({name: recipeName})
            .then((attr) => {

                Recipe.findOne({name: recipeName}).then(recipe => {

                    request(app)
                        .get('/category/check/'+recipe._id)
                        .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it('should load category by passing an Id', async done => {

        await Category.findOne({name: categoryNames[0]})
            .then(doc => {
                request(app)
                    .get('/category/' + doc._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(doc._id.toString())
                    }).end(done)
            });

    });

    it("should save/post a category", async done => {

        let name = 'new cat';

        await Recipe.findOne({name: recipeName})
            .then(recipe => {
                request(app)
                    .post('/category')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it("should fail to save/post a category", async done => {

        await request(app)
            .post('/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it("should fail to save/post a duplicate category", async done => {

        await request(app)
            .post('/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .send({name : categoryNames[0]})
            .expect(400)
            .expect((res) => {
                expect(res.body.message).toInclude('duplicate key error')
            }).end(done)
    });

    it("should update a category", async done => {

        //update date
        let nameTestUpdate = categoryNames[0];

        await Category.findOne({name: nameTestUpdate})
            .then(doc => {

                nameTestUpdate += 'updateName';

                request(app)
                    .put('/category')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it("should delete a category", async done => {

        await Category.find({})
            .then((docs) => {

                let category = docs[0];

                request(app)
                    .delete('/category')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it("should get category along ingredient populated", async done => {

        //FIXME review test
        await request(app)
            .get('/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

    it("should get category/ingredient for the shopping week", async done => {

        await request(app)
            .get('/category/week/shopping')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
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

});