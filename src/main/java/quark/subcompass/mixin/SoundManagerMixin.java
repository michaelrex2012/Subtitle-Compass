package quark.subcompass.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import quark.subcompass.SoundTracker;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void onPlaySound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound != null && sound.getSource() != null) {
            Vec3 pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            String soundId = sound.getIdentifier().toString(); // Corrected for 26.3
            SoundTracker.addSound(pos, sound.getSource(), soundId);
        }
    }
}