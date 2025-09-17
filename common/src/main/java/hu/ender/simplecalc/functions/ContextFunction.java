package hu.ender.simplecalc.functions;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.objecthunter.exp4j.function.Function;

public abstract class ContextFunction extends Function {
    protected final CommandContext<CommandSourceStack> ctx;
    protected ContextFunction(CommandContext<CommandSourceStack> ctx, String name, int argc) {
        super(name, argc);
        this.ctx = ctx;
    }
}
