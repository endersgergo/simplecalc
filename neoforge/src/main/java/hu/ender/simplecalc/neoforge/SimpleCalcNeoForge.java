package hu.ender.simplecalc.neoforge;

import hu.ender.simplecalc.SimpleCalc;
import net.neoforged.fml.common.Mod;

@Mod(SimpleCalc.MOD_ID)
public final class SimpleCalcNeoForge {
    public SimpleCalcNeoForge() {
        // Run our common setup.
        SimpleCalc.init();
    }
}
