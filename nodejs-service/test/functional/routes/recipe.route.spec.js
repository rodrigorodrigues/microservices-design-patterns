/**
 *  Legacy code, very messy
 *
 * npm run test-watch
 */

const request = require('supertest');
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Recipe2} = require('../../../models/recipe2.model.js');
const {Category} = require('../../../models/category.model.js');
const {Ingredient} = require('../../../models/ingredient.model.js');
const {IngredientRecipeAttributes} = require('../../../models/ingredient.recipe.attributes.model.js');

const categoryNames = [
    'from_rec_cattest0',
    'from_rec_cattest1'
];

const ingredientNames = [
    "from_rec_ingredient0",
    "from_rec_ingredient1"
];

import sleep from 'await-sleep';

import 'jest-extended';

const Q = require('q');

const recipes = [
    {
        name: 'from_rec_spec_testname1',
        isInMenuWeek: true
    },
    {
        name: 'from_rec_spec_testname2'
    }
];


describe('Recipe', () => {
    beforeAll(async done => {
        console.log('Waiting for 5 secs');
    
        await sleep(5000);
    
        done();
    });

    afterAll(async done => {
        app.emit('close');

        done();
    });

    it('should get recipe list', (done) => {

       return request(app)
           .get('/v2/recipe')
           .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
           .expect(200)
           .expect((res) => {
                expect(res.body).toBeArray();
           })
           .end(done);

    });

    it('should get recipe week list', (done) => {

        return request(app)
            .get('/recipe/week')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .expect((res) => {
                expect(res.body).toBeArray();
            })
            .end(done)

    });

    it('should load recipe by passing an Id', (done) => {

        return Recipe2.findOne()
            .then(rec => {
                console.log(`Recipe resource by passing id: ${rec}`);

                request(app)
                    .get('/v2/recipe/' + rec._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(rec._id.toString())
                    }).end(done);
            });
    });

    it("should create a recipe", (done) => {

        let name = 'rec_spec_post';

        return Recipe2.remove({name}).then( () => {
            request(app)
                .post('/v2/recipe')
                .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                .send({'name' : name})
                .expect(201)
                .expect((res) => {
                    expect(res.body).toContainKey('_id');
                    let id = res.body._id;

                    Recipe2.findOne({_id: id})
                        .then((docs) => {
                            expect(docs.length).toBe(3)

                        }).catch((reason) => {
                        return reason
                    });
                })
                .end(done);
        });
    });

    it("should fail to create a recipe, empty name", (done) => {

        return request(app)
            .post('/v2/recipe')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(400)
            .end((err, res) => {

                if (err) return done(err);

                Recipe2.findOne()
                    .then( () => {

                        expect(res.body).toContainKeys(['message', 'errors', 'name']);
                        done();

                    }).catch((reason) => {
                        done(reason);
                    });
            });

    });

    it("should fail to create a duplicate recipe", (done) => {

        return Recipe2.findOne()
            .then(rec => {
                console.log(`Recipe resource for duplicate: ${rec}`);
                request(app)
                    .post('/v2/recipe')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({name : rec.name})
                    .expect(400)
                    .expect((res) => {
                        expect(res.body.message).toInclude('This product already exist')
                    }).end(done)
            });
    });

    it("should update a recipe", async (done) => {

        let nameTestUpdate = 'from_recipe_testnameUpdate';

        const category = await Category.findOne();
        console.log(`Category resource for update recipe: ${category}`);

        return Recipe2.findOne()
            .then(rec => {
                console.log(`Recipe resource for update: ${rec}`);

                request(app)
                    .put('/v2/recipe')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({name: nameTestUpdate, _id: rec._id, categories: [category]})
                    .expect(204)
                    .end((err, res) => {

                        if (err) return done(err);

                        Recipe2.findOne({name: recipes[1].name})
                            .then(doc => {

                                expect(doc).toBeNil();
                                done();

                            }).catch((reason) => {
                                done(reason)
                            });
                    });
            });

    });


    it("should get all recipe's ingredients along it categories", (done) => {

        return Recipe2.findOne()
            .then((docFindOne) => {
                console.log(`Recipe resource for ingredients along categories: ${docFindOne}`);

                request(app)
                    .get('/recipe/category/'+ docFindOne._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .end((err, res) => {

                        if (err) return done(err);

                        let recipe = res.body;

                        if (recipe && recipe.categories) {

                            expect(recipe.categories.length).toBeArrayOfSize(1);

                            recipe.categories.forEach(category => {

                                expect(category.ingredients).toSatisfy(n => n > 0);

                            });

                        }

                        done();
                    });
            });
    });

    it("should link recipe to categories/ingredients and return it populated", (done) => {

        return Recipe2.findOne().then((recipe) => {
            console.log(`Recipe resource to categories/ingredients: ${recipe}`);
            const name = recipe.name;

            //same recipe name
            IngredientRecipeAttributes.findOne({name}).then(attr => {
                if (attr) {

                    attr.isRecipeLinkedToCategory = true;

                    attr.save().then(() => {

                        Ingredient.findOne({_id: attr.ingredientId}).then(ingredient => {
                            //mark checkbox
                            ingredient.tempRecipeLinkIndicator = true;
                            sendRecipeAndParameters(ingredient);
                        });
                    });
                } else {
                    done();
                }
            });
        });


        function sendRecipeAndParameters(ingredient) {

            let name = recipes[0].name;

            Recipe2.findOne({name}).then( (recFindOne) => {

                request(app)
                    .put('/v2/recipe/ingredient')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({_id: recFindOne._id, ingredient : ingredient})
                    .expect(204)
                    .end((err, res) => {

                        if (err) return done(err);

                        requestRecipePopulated(recFindOne._id, ingredient);
                    });
            });
        }

        function requestRecipePopulated(id, ingredientSent) {
            let getInto = false;
            request(app)
                .get('/v2/recipe/category/'+ id)
                .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                .expect(200)
                .end((err, res) => {

                    if (err) return done(err);

                    let recipe = res.body;

                    recipe.categories.forEach(cat => {
                        //send the tempRecipeLinkIndicator/false = remove
                        cat.ingredients.forEach(ing => {

                            if(ing.name === ingredientSent.name) {
                                expect(ingredientSent.tempRecipeLinkIndicator).toBe(true);
                                getInto = true;
                            }

                        });
                    });

                    //make sure the expect run in the loop
                    expect(getInto).toBe(true);

                    done();
                });
        }

    });

    it("should load all ingredients and current attributes of a recipe", done => {

        return Recipe2.findOne().then(rec => {
            console.log(`Recipe Resource for load all ingredients and current attributes: ${rec}`);

            request(app)
                .get('/recipe/category/currentAttribute/'+rec._id)
                .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                .expect(200)
                .expect(res => {
                    let recipe = res.body;

                    if (recipe && recipe.categories) {

                        recipe.categories.forEach(category => {

                            category.ingredients.forEach(ingredient => {

                                expect(ingredient.attributes).toSatisfy(n => n > 0);
                                expect(ingredient.attributes[0].itemSelectedForShopping).toBe(true);

                            });

                        });

                    }

                })
                .end(done)
        });
    });

    it("should delete a recipe", (done) => {

        return Recipe2.findOne()
            .then((doc) => {
                console.log(`Recipe resource for deletion: ${doc}`);
                const name = doc.name;
                request(app)
                    .delete('/v2/recipe/'+doc._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .end((err, res) => {

                        if(err) {
                            return done(err)
                        }

                        Recipe2.findOne({_id : doc._id})
                            .then((doc) => {
                                expect(doc).toBeNil();
                                done();
                            }).catch((reason) => {
                                done(reason)
                            });

                    })
            });
    });

});
