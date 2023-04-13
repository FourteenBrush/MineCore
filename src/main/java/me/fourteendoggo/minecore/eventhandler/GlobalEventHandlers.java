package me.fourteendoggo.minecore.eventhandler;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.utils.time.TimeUnit;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static me.fourteendoggo.minecore.util.SharedConstants.COLOR_SUCCESS;

public class GlobalEventHandlers {
    private final InstanceContainer spawnInstance;
    private final Sidebar serverStatsSidebar;
    private TickMonitor lastTick;
    private final Map<UUID, BossBar> playerBossBars;

    public GlobalEventHandlers(InstanceContainer spawnInstance) {
        this.spawnInstance = spawnInstance;
        this.serverStatsSidebar = new Sidebar(Component.empty());
        this.playerBossBars = new WeakHashMap<>();

        serverStatsSidebar.createLine(new Sidebar.ScoreboardLine("ramUsage", Component.empty(), 1));
        serverStatsSidebar.createLine(new Sidebar.ScoreboardLine("tickTime", Component.empty(), 0));

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerLoginEvent.class, this::onPlayerLogin);
        globalEventHandler.addListener(PlayerSpawnEvent.class, this::onPlayerSpawn);
        globalEventHandler.addListener(PlayerBlockBreakEvent.class, this::onPlayerBlockBreak);
        globalEventHandler.addListener(ItemDropEvent.class, this::onItemDrop);
        globalEventHandler.addListener(PickupItemEvent.class, this::onPickupItem);
        globalEventHandler.addListener(ServerTickMonitorEvent.class, this::onServerTickMonitor);
        globalEventHandler.addListener(EntityAttackEvent.class, this::onEntityAttack);

        MinecraftServer.getSchedulerManager().buildTask(this::tickServerStats)
                .delay(Duration.ofMillis(500))
                .repeat(10, TimeUnit.SERVER_TICK)
                .schedule();
    }

    private void tickServerStats() {
        BenchmarkManager benchmarkManager = MinecraftServer.getBenchmarkManager();
        long ramUsage = benchmarkManager.getUsedMemory() / 1024 / 1024; // MB

        Component ramUsageComponent = Component.text("Ram usage: " + ramUsage + "mb", COLOR_SUCCESS);
        serverStatsSidebar.updateLineContent("ramUsage", ramUsageComponent);

        if (lastTick == null) return; // just to be sure
        double tickTime = Math.round(lastTick.getTickTime() * 100) / 100.0;
        Component tickTimeComponent = Component.text("Tick time: " + tickTime + "ms", COLOR_SUCCESS);
        serverStatsSidebar.updateLineContent("tickTime", tickTimeComponent);

        Audiences.players().sendPlayerListFooter(benchmarkManager.getCpuMonitoringMessage());
    }

    private void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        event.setSpawningInstance(spawnInstance);
        player.setRespawnPoint(findSpawnablePos());
        player.setPermissionLevel(4);
        player.setGameMode(GameMode.CREATIVE);
    }

    private Pos findSpawnablePos() {
        int spawnX = 0, spawnY = 62, spawnZ = 0;
        spawnInstance.loadChunk(spawnX, spawnZ).join();

        while (!spawnInstance.getBlock(spawnX, spawnY, spawnX).isAir()) {
            spawnY++;
        }
        return new Pos(spawnX, spawnY, spawnZ);
    }

    private void onPlayerSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        Player player = event.getPlayer();
        double tps = Math.min(MinecraftServer.TICK_PER_SECOND, Math.floor(1000 / lastTick.getTickTime()));
        int latency = player.getLatency();

        // <gray>TPS: <gray><green>20.00<green> <gray>Ping: <gray><yellow>123<yellow><gray>ms<gray>
        Component content = Component.text("TPS: ", NamedTextColor.GRAY)
                .append(Component.text(tps, NamedTextColor.GREEN))
                .append(Component.text(" Ping: ", NamedTextColor.GRAY))
                .append(Component.text(latency, NamedTextColor.YELLOW))
                .append(Component.text(" ms", NamedTextColor.GRAY));

        BossBar statsBossBar = playerBossBars.computeIfAbsent(player.getUuid(),
                uuid -> BossBar.bossBar(content, BossBar.MAX_PROGRESS, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_20));

        player.showBossBar(statsBossBar);
        serverStatsSidebar.addViewer(player);

        player.getInventory().addItemStack(ItemStack.of(Material.CRAFTING_TABLE));
        player.getInventory().addItemStack(ItemStack.of(Material.WHITE_WOOL, 64));
    }

    @SuppressWarnings("ConstantConditions")
    private void onItemDrop(ItemDropEvent event) {
        Player player = event.getPlayer();
        Pos playerPos = player.getPosition();
        ItemStack droppedItem = event.getItemStack();

        ItemEntity itemEntity = new ItemEntity(droppedItem);
        itemEntity.setPickupDelay(Duration.ofMillis(500));
        itemEntity.setInstance(player.getInstance(), playerPos.withY(y -> y + 1.5));
        itemEntity.setVelocity(playerPos.direction().mul(6));
    }

    @SuppressWarnings("ConstantConditions")
    private void onPlayerBlockBreak(PlayerBlockBreakEvent event) {
        Material material = event.getBlock().registry().material();
        ItemStack itemStack = ItemStack.of(material);
        event.getPlayer().getInventory().addItemStack(itemStack);
    }

    private void onPickupItem(PickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack itemStack = event.getItemEntity().getItemStack();
            boolean success = player.getInventory().addItemStack(itemStack);
            event.setCancelled(!success);
        }
    }

    private void onServerTickMonitor(ServerTickMonitorEvent event) {
        lastTick = event.getTickMonitor();
    }

    private void onEntityAttack(EntityAttackEvent event) {
        Entity source = event.getEntity();
        Entity damaged = event.getTarget();

        double someValue = source.getPosition().yaw() * (Math.PI / 180);
        damaged.takeKnockback(0.4f, Math.sin(someValue), -Math.cos(someValue));

        if (damaged instanceof LivingEntity livingEntity && source instanceof Player player) {
            int itemDamage = Math.max(1, player.getInventory().getItemInMainHand().meta().getDamage());
            System.out.println("Item damage: " + itemDamage);

            livingEntity.damage(DamageType.fromEntity(source), itemDamage);
        }
    }
}
