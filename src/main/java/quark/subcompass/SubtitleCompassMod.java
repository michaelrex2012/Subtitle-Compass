package quark.subcompass;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class SubtitleCompassMod implements ClientModInitializer {
    public static final String MOD_ID = "subcompass";

    @Override
    public void onInitializeClient() {
        // Safe 26.3 Registration using method reference
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "subtitle_compass_layer"),
                SubtitleCompassRenderer::drawCompass
        );
    }
}
