/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.phys.Vec3
 */
package net.waypointcompass.mod;

import java.util.Comparator;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.waypointcompass.mod.Waypoint;
import net.waypointcompass.mod.WaypointManager;

public class WaypointBarOverlay {
    public static boolean visible = true;
    private static final int MAX_WAYPOINTS = 8;

    private static final Map<Waypoint, Float> smoothedPositions = new IdentityHashMap<>();
    private static long lastRenderTime;

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!visible) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();

        if (waypoints.isEmpty()) {
            smoothedPositions.clear();
            return;
        }

        float partialTick = delta.getGameTimeDeltaPartialTick(false);

        int screenWidth = graphics.guiWidth();
        int centerX = screenWidth / 2;
        int barY = 2;

        Vec3 playerPos = mc.player.getPosition(partialTick);
        float playerYaw = mc.player.getYRot(partialTick);

        String currentDim = mc.level.dimension().identifier().toString();

        List<Waypoint> sorted = waypoints.stream()
            .filter(wp -> wp.visible && (wp.dimension == null || wp.dimension.equals(currentDim)))
            .sorted(Comparator.comparingDouble(wp -> wp.distanceTo(playerPos)))
            .limit(MAX_WAYPOINTS)
            .toList();

        smoothedPositions.keySet().retainAll(sorted);

        // Calculate frame time for smooth movement.
        long currentTime = System.nanoTime();

        float deltaTime = lastRenderTime == 0L
            ? 0.016f
            : Math.min((currentTime - lastRenderTime) / 1_000_000_000.0f, 0.1f);

        lastRenderTime = currentTime;

        float smoothing = 1.0f - (float)Math.exp(-12.0f * deltaTime);

        int visibleRange = 80;
        int screenMargin = 12;

        for (Waypoint wp : sorted) {

            float wpBearing = wp.getBearing(playerPos);

            float diff = wpBearing - ((playerYaw + 180.0f) % 360.0f);

            // Normalize the angle to -180 through +180.
            if (diff < -180.0f) {
                diff += 360.0f;
            }

            if (diff > 180.0f) {
                diff -= 360.0f;
            }

            double dist = wp.distanceTo(playerPos);

            float proxFade = Math.max(
                0.0f,
                Math.min(1.0f, (float)((dist - 5.0) / 5.0))
            );

            // Calculate the waypoint's actual screen position.
            float actualX = centerX + diff * 2.0f;

            // Keep the marker inside the screen.
            float targetX = Math.max(
                screenMargin,
                Math.min(screenWidth - screenMargin, actualX)
            );

            // Remember the previous position and interpolate towards the target.
            float previousX = smoothedPositions.computeIfAbsent(
                wp,
                key -> targetX
            );

            float currentX = previousX + (targetX - previousX) * smoothing;

            smoothedPositions.put(wp, currentX);

            int xPos = Math.round(currentX);

            // Determine whether the waypoint is outside the normal compass range.
            boolean offscreen = Math.abs(diff) >= visibleRange;

            // Keep distant directional indicators visible.
            float angularFade = Math.max(
                0.35f,
                1.0f - Math.abs(diff) / 180.0f
            );

            float opacity = angularFade * proxFade;

            int alpha = (int)(opacity * 255.0f);
            int colorWithAlpha = alpha << 24 | wp.color & 0xFFFFFF;

            // Draw the waypoint icon.
            wp.icon.render(
                graphics,
                mc.font,
                xPos,
                barY + 5,
                colorWithAlpha,
                0.8f
            );

            // Draw a directional arrow for off-screen waypoints.
            if (offscreen) {
                String arrow = diff < 0.0f ? "◀" : "▶";

                graphics.text(
                    mc.font,
                    Component.literal(arrow),
                    xPos - 4,
                    barY - 1,
                    colorWithAlpha,
                    true
                );
            }

            // Keep the waypoint name inside the screen.
            int nameX = xPos - mc.font.width(wp.name) / 2;

            nameX = Math.max(
                2,
                Math.min(screenWidth - mc.font.width(wp.name) - 2, nameX)
            );

            graphics.text(
                mc.font,
                Component.literal(wp.name),
                nameX,
                barY + 12,
                colorWithAlpha,
                true
            );

            // Draw distance.
            String distStr = formatDistance(dist);

            int distX = xPos - mc.font.width(distStr) / 2;

            distX = Math.max(
                2,
                Math.min(screenWidth - mc.font.width(distStr) - 2, distX)
            );

            graphics.text(
                mc.font,
                Component.literal(distStr),
                distX,
                barY + 21,
                alpha << 24 | 0xAAAAAA,
                true
            );
        }
    }

    private static String formatDistance(double dist) {
        if (dist < 1000.0) {
            return (int)dist + "m";
        }
        return String.format("%.1fkm", dist / 1000.0);
    }
}

