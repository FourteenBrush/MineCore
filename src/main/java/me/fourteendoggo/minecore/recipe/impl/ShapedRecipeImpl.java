package me.fourteendoggo.minecore.recipe.impl;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.List;

public class ShapedRecipeImpl extends ShapedRecipe {
    public ShapedRecipeImpl(String recipeId, int width, int height, String group, List<DeclareRecipesPacket.Ingredient> ingredients, ItemStack result) {
        super(recipeId, width, height, group, result, ingredients);
    }

    @Override
    public boolean shouldShow(@NotNull Player player) {
        /*
        List<ItemStack> inventoryContents = Arrays.asList(player.getInventory().getItemStacks());
        return getIngredients().stream().anyMatch(ingredient -> {
            List<ItemStack> items = ingredient.items();
            return items.stream().anyMatch(inventoryContents::contains);
        });
         */
        return true;
    }

    @Override
    public @Nullable ItemStack assemble(@NotNull Inventory inventory) {
        int recipeStartRow = -1;
        int recipeStartCol = -1;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack placedItem = inventory.getItemStack(row * 3 + col + 1); // grid indices from 1 to 10
                if (!placedItem.isAir()) {
                    recipeStartRow = recipeStartRow == -1 ? row : recipeStartRow;
                    recipeStartCol = recipeStartCol == -1 ? col : recipeStartCol;
                }

                if (3 - recipeStartRow < getHeight() || 3 - recipeStartCol < getWidth()/* || row - recipeStartRow >= getHeight() || col - recipeStartCol >= getWidth()*/) {
                    Logger.info("Out of bounds: at [{}, {}] of [{}, {}], startRow: {}, startCol: {}", row, col, getHeight(), getWidth(), recipeStartRow, recipeStartCol);
                    return null; // placed an unexpected item outside the pattern bounds
                }

                int ingredientIndex = (row - recipeStartRow) * getWidth() + col - recipeStartCol;
                if (ingredientIndex >= getIngredients().size()) return null;

                List<ItemStack> items = getIngredients().get(ingredientIndex).items();
                Logger.info("Ingredient at index {}: {}, row: {}, col: {}", ingredientIndex, items, row, col);

                if (items.isEmpty()) {
                    if (placedItem.isAir()) continue;
                    return null; // placed an item where there should be none
                }
                Logger.info("Items: {}, placed item: {}", items, placedItem);
                if (items.stream().map(ItemStack::material).noneMatch(placedItem.material()::equals)) return null; // placed an item that doesn't match the recipe
            }
        }
        return getResult();
    }

    @Override
    public String toString() {
        return "ShapedRecipeImpl{recipeId=" + getRecipeId() + '}';
    }
}
