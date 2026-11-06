package quark.subcompass;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SoundTracker {
	public static final List<ActiveSound> SOUNDS = new ArrayList<>();
	private static final long EXPIRE_TIME_MS = 2000;

	public record ActiveSound(Vec3 position, SoundSource category, String name, long timestamp) {}

	public static void addSound(Vec3 pos, SoundSource category, String name) {
		SOUNDS.add(new ActiveSound(pos, category, name, System.currentTimeMillis()));
	}

	public static void tick() {
		long now = System.currentTimeMillis();
		SOUNDS.removeIf(sound -> now - sound.timestamp() > EXPIRE_TIME_MS);
	}
}