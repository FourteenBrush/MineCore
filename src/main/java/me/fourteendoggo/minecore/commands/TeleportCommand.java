package me.fourteendoggo.minecore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import net.minestom.server.utils.location.RelativeVec;

import static me.fourteendoggo.minecore.util.SharedConstants.COLOR_SUCCESS;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("teleport", "tp");

        ArgumentRelativeVec3 positionArgument = ArgumentType.RelativeVec3("position");
        ArgumentEntity playerArgument = ArgumentType.Entity("target").onlyPlayers(true);

        setCondition(Conditions::playerOnly);
        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.text("Usage: /teleport [x y z | entity]", NamedTextColor.RED)));

        // teleport <x y z>
        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            RelativeVec relativeVec = context.get(positionArgument);
            Pos position = player.getPosition().withCoord(relativeVec.from(player));

            player.teleport(position);
            player.sendMessage(Component.text("You have been teleported to " + position, COLOR_SUCCESS));
        }, positionArgument);

        // teleport <player>
        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            EntityFinder entityFinder = context.get(playerArgument);
            Player targetPlayer = entityFinder.findFirstPlayer(sender);
            assert targetPlayer != null;

            player.teleport(targetPlayer.getPosition());
            player.sendMessage(Component.text("Teleported to player ", COLOR_SUCCESS).append(targetPlayer.getName()));
        }, playerArgument);
    }
}
