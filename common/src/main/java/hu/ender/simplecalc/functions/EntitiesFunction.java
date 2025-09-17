package hu.ender.simplecalc.functions;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

public class EntitiesFunction extends ContextFunction {
    public EntitiesFunction(CommandContext<CommandSourceStack> context) {
        super(context, "entities", 1);
    }

    @Override
    public double apply(double... args) {
        if (!ctx.getSource().hasPermission(2))
            return Double.NaN;
        args[0] = Math.pow(args[0], 2);
        return ctx
                .getSource()
                .getLevel()
                .getEntities(
                        EntityTypeTest.forClass(Entity.class),
                        e -> e
                                .position()
                                .distanceToSqr(ctx
                                        .getSource()
                                        .getPosition()) <= args[0]).size();
    }
}
