package pl.asie.environmentchecker;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import pl.asie.environmentchecker.tracker.TransformerTracker;

@Mod(modid = "environmentchecker", name = "EnvironmentChecker", version = "@VERSION@", acceptableRemoteVersions = "*", acceptableSaveVersions = "*", acceptedMinecraftVersions = "[1.10,2.0)")
public class EnvCheck {
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        TransformerTracker.INSTANCE.delaySaves = false;
        TransformerTracker.INSTANCE.save(true);
    }
}
