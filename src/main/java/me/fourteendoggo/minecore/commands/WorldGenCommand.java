package me.fourteendoggo.minecore.commands;

import me.fourteendoggo.minecore.generation.WorldGenMutator;
import me.fourteendoggo.minecore.util.SharedConstants;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;

public class WorldGenCommand extends Command {
    private static final double SCALE_INCREMENT = 0.02;

    public WorldGenCommand() {
        super("worldgen", "gen");

        ArgumentWord actionArgument = ArgumentType.Word("action").from("scaleup", "scaledown", "toggletrees");

        setCondition(Conditions::playerOnly);

        addSyntax((sender, context) -> {
            Player player = (Player) sender;

            switch (context.get(actionArgument)) {
                case "scaleup" -> {
                    double newScale = WorldGenMutator.changeNoiseScale(SCALE_INCREMENT);
                    player.sendMessage(Component.text("New scale is now " + newScale, SharedConstants.COLOR_SUCCESS));
                }
                case "scaledown" -> {
                    double newScale = WorldGenMutator.changeNoiseScale(-SCALE_INCREMENT);
                    player.sendMessage(Component.text("New scale is now " + newScale, SharedConstants.COLOR_SUCCESS));
                }
                case "toggletrees" -> {
                    String state = WorldGenMutator.setTreeGeneration() ? "ON" : "OFF";
                    player.sendMessage(Component.text("Tree generation is now " + state, SharedConstants.COLOR_SUCCESS));
                }
            }
        }, actionArgument);
    }
}
