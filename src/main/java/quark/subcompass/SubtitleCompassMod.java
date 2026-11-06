package quark.subcompass;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;

//? if <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?} else {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//?}

public class SubtitleCompassMod implements ClientModInitializer {
    public static final String MOD_ID = "subcompass";

    @Override
    public void onInitializeClient() {
        //? if <26.1 {
        HudRenderCallback.EVENT.register(new SubtitleCompassRenderer());
        //?} else {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "compass_hud"),
                SubtitleCompassRenderer::render
        );
        //?}
    }
}