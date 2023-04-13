package me.fourteendoggo.minecore.recipe.impl;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.CampfireCookingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CampfireCookingRecipeImpl extends CampfireCookingRecipe {
    public CampfireCookingRecipeImpl(String recipeId, String group, DeclareRecipesPacket.Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(recipeId, group, result, ingredient, experience, cookingTime);
    }

    @Override
    public boolean shouldShow(@NotNull Player player) {
        return true;
    }

    @Override
    public @Nullable ItemStack assemble(@NotNull Inventory inventory) {
        return null;
    }

    @Override
    public String toString() {
        return "CampfireCookingRecipeImpl{recipeId=" + getRecipeId() + '}';
    }
}
