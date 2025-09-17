package hu.ender.simplecalc.functions;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class TpsFunction extends ContextFunction {
    public TpsFunction(CommandContext<CommandSourceStack> ctx) {
        super(ctx, "tps", 0);
    }

    @Override
    public double apply(double... args) {
        return ctx.getSource().getServer().tickRateManager().tickrate();
    }
}
