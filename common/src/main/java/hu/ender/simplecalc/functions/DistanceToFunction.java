package hu.ender.simplecalc.functions;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class DistanceToFunction extends ContextFunction {
    public DistanceToFunction(CommandContext<CommandSourceStack> context) {
        super(context, "distanceTo", 3);
    }

    @Override
    public double apply(double... args) {
        return Math.sqrt(ctx.getSource().getPosition().distanceToSqr(args[0], args[1], args[2]));
    }
}
