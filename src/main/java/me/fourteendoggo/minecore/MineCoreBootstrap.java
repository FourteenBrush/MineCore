package me.fourteendoggo.minecore;

import net.minestom.server.MinecraftServer;

public class MineCoreBootstrap {
    public static void main(String[] args) throws Exception {
        MinecraftServer server = MinecraftServer.init();
        MineCore core = new MineCore();
        core.initialize();
        server.start("0.0.0.0", 25565);
    }
}
