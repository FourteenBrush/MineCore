package me.fourteendoggo.minecore.recipe.impl;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShapelessRecipeImpl extends ShapelessRecipe {
    public ShapelessRecipeImpl(String recipeId, String group, List<DeclareRecipesPacket.Ingredient> ingredients, ItemStack result) {
        super(recipeId, group, result, ingredients);
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
        return "ShapelessRecipeImpl{recipeId=" + getRecipeId() + '}';
    }
}
