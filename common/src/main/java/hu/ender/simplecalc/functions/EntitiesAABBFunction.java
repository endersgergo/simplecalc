package hu.ender.simplecalc.functions;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class EntitiesAABBFunction extends ContextFunction {
    public EntitiesAABBFunction(CommandContext<CommandSourceStack> ctx) {
        super(ctx, "entitiesAABB", 4);
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
                        ctx
                                .getSource()
                                .getEntity(),
                        AABB.encapsulatingFullBlocks(
                                new BlockPos((int)args[0],(int)args[1],(int)args[2]),
                                new BlockPos((int)args[3],(int)args[4],(int)args[5])),
                        x -> true)
                .size();
    }
}
