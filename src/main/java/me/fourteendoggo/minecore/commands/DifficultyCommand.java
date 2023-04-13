package me.fourteendoggo.minecore.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.world.Difficulty;

import static me.fourteendoggo.minecore.util.SharedConstants.COLOR_SUCCESS;

public class DifficultyCommand extends Command {

    public DifficultyCommand() {
        super("difficulty");

        setDefaultExecutor((sender, commandString) -> {
            String currentDifficulty = MinecraftServer.getDifficulty().name().toLowerCase();
            sender.sendMessage(Component.text("The difficulty is " + currentDifficulty, COLOR_SUCCESS));
        });

        ArgumentEnum<Difficulty> difficultyArgument = ArgumentType.Enum("difficulty", Difficulty.class).setFormat(ArgumentEnum.Format.LOWER_CASED);

        addSyntax((sender, context) -> {
            Difficulty newDifficulty = context.get(difficultyArgument);
            MinecraftServer.setDifficulty(newDifficulty);
            sender.sendMessage(Component.text("The difficulty has been set to " + newDifficulty.name().toLowerCase(), COLOR_SUCCESS));
        }, difficultyArgument);
    }
}
