package me.fourteendoggo.minecore.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.goal.DoNothingGoal;
import net.minestom.server.entity.ai.goal.RandomLookAroundGoal;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.timer.Task;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Sheep extends EntityCreature {
    private final Task grassEatingTask;

    @SuppressWarnings("UnstableApiUsage")
    public Sheep() {
        super(EntityType.SHEEP);

        getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.23f);
        setHealth(8);

        List<GoalSelector> goalSelectors = Arrays.asList(
                new RandomLookAroundGoal(this, 30),
                new DoNothingGoal(this, 1500, 0.5f),
                new RandomStrollGoal(this, 16)
        );

        addAIGroup(goalSelectors, Collections.emptyList());

        grassEatingTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            Pos groundPos = position.withY(y -> y - 1);
            Block underneath = instance.getBlock(groundPos);

            if (underneath.registry().material() == Material.GRASS_BLOCK) {
                instance.setBlock(groundPos, Block.DIRT);
                EntityStatusPacket statusPacket = new EntityStatusPacket(getEntityId(), (byte) 10); // grass eating animation
                sendPacketToViewers(statusPacket);
            }
        }).delay(Duration.ofSeconds(2)).repeat(Duration.ofSeconds(60)).schedule();
    }

    @Override
    public void remove() {
        if (isRemoved()) return; // super.remove() doesn't return a flag so checking it twice
        super.remove();
        grassEatingTask.cancel();
    }
}
