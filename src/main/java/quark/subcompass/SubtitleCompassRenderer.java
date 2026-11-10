package quark.subcompass;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubtitleCompassRenderer {
    private static final float COMPASS_RADIUS = 60.0f;

    // Caches dynamically generated head textures to ensure smooth 60+ FPS performance
    private static final Map<String, Identifier> HEAD_TEXTURE_CACHE = new HashMap<>();

    public static void drawCompass(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        SoundTracker.tick();

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;

        Vec3 playerPos = player.getEyePosition();
        float playerYaw = Mth.wrapDegrees(player.getYRot());

        for (SoundTracker.ActiveSound sound : SoundTracker.SOUNDS) {
            double dx = sound.position().x - playerPos.x;
            double dz = sound.position().z - playerPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);

            // Corrected angle calculation
            double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            double relativeAngle = Mth.wrapDegrees((float)(playerYaw - targetAngle));
            double radians = Math.toRadians(relativeAngle);

            float maxDistance = 32.0f;
            float proximityFactor = 1.0f - (float) Math.min(distance / maxDistance, 1.0);

            int color = getColorForCategory(sound.category());

            // 1. SOLID CRESCENT ARC
            float angularSpan = 0.18f + (proximityFactor * 0.15f);
            float step = 0.01f;

            for (float a = -angularSpan; a <= angularSpan; a += step) {
                double currentRadians = radians + a;
                int x = centerX + (int) (Math.sin(currentRadians) * COMPASS_RADIUS);
                int y = centerY - (int) (Math.cos(currentRadians) * COMPASS_RADIUS);

                graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
            }

            // 2. SOLID ARROWHEAD POINTER
            int pointerRadius = (int) (COMPASS_RADIUS + 8 + (proximityFactor * 4));
            int tipX = centerX + (int) (Math.sin(radians) * pointerRadius);
            int tipY = centerY - (int) (Math.cos(radians) * pointerRadius);

            for (int ox = -2; ox <= 2; ox++) {
                for (int oy = -2; oy <= 2; oy++) {
                    if (Math.sqrt(ox * ox + oy * oy) <= 2.2) {
                        graphics.fill(tipX + ox, tipY + oy, tipX + ox + 1, tipY + oy + 1, color);
                    }
                }
            }

            // 3. ICON RENDERING
            float innerRadius = COMPASS_RADIUS - 16.0f;
            int iconX = centerX + (int) (Math.sin(radians) * innerRadius);
            int iconY = centerY - (int) (Math.cos(radians) * innerRadius);

            renderMobIcon(graphics, client, sound.name(), sound.category(), iconX - 8, iconY - 8);
        }
    }

    private static void renderMobIcon(GuiGraphicsExtractor graphics, Minecraft client, String soundId, SoundSource category, int x, int y) {
        String skinPath = resolveEntityTexturePath(client, soundId);

        if (skinPath != null) {
            Identifier headTexId = HEAD_TEXTURE_CACHE.computeIfAbsent(skinPath, path -> {
                try {
                    Identifier fullTexRes = Identifier.fromNamespaceAndPath("minecraft", path);
                    Optional<net.minecraft.server.packs.resources.Resource> resourceOpt = client.getResourceManager().getResource(fullTexRes);

                    if (resourceOpt.isPresent()) {
                        try (InputStream inputStream = resourceOpt.get().open();
                             NativeImage skinImage = NativeImage.read(inputStream)) {

                            int faceX = 8;
                            int faceY = 8;
                            int faceSize = 8;

                            NativeImage headImage = new NativeImage(faceSize, faceSize, true);

                            for (int hx = 0; hx < faceSize; hx++) {
                                for (int hy = 0; hy < faceSize; hy++) {
                                    int color = skinImage.getPixel(faceX + hx, faceY + hy);
                                    headImage.setPixel(hx, hy, color);
                                }
                            }

                            DynamicTexture dynamicTexture = new DynamicTexture(() -> "subcompass:dynamic_head_" + path.hashCode(), headImage);
                            Identifier cachedId = Identifier.fromNamespaceAndPath("subcompass", "dynamic_head_" + path.hashCode());
                            client.getTextureManager().register(cachedId, dynamicTexture);
                            return cachedId;
                        }
                    }
                } catch (Exception ignored) {
                }
                return null;
            });

            if (headTexId != null) {
                // FIXED: Explicitly pass RenderPipelines.GUI_TEXTURED to prevent giant colored box artifacts
                graphics.blit(RenderPipelines.GUI_TEXTURED, headTexId, x, y, 16, 16, 0, 0, 8, 8, 8, 8);
                return;
            }
        }

        // Fallback to item stack icon if texture resolution fails
        ItemStack fallbackStack = getItemFromSoundId(soundId, category);
        graphics.item(fallbackStack, x, y);
    }

    private static String resolveEntityTexturePath(Minecraft client, String soundId) {
        if (soundId == null || !soundId.contains("entity.")) return null;

        String path = soundId.contains(":") ? soundId.split(":")[1] : soundId;
        String[] tokens = path.split("[._]");

        String entityName = "";
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("entity") && i + 1 < tokens.length) {
                entityName = tokens[i + 1];
                if (i + 2 < tokens.length && !isActionWord(tokens[i + 2])) {
                    entityName += "_" + tokens[i + 2];
                }
                break;
            }
        }

        if (entityName.isEmpty()) return null;

        String[] candidatePaths = {
                "textures/entity/" + entityName + "/" + entityName + ".png",
                "textures/entity/" + entityName + ".png"
        };

        for (String candidate : candidatePaths) {
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", candidate);
            if (client.getResourceManager().getResource(id).isPresent()) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isActionWord(String word) {
        return List.of("ambient", "hurt", "death", "step", "hit", "attack", "flap", "swim", "say").contains(word);
    }

    private static ItemStack getItemFromSoundId(String soundId, SoundSource category) {
        if (soundId == null) return new ItemStack(Items.COMPASS);
        String path = soundId.contains(":") ? soundId.split(":")[1] : soundId;
        String[] tokens = path.split("[._]");

        List<String> ignoredWords = List.of("minecraft", "entity", "block", "item", "ambient", "living", "step", "hit", "hurt", "death");

        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i].toLowerCase();
            if (ignoredWords.contains(token)) continue;

            Identifier directId = Identifier.fromNamespaceAndPath("minecraft", token);
            var directHolder = BuiltInRegistries.ITEM.get(directId);
            if (directHolder.isPresent()) return new ItemStack(directHolder.get());
        }

        return switch (category) {
            case HOSTILE -> new ItemStack(Items.ZOMBIE_HEAD);
            case PLAYERS -> new ItemStack(Items.PLAYER_HEAD);
            case BLOCKS  -> new ItemStack(Items.BRICKS);
            case NEUTRAL -> new ItemStack(Items.PORKCHOP);
            case AMBIENT -> new ItemStack(Items.FEATHER);
            default      -> new ItemStack(Items.COMPASS);
        };
    }

    private static int getColorForCategory(SoundSource category) {
        return switch (category) {
            case HOSTILE -> 0xFFFF2222;
            case PLAYERS -> 0xFF22FF22;
            case BLOCKS  -> 0xFF2288FF;
            case NEUTRAL -> 0xFFFFFF22;
            case AMBIENT -> 0xFFAAAAAA;
            default      -> 0xFFFFFFFF;
        };
    }
}