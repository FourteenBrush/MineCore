package me.fourteendoggo.minecore.recipe.impl;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.SmithingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmithingRecipeImpl extends SmithingRecipe {
    public SmithingRecipeImpl(String recipeId, DeclareRecipesPacket.Ingredient baseIngredient, DeclareRecipesPacket.Ingredient additionIngredient, ItemStack result) {
        super(recipeId, baseIngredient, additionIngredient, result);
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
        return "SmithingRecipeImpl{recipeId=" + getRecipeId() + '}';
    }
}
