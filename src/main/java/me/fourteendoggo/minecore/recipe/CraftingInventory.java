package me.fourteendoggo.minecore.recipe;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.recipe.Recipe;
import org.tinylog.Logger;

import static me.fourteendoggo.minecore.util.SharedConstants.COLOR_SUCCESS;

/**
 * Represents a crafting inventory where at most one viewer has access to.
 */
public class CraftingInventory extends Inventory {

    public CraftingInventory(Component title) {
        super(InventoryType.CRAFTING, title);
    }

    public void onCraft(Recipe recipe, Player player) {
        ItemStack assembled = recipe.assemble(this);
        if (assembled == null) {
            Logger.info("Crafting failed");
            return;
        }
        clear();
        player.getInventory().addItemStack(assembled);
        player.sendMessage(Component.text("Crafted " + assembled.amount() + " " + assembled.material().name(), COLOR_SUCCESS));
    }
}
