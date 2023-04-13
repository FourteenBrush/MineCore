package me.fourteendoggo.minecore.eventhandler;

import me.fourteendoggo.minecore.recipe.CraftingInventory;
import me.fourteendoggo.minecore.recipe.RecipeManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryClickEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.*;
import net.minestom.server.recipe.Recipe;
import org.tinylog.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class RecipeEventHandler {
    private static final boolean LOG_NON_IGNORED_PACKETS = false;
    private final RecipeManager recipeManager;
    private final Map<UUID, String> requestedRecipes = new WeakHashMap<>();
    private final Set<Class<? extends ClientPacket>> ignoredPackets = Set.of(
            ClientKeepAlivePacket.class,
            ClientPlayerRotationPacket.class,
            ClientPlayerPositionPacket.class,
            ClientPlayerPositionAndRotationPacket.class
    );

    public RecipeEventHandler(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerPacketEvent.class, this::onServerBoundPacket);
        globalEventHandler.addListener(InventoryClickEvent.class, this::onInventoryClick);
    }

    private void onServerBoundPacket(PlayerPacketEvent event) {
        ClientPacket packet = event.getPacket();
        Player player = event.getPlayer();
        if (LOG_NON_IGNORED_PACKETS && !ignoredPackets.contains(packet.getClass())) {
            Logger.info(packet);
        }
        if (!(packet instanceof ClientCraftRecipeRequest recipeRequest)) return;
        if (!(player.getOpenInventory() instanceof CraftingInventory)) return;

        requestedRecipes.put(player.getUuid(), recipeRequest.recipe());
        Logger.info("Requested recipe");
    }

    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory craftingInventory)) return;
        if (event.getSlot() > 0) return;

        Player player = event.getPlayer();
        String requestedRecipeId = requestedRecipes.get(player.getUuid());
        if (requestedRecipeId == null) return;

        Recipe recipe = recipeManager.getById(requestedRecipeId);
        craftingInventory.onCraft(recipe, player);
    }
}
