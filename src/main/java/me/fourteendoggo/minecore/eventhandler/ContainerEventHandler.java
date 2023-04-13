package me.fourteendoggo.minecore.eventhandler;

import me.fourteendoggo.minecore.recipe.CraftingInventory;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.type.*;
import net.minestom.server.item.Material;

import java.util.function.Function;
import java.util.function.Predicate;

public class ContainerEventHandler {

    public ContainerEventHandler() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerBlockInteractEvent.class, this::onBlockInteract);
    }

    private void onBlockInteract(PlayerBlockInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && !player.getItemInMainHand().isAir()) return; // player is trying to place a block against this block

        Material material = event.getBlock().registry().material();
        if (material == null) return;

        BlockInventoryType blockInventoryType = BlockInventoryType.fromMaterial(material);
        if (blockInventoryType == null) return;

        event.setBlockingItemUse(true);
        player.openInventory(blockInventoryType.creatInventory());
    }

    private enum BlockInventoryType {
        CRAFTING_TABLE("container.crafting", Material.CRAFTING_TABLE, CraftingInventory::new),
        FURNACE("container.furnace", Material.FURNACE, FurnaceInventory::new),
        BLAST_FURNACE("container.blast_furnace", Material.BLAST_FURNACE, FurnaceInventory::new),
        SMOKER("container.smoker", Material.SMOKER, FurnaceInventory::new),
        STONE_CUTTER("container.stonecutter", InventoryType.STONE_CUTTER, Material.STONECUTTER),
        BARREL("container.barrel", InventoryType.CHEST_3_ROW, Material.BARREL),
        CHEST("container.chest", InventoryType.CHEST_4_ROW, Material.CHEST),
        ENDER_CHEST("container.enderchest", InventoryType.CHEST_4_ROW, Material.ENDER_CHEST),
        SHULKER_BOX("container.shulkerBox", InventoryType.CHEST_4_ROW, material -> material.name().endsWith("shulker_box")),
        DISPENSER("container.dispenser", InventoryType.WINDOW_3X3, Material.DISPENSER),
        DROPPER("container.dropper", InventoryType.WINDOW_3X3, Material.DROPPER),
        HOPPER("container.hopper", InventoryType.HOPPER, Material.HOPPER),
        ENCHANTMENT("container.enchant", Material.ENCHANTING_TABLE, EnchantmentTableInventory::new),
        BREWING_STAND("container.brewing", Material.BREWING_STAND, BrewingStandInventory::new),
        ANVIL("container.repair", material -> material.name().endsWith("anvil"), AnvilInventory::new),
        BEACON("container.beacon", Material.BEACON, BeaconInventory::new),
        GRINDSTONE("container.grindstone_title", InventoryType.GRINDSTONE, Material.GRINDSTONE),
        LOOM("container.loom", InventoryType.LOOM, Material.LOOM),
        SMITHING("container.upgrade", InventoryType.SMITHING, Material.SMITHING_TABLE),
        CARTOGRAPHY("container.cartography_table", InventoryType.CARTOGRAPHY, Material.CARTOGRAPHY_TABLE);

        private final Component title;
        private final Predicate<Material> matcher;
        private final Function<Component, Inventory> inventoryFunction;

        BlockInventoryType(String title, InventoryType innerType, Material matchMaterial) {
            this(title, material -> material == matchMaterial, component -> new Inventory(innerType, component));
        }

        BlockInventoryType(String title, InventoryType innerType, Predicate<Material> matcher) {
            this(title, matcher, component -> new Inventory(innerType, component));
        }

        BlockInventoryType(String title, Material matchMaterial, Function<Component, Inventory> inventoryFunction) {
            this(title, material -> material == matchMaterial, inventoryFunction);
        }

        BlockInventoryType(String title, Predicate<Material> matcher, Function<Component, Inventory> inventoryFunction) {
            this.title = Component.translatable(title);
            this.matcher = matcher;
            this.inventoryFunction = inventoryFunction;
        }

        public Inventory creatInventory() {
            return inventoryFunction.apply(title);
        }

        public static BlockInventoryType fromMaterial(Material material) {
            for (BlockInventoryType type : values()) {
                if (type.matcher.test(material)) {
                    return type;
                }
            }
            return null;
        }
    }
}
