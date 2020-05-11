/**
 * Legacy code, very messy
 */
require('dotenv').config();
const request = require("supertest");
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Category} = require('../../../models/category.model.js');
const {Ingredient} = require('../../../models/ingredient.model.js');
const {Recipe2} = require('../../../models/recipe2.model');
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

import 'jest-extended';

describe("Category", () => {
    beforeAll(async done => {
        console.log('Waiting for 5 secs');
    
        await sleep(5000);
    
        done();
    });

    afterAll(async done => {
        app.emit('close');

        done();
    });

    it("should return 403 when calling api with not valid role", (done) => {
        return request(app)
            .get('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_TEST'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(403)
            .end(done);
    });
    

    it("should get category list", (done) => {
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
        return request(app)
            .get('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .expect(res => {
                expect(res.body).toBeArray()
            })
            .end(done);
    });

    it("should get category list and ingredient marked", async (done) => {

        let testPassed = false;

        const count = await IngredientRecipeAttributes.count();
        if (count == 0) {
            return done();
        }
        return IngredientRecipeAttributes.findOne()
            .then((attr) => {

                Recipe2.findOne().then(recipe => {

                    request(app)
                        .get('/v2/category/check/'+recipe._id)
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

    it('should load category by passing an Id', (done) => {

        return Category.findOne()
            .then(doc => {
                request(app)
                    .get('/v2/category/' + doc._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(doc._id.toString())
                    }).end(done)
            });

    });

    it("should save/post a category", (done) => {

        let name = 'new cat';

        return Recipe2.findOne()
            .then(recipe => {
                request(app)
                    .post('/v2/category')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({'name' : name, recipeId: recipe._id})
                    .expect(201)
                    .end( (err, res) => {

                        if(err) return done;

                        expect(res.body).toContainKey('_id');

                        let id = res.body._id;

                        Category.findOne({_id: id})
                            .populate('recipes')
                            .then((doc) => {

                                expect(doc.name).not.toBeEmpty();
                                if (doc.recipes) {
                                    expect(doc.recipes).toSatisfy(n => n > 0);
                                }

                                done();

                            }).catch((reason) => {
                                return done(reason);
                            });
                    });
            });
    });

    it("should fail to save/post a category", (done) => {

        return request(app)
            .post('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(400)
            .expect((res) => {
                expect(res.body).toContainKeys(['message', 'errors', 'name']);

                Category.findOne()
                    .then((docs) => {

                        expect(docs.length).toBe(2);

                    }).catch((reason) => {
                    return reason
                });
            })
            .end(done);

    });

    it("should fail to save/post a duplicate category", (done) => {

        return Category.findOne()
            .then(cat => {
                request(app)
                    .post('/v2/category')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({name : cat.name})
                    .expect(400)
                    .expect((res) => {
                        expect(res.body.message).toInclude('This product already exist');
                    }).end(done)
            });
    });

    it("should update a category", (done) => {

        //update date
        let nameTestUpdate = categoryNames[0];

        return Category.findOne()
            .then(doc => {
                nameTestUpdate += 'updateName';

                request(app)
                    .put('/v2/category')
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

    it("should delete a category", (done) => {

        return Category.findOne()
            .then((category) => {
                request(app)
                    .delete('/v2/category/'+category._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
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

    it("should get category along ingredient populated", (done) => {

        //FIXME review test
        return request(app)
            .get('/v2/category')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .end((err, res) => {

                if(err) throw err

                const categories = res.body.filter(cat => cat.ingredients !== undefined && cat.ingredients.length > 0);

                if (categories.length > 0) {

                    let temp = 0;
                    categories.forEach( (cat) => {

                        if(cat.ingredients.length > 0) {
                            temp++;
                        }

                    });
                    //for now the category its messy
                    expect(temp > 0).toBe(true);

                }

                done();
            });
    });

    it("should get category/ingredient for the shopping week", (done) => {

        return request(app)
            .get('/category/week/shopping')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .end((err, res) => {

                if(err) throw err

                let categories  = res.body;

                let contLoopEnd = categories.length;

                categories.forEach( (cat) => {

                    if (cat.ingredients) {
                        cat.ingredients.forEach(ingredient => {

                            ingredient.attributes.forEach(attr => {
                            expect(attr.itemSelectedForShopping).toBe(true);
                            });
                        });
                    }

                    if(--contLoopEnd === 0) {
                        done();
                    }
                });

            });
    });

});