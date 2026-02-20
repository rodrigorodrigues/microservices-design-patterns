package com.springboot.android.util;

import java.util.List;

public class PermissionHelper {

    // Check if user has any of the specified permissions
    public static boolean hasAnyPermission(List<String> authorities, String... permissions) {
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        // Check for ROLE_ADMIN first (admins have all permissions)
        if (authorities.contains("ROLE_ADMIN")) {
            return true;
        }

        // Check for SCOPE_openid (OAuth users)
        if (authorities.contains("SCOPE_openid")) {
            return true;
        }

        // Check for specific permissions
        for (String permission : permissions) {
            if (authorities.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    // Permission check methods for specific resources

    // Company permissions
    public static boolean hasCompanyCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_COMPANY_CREATE");
    }

    public static boolean hasCompanySaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_COMPANY_SAVE");
    }

    public static boolean hasCompanyDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_COMPANY_DELETE");
    }

    // Person permissions
    public static boolean hasPersonCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PERSON_CREATE");
    }

    public static boolean hasPersonSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PERSON_SAVE");
    }

    public static boolean hasPersonDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PERSON_DELETE");
    }

    // Product permissions
    public static boolean hasProductCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PRODUCT_CREATE");
    }

    public static boolean hasProductSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PRODUCT_SAVE");
    }

    public static boolean hasProductDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_PRODUCT_DELETE");
    }

    // User permissions
    public static boolean hasUserCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_USER_CREATE");
    }

    public static boolean hasUserSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_USER_SAVE");
    }

    public static boolean hasUserDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_USER_DELETE");
    }

    // Post permissions
    public static boolean hasPostCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_POST_CREATE");
    }

    public static boolean hasPostSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_POST_SAVE");
    }

    public static boolean hasPostDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_POST_DELETE");
    }

    // Task permissions
    public static boolean hasTaskCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_TASK_CREATE");
    }

    public static boolean hasTaskSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_TASK_SAVE");
    }

    public static boolean hasTaskDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_TASK_DELETE");
    }

    // Category permissions (Week Menu)
    public static boolean hasCategoryCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_CATEGORY_CREATE");
    }

    public static boolean hasCategorySaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_CATEGORY_SAVE");
    }

    public static boolean hasCategoryDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_CATEGORY_DELETE");
    }

    // Ingredient permissions
    public static boolean hasIngredientCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_INGREDIENT_CREATE");
    }

    public static boolean hasIngredientSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_INGREDIENT_SAVE");
    }

    public static boolean hasIngredientDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_INGREDIENT_DELETE");
    }

    // Recipe permissions
    public static boolean hasRecipeCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_RECIPE_CREATE");
    }

    public static boolean hasRecipeSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_RECIPE_SAVE");
    }

    public static boolean hasRecipeDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_RECIPE_DELETE");
    }

    // Warehouse permissions
    public static boolean hasWarehouseCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_WAREHOUSE_CREATE");
    }

    public static boolean hasWarehouseSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_WAREHOUSE_SAVE");
    }

    public static boolean hasWarehouseDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_WAREHOUSE_DELETE");
    }

    // Stock permissions
    public static boolean hasStockCreateAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_STOCK_CREATE");
    }

    public static boolean hasStockSaveAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_STOCK_SAVE");
    }

    public static boolean hasStockDeleteAccess(List<String> authorities) {
        return hasAnyPermission(authorities, "ROLE_STOCK_DELETE");
    }
}
