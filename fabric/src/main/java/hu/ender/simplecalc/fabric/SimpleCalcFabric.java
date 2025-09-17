package hu.ender.simplecalc.fabric;

import hu.ender.simplecalc.SimpleCalc;
import net.fabricmc.api.ModInitializer;

public class SimpleCalcFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleCalc.init();
    }
}
