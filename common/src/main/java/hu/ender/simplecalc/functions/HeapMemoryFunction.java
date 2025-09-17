package hu.ender.simplecalc.functions;

import net.objecthunter.exp4j.function.Function;

public class HeapMemoryFunction extends Function {
    public HeapMemoryFunction() {
        super("heapMemory", 1);
    }

    @Override
    public double apply(double... args) {
        if (args[0] == 0)
            return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
        else
            return Runtime.getRuntime().totalMemory();
    }
}
