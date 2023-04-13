package me.fourteendoggo.minecore.commands;

import me.fourteendoggo.minecore.entity.Sheep;
import me.fourteendoggo.minecore.util.SharedConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.goal.RandomLookAroundGoal;
import net.minestom.server.utils.location.RelativeVec;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class SummonCommand extends Command {

    @SuppressWarnings("ConstantConditions") // a player cannot execute a command without being in an instance
    public SummonCommand() {
        super("summon");

        ArgumentEntityType entityTypeArgument = ArgumentType.EntityType("entityType");
        ArgumentRelativeVec3 positionArgument = ArgumentType.RelativeVec3("position");
        positionArgument.setDefaultValue(new RelativeVec(
                Vec.ZERO,
                RelativeVec.CoordinateType.RELATIVE,
                true, true, true
        ));
        Argument<EntityClass> entityClassArgument = ArgumentType.Enum("class", EntityClass.class)
                .setFormat(ArgumentEnum.Format.LOWER_CASED)
                .setDefaultValue(EntityClass.CREATURE);

        setCondition(Conditions::playerOnly);
        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.text("Usage: /summon <type> [x y z] [class]", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            EntityType entityType = context.get(entityTypeArgument);
            EntityClass entityClass = context.get(entityClassArgument);

            Entity entityToSummon = entityClass.apply(entityType);
            Vec spawnPosition = context.get(positionArgument).fromSender(sender);

            entityToSummon.setInstance(player.getInstance(), spawnPosition).thenRun(() -> {
                // translate minecraft:cow to Cow
                String entityName = entityType.name().toLowerCase().replace(':', '.');
                Component entityNameComponent = Component.translatable("entity." + entityName);
                player.sendMessage(Component.translatable("commands.summon.success", SharedConstants.COLOR_SUCCESS, entityNameComponent));
            });
        }, entityTypeArgument, positionArgument, entityClassArgument);
    }

    private enum EntityClass {
        BASE(Entity::new),
        LIVING(LivingEntity::new),
        CREATURE(entityType -> {
            if (entityType == EntityType.SHEEP) {
                return new Sheep();
            }

            EntityCreature creature = new EntityCreature(entityType);
            List<GoalSelector> goalSelectors = Collections.singletonList(new RandomLookAroundGoal(creature, 20));
            creature.addAIGroup(goalSelectors, Collections.emptyList());
            return creature;
        });
        private final Function<EntityType, Entity> factory;

        EntityClass(Function<EntityType, Entity> factory) {
            this.factory = factory;
        }

        public Entity apply(EntityType type) {
            return factory.apply(type);
        }
    }
}
