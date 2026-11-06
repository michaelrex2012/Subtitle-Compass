package quark.subcompass;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SubtitleCompassRenderer {
    private static final float COMPASS_RADIUS = 60.0f; // Distance from crosshair center

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        SoundTracker.tick();

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;

        Vec3 playerPos = player.getPosition();
        float playerYaw = Mth.wrapDegrees(Float(player.getY()));

        for (SoundTracker.ActiveSound sound : SoundTracker.SOUNDS) {
            double dx = sound.position().x - playerPos.x;
            double dz = sound.position().z - playerPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);

            // Calculate angle relative to player view
            double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            double relativeAngle = Mth.wrapDegrees((float)(targetAngle - playerYaw));
            double radians = Math.toRadians(relativeAngle);

            // Proximity length (closer sound = longer indicator bar)
            float maxDistance = 32.0f;
            float proximityFactor = 1.0f - (float) Math.min(distance / maxDistance, 1.0);
            int arcLengthPixels = Math.max(8, (int) (proximityFactor * 32.0f));

            // Get category color
            int color = getColorForCategory(sound.category());

            // Position indicator around the screen center circle
            int x = centerX + (int) (Math.sin(radians) * COMPASS_RADIUS);
            int y = centerY - (int) (Math.cos(radians) * COMPASS_RADIUS);

            // Draw indicator bar on screen
            graphics.fill(x - 2, y - (arcLengthPixels / 2), x + 2, y + (arcLengthPixels / 2), color);
        }
    }

    private static int getColorForCategory(SoundSource category) {
        return switch (category) {
            case HOSTILE -> 0xFFFF2222;  // Red for hostile mobs
            case PLAYERS -> 0xFF22FF22;  // Green for players
            case BLOCKS ->  0xFF2288FF;  // Blue for block sounds
            case NEUTRAL -> 0xFFFFFF22;  // Yellow for animals/neutral
            case AMBIENT -> 0xFFAAAAAA;  // Gray for ambient/weather
            default ->      0xFFFFFFFF;  // White default
        };
    }
}