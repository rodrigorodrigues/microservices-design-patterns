/**
 *  Legacy code, very messy
 *
 * npm run test-watch
 */

const request = require('supertest');
const expect = require('expect');
const jwt = require('jsonwebtoken');

const app = require('../../../server').app;

const {Recipe} = require('../../../models/recipe.model.js');
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

const nock = require('nock')

import sleep from 'await-sleep';

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

    it('should get recipe list', (done) => {

       request(app)
           .get('/recipe')
           .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
           .expect(200)
           .expect((res) => {
                expect(Array.isArray(res.body)).toBe(true)
           })
           .end(done);

    });

    it('should get recipe week list', (done) => {

        request(app)
            .get('/recipe/week')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(200)
            .expect((res) => {
                expect(res.body.length).toBe(1);
            })
            .end(done)

    });

    it('should load recipe by passing an Id', (done) => {

        Recipe.findOne({name : recipes[0].name})
            .then(rec => {

                request(app)
                    .get('/recipe/' + rec._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .expect((res) => {
                        expect(res.body._id).toBe(rec._id.toString())
                    }).end(done);
            });
    });

    it("should create a recipe", (done) => {

        let name = 'rec_spec_post';

        Recipe.remove({name}).then( () => {
            request(app)
                .post('/recipe')
                .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                .send({'name' : name})
                .expect(201)
                .expect((res) => {
                    expect(res.body).toIncludeKey('_id');
                    let id = res.body._id;

                    Recipe.findOne({_id: id})
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

        request(app)
            .post('/recipe')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .expect(400)
            .end((err, res) => {

                if (err) return done(err);

                Recipe.find({})
                    .then( () => {

                        expect(res.body).toIncludeKeys(['message', 'errors', 'name']);
                        done();

                    }).catch((reason) => {
                        done(reason);
                    });
            });

    });

    it("should fail to create a duplicate recipe", (done) => {

        request(app)
            .post('/recipe')
            .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
            .send({name : recipes[0].name})
            .expect(400)
            .expect((res) => {
                expect(res.body.message).toInclude('duplicate key error')
            }).end(done)
    });

    it("should update a recipe", (done) => {

        let nameTestUpdate = 'from_recipe_testnameUpdate';

        Recipe.findOne({name: recipes[1].name})
            .then(rec => {

                request(app)
                    .put('/recipe')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({name: nameTestUpdate, _id: rec._id})
                    .expect(204)
                    .end((err, res) => {

                        if (err) return done(err);

                        Recipe.findOne({name: recipes[1].name})
                            .then(doc => {

                                expect(doc).toBe(null);
                                done();

                            }).catch((reason) => {
                                done(reason)
                            });
                    });
            });

    });


    it("should get all recipe's ingredients along it categories", (done) => {

        let name = recipes[0].name;

        Recipe.findOne({name})
            .then((docFindOne) => {

                request(app)
                    .get('/recipe/category/'+ docFindOne._id)
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .expect(200)
                    .end((err, res) => {

                        if (err) return done(err);

                        let recipe = res.body;

                        expect(recipe.categories.length).toBe(1);

                        recipe.categories.forEach(category => {

                            expect(category.ingredients.length > 0).toBe(true);

                        });

                        done();
                    });
            });
    });

    it("should link recipe to categories/ingredients and return it populated", (done) => {

        let name = recipes[0].name;

        Recipe.findOne({name}).then(() => {

            //same recipe name
            IngredientRecipeAttributes.findOne({name}).then(attr => {

                attr.isRecipeLinkedToCategory = true;

                attr.save().then(() => {

                    Ingredient.findOne({_id: attr.ingredientId}).then(ingredient => {
                        //mark checkbox
                        ingredient.tempRecipeLinkIndicator = true;
                        sendRecipeAndParameters(ingredient);
                    });
                });
            });
        });


        function sendRecipeAndParameters(ingredient) {

            let name = recipes[0].name;

            Recipe.findOne({name}).then( (recFindOne) => {

                request(app)
                    .put('/recipe/ingredient')
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
                .get('/recipe/category/'+ id)
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

        Recipe.findOne({name: recipes[0].name}).then(rec => {

            request(app)
                .get('/recipe/category/currentAttribute/'+rec._id)
                .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                .expect(200)
                .expect(res => {
                    let recipe = res.body;

                   // console.log("deep docSaved", categories);

                    recipe.categories.forEach(category => {

                        category.ingredients.forEach(ingredient => {

                            expect(ingredient.attributes.length > 0).toBe(true);
                            expect(ingredient.attributes[0].itemSelectedForShopping).toBe(true);

                        });

                    });

                })
                .end(done)
        });
    });

    it("should delete a recipe", (done) => {

        let name = recipes[0].name;

        Recipe.findOne({name})
            .then((doc) => {
                request(app)
                    .delete('/recipe')
                    .set('Authorization', 'Bearer ' + jwt.sign({ user: 'Test', authorities: ['ROLE_ADMIN'] }, Buffer.from(process.env.SECRET_TOKEN, 'base64'), { expiresIn: '1h' }))
                    .send({_id : doc._id})
                    .expect(204)
                    .expect((res) => {
                        Recipe.findOne({name})
                            .then(result => {
                                expect(result).toBe(null);
                            });
                    })
                    .end(done)
            });
    });

});
