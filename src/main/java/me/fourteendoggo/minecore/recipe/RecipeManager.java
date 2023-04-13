package me.fourteendoggo.minecore.recipe;

import net.minestom.server.recipe.Recipe;

import java.util.Map;

public class RecipeManager {
    private final Map<String, Recipe> byId;

    public RecipeManager(Map<String, Recipe> recipes) {
        this.byId = recipes;
    }

    public Recipe getById(String recipeId) {
        if (recipeId.startsWith("minecraft:")) {
            recipeId = recipeId.substring(10);
        }
        return byId.get(recipeId);
    }
}
