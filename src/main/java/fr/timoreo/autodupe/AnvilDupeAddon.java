package fr.timoreo.autodupe;

import fr.timoreo.autodupe.modules.AnvilDupe;
import meteordevelopment.meteorclient.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public class AnvilDupeAddon extends MeteorAddon {
    public static final Logger LOG = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing ez meteor auto dupe");

        // Required when using @EventHandler
        MeteorClient.EVENT_BUS.registerLambdaFactory("fr.timoreo.autodupe", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // Modules
        Modules.get().add(new AnvilDupe());
    }
}
