package me.fourteendoggo.minecore.commands;

import me.fourteendoggo.minecore.util.SharedConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

public class GamemodeCommand extends Command {
    public GamemodeCommand() {
        super("gamemode", "gm");

        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.text("Usage: /gamemode <mode> [player]", NamedTextColor.RED)));

        ArgumentEnum<GameMode> gameModeArgument = ArgumentType.Enum("gameMode", GameMode.class).setFormat(ArgumentEnum.Format.LOWER_CASED);
        ArgumentEntity playerArgument = ArgumentType.Entity("target").onlyPlayers(true);

        gameModeArgument.setCallback((sender, exception) -> sender.sendMessage(
                Component.text("Invalid gamemode ", NamedTextColor.RED)
                        .append(Component.text(exception.getInput(), NamedTextColor.WHITE))
                        .append(Component.text("!"))));

        // /gamemode <mode>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
                return;
            }
            if (player.getPermissionLevel() < 2) {
                player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return;
            }

            GameMode gameMode = context.get(gameModeArgument);
            executeSelf(player, gameMode);
        }, gameModeArgument);

        // gamemode <mode> <player>
        addSyntax((sender, context) -> {
            if (sender instanceof Player player && player.getPermissionLevel() < 2) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return;
            }

            GameMode gameMode = context.get(gameModeArgument);
            EntityFinder finder = context.get(playerArgument);

            executeOthers(sender, gameMode, finder.find(sender));
        }, gameModeArgument, playerArgument);
    }

    /**
     * Sets the gamemode for the executing player and notifies them in chat.
     */
    private void executeSelf(Player player, GameMode gameMode) {
        player.setGameMode(gameMode);

        Component gameModeComponent = getGameModeComponent(gameMode);
        player.sendMessage(Component.translatable("commands.gamemode.success.self", SharedConstants.COLOR_SUCCESS, gameModeComponent));
    }

    /**
     * Sets the gamemode for the specified entities and notifies them (and the sender) in chat.
     */
    private void executeOthers(CommandSender sender, GameMode gameMode, List<Entity> entities) {
        if (entities.isEmpty()) {
            Component notFoundComponent = sender instanceof Player
                    ? Component.translatable("argument.entity.notfound.player", NamedTextColor.RED)
                    : Component.text("No player was found", NamedTextColor.RED);
            sender.sendMessage(notFoundComponent);
            return;
        }

        for (Entity entity : entities) {
            if (!(entity instanceof Player targetPlayer)) continue;

            if (targetPlayer == sender) {
                executeSelf((Player) sender, gameMode);
                continue;
            }

            targetPlayer.setGameMode(gameMode);
            Component gameModeComponent = getGameModeComponent(gameMode);
            Component playerName = targetPlayer.getName();

            targetPlayer.sendMessage(Component.translatable("gamemode.changed", gameModeComponent));
            sender.sendMessage(Component.translatable("commands.gamemode.success.other", playerName, gameModeComponent));
        }
    }

    private Component getGameModeComponent(GameMode gameMode) {
        return Component.translatable("gameMode." + gameMode.name().toLowerCase());
    }
}
