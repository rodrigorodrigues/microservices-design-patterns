package com.microservice.web.common.util.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public enum Permissions {
    ROLE_ADMIN("Admin Permission", Collections.singletonList("ROLE_ADMIN")),

    ROLE_PERSON("Person Permissions", Arrays.asList("ROLE_PERSON_READ", "ROLE_PERSON_SAVE", "ROLE_PERSON_DELETE", "ROLE_PERSON_CREATE")),

    ROLE_PRODUCT("Product Permissions", Arrays.asList("ROLE_PRODUCT_READ", "ROLE_PRODUCT_SAVE", "ROLE_PRODUCT_DELETE", "ROLE_PRODUCT_CREATE")),

    ROLE_INGREDIENT("Ingredient Permissions", Arrays.asList("ROLE_INGREDIENT_READ", "ROLE_INGREDIENT_SAVE", "ROLE_INGREDIENT_DELETE", "ROLE_INGREDIENT_CREATE")),

    ROLE_CATEGORY("Category Permissions", Arrays.asList("ROLE_CATEGORY_READ", "ROLE_CATEGORY_SAVE", "ROLE_CATEGORY_DELETE", "ROLE_CATEGORY_CREATE")),

    ROLE_RECIPE("Recipe Permissions", Arrays.asList("ROLE_RECIPE_READ", "ROLE_RECIPE_SAVE", "ROLE_RECIPE_DELETE", "ROLE_RECIPE_CREATE")),

    ROLE_TASK("Task Permissions", Arrays.asList("ROLE_TASK_READ", "ROLE_TASK_SAVE", "ROLE_TASK_DELETE", "ROLE_TASK_CREATE"));

    private final String type;

    private final List<String> permissions;
}
