package me.fourteendoggo.minecore;

import me.fourteendoggo.minecore.commands.*;
import me.fourteendoggo.minecore.eventhandler.ContainerEventHandler;
import me.fourteendoggo.minecore.eventhandler.GlobalEventHandlers;
import me.fourteendoggo.minecore.eventhandler.RecipeEventHandler;
import me.fourteendoggo.minecore.generation.ChunkGenerator;
import me.fourteendoggo.minecore.generation.SingleChunkGenerator;
import me.fourteendoggo.minecore.recipe.RecipeManager;
import me.fourteendoggo.minecore.recipe.loader.RecipeLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.PlacementRules;
import net.minestom.server.extras.optifine.OptifineSupport;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.client.play.ClientSetDisplayedRecipePacket;
import net.minestom.server.recipe.Recipe;
import org.tinylog.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public class MineCore {
    private static final boolean WORLD_GENERATION_TEST = false;

    public void initialize() throws IOException {
        MojangAuth.init();
        PlacementRules.init();
        OptifineSupport.enable();

        InstanceContainer instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        instanceContainer.setGenerator(WORLD_GENERATION_TEST ? new SingleChunkGenerator() : new ChunkGenerator());

        // avoid spamming the console when debugging packets
        MinecraftServer.getPacketListenerManager().setListener(ClientSetDisplayedRecipePacket.class, (player, packet) -> {});

        /*
        NoiseGeneratorSettings generatorSettings = WorldgenRegistries.NOISE_SETTINGS.getOrThrow(NamespaceID.from("minecraft:overworld"));
        NoiseChunkGenerator chunkGenerator = new NoiseChunkGenerator((x, y, z, sampler) -> NamespaceID.from("minecraft:jungle"),
                generatorSettings, instanceContainer.getDimensionType());

        instanceContainer.setChunkGenerator(chunkGenerator);
         */
        long now = System.currentTimeMillis();
        RecipeLoader recipeLoader = new RecipeLoader();
        Map<String, Recipe> recipes = recipeLoader.loadAll();
        RecipeManager recipeManager = new RecipeManager(recipes);
        Logger.info("Loaded {} recipes in {} ms", recipes.size(), System.currentTimeMillis() - now);

        new GlobalEventHandlers(instanceContainer);
        new RecipeEventHandler(recipeManager);
        new ContainerEventHandler();

        MinecraftServer.getBenchmarkManager().enable(Duration.ofMillis(10));

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new WorldGenCommand());
        commandManager.register(new SummonCommand());
        commandManager.register(new GamemodeCommand());
        commandManager.register(new TeleportCommand());
        commandManager.register(new DifficultyCommand());
    }
}