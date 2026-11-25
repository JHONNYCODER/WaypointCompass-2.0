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
import java.util.ArrayList;

public class WaypointBarOverlay {
    public static boolean visible = true;
    private static final int MAX_WAYPOINTS = 8;
    private static long visibilityChangedAt;
    private static final long NOTIFICATION_DURATION = 2000L;

    public static void toggleVisibility() {
        visible = !visible;
        visibilityChangedAt = System.currentTimeMillis();
    }

    private static void renderVisibilityNotification(GuiGraphicsExtractor graphics, Minecraft mc) {
        if (visibilityChangedAt == 0L) {
            return;
        }

        long elapsed = System.currentTimeMillis() - visibilityChangedAt;

        if (elapsed >= NOTIFICATION_DURATION) {
            visibilityChangedAt = 0L;
            return;
        }

        String message = visible
            ? "Waypoint Compass: ON"
            : "Waypoint Compass: OFF";

        int color = visible ? 0xFF55FF55 : 0xFFFF5555;

        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 48;
        int textWidth = mc.font.width(message);

        graphics.fill(
            centerX - textWidth / 2 - 8,
            y - 4,
            centerX + textWidth / 2 + 8,
            y + 13,
            0xCC202020
        );

        graphics.centeredText(
            mc.font,
            Component.literal(message),
            centerX,
            y,
            color
        );
    }


    private static final Map<Waypoint, Float> layoutPositions = new IdentityHashMap<>();
    private static final Map<Waypoint, Float> resolvedPositions = new IdentityHashMap<>();
    private static final Map<Waypoint, Integer> markerRows = new IdentityHashMap<>();

    private static final Map<Waypoint, Float> groupAnchorPositions = new IdentityHashMap<>();
    private static final List<List<Waypoint>> collisionGroups = new ArrayList<>();


    private static final float MARKER_GAP = 8.0f;
    private static final float COLLISION_RELEASE_GAP = 24.0f;
    private static final float BASE_ROW_HEIGHT = 30.0f;

    private static long lastRenderTime;

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        renderVisibilityNotification(graphics, mc);

        if (!visible) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();

        if (waypoints.isEmpty()) {
            layoutPositions.clear();
            resolvedPositions.clear();
            markerRows.clear();
            collisionGroups.clear();
            groupAnchorPositions.clear();
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

        // Calculate frame time for smooth movement.
        long currentTime = System.nanoTime();

        float deltaTime = lastRenderTime == 0L
            ? 0.016f
            : Math.min((currentTime - lastRenderTime) / 1_000_000_000.0f, 0.1f);

        lastRenderTime = currentTime;

        float smoothing = 1.0f - (float)Math.exp(-12.0f * deltaTime);

        int visibleRange = 80;
        int screenMargin = 12;

        // Calculate the desired position and width of every marker.
        layoutPositions.clear();

        Map<Waypoint, Integer> markerWidths = new IdentityHashMap<>();

        for (Waypoint wp : sorted) {
            float bearing = wp.getBearing(playerPos);
            float diff = bearing - ((playerYaw + 180.0f) % 360.0f);

            if (diff < -180.0f) {
                diff += 360.0f;
            }

            if (diff > 180.0f) {
                diff -= 360.0f;
            }

            double dist = wp.distanceTo(playerPos);

            float targetX = centerX + diff * 2.0f;

            int markerWidth = getMarkerWidth(
                wp,
                mc.font,
                dist,
                screenWidth - screenMargin * 2
            );

            float halfWidth = markerWidth / 2.0f;

            targetX = Math.max(
                screenMargin + halfWidth,
                Math.min(screenWidth - screenMargin - halfWidth, targetX)
            );

            layoutPositions.put(wp, targetX);
            markerWidths.put(wp, markerWidth);
        }

        // Update persistent collision groups after calculating marker positions.
        markerRows.keySet().retainAll(sorted);
        groupAnchorPositions.keySet().retainAll(sorted);
        resolvedPositions.clear();

        updateCollisionGroups(sorted, markerWidths);

        // Calculate the layout using shared group anchors.
        List<List<Waypoint>> rows = new ArrayList<>();

        int availableWidth = screenWidth - screenMargin * 2;

        for (List<Waypoint> group : collisionGroups) {
            if (group.isEmpty()) {
                continue;
            }

            float targetAnchor = 0.0f;

            for (Waypoint wp : group) {
                targetAnchor += layoutPositions.get(wp);
            }

            targetAnchor /= group.size();

            Waypoint anchorWaypoint = group.get(0);

            float previousAnchor = groupAnchorPositions.getOrDefault(
                anchorWaypoint,
                targetAnchor
            );

            float currentAnchor = previousAnchor
                + (targetAnchor - previousAnchor) * smoothing;

            groupAnchorPositions.put(anchorWaypoint, currentAnchor);

            // Divide groups that cannot fit into the available screen width.
            List<List<Waypoint>> sections = new ArrayList<>();
            List<Waypoint> section = new ArrayList<>();
            int sectionWidth = 0;

            for (Waypoint wp : group) {
                int width = markerWidths.get(wp);

                int requiredWidth = section.isEmpty()
                    ? width
                    : sectionWidth + (int)MARKER_GAP + width;

                if (!section.isEmpty() && requiredWidth > availableWidth) {
                    sections.add(section);
                    section = new ArrayList<>();
                    sectionWidth = 0;
                }

                section.add(wp);

                sectionWidth = section.size() == 1
                    ? width
                    : sectionWidth + (int)MARKER_GAP + width;
            }

            if (!section.isEmpty()) {
                sections.add(section);
            }

            // Place each section without overlapping other markers.
            for (List<Waypoint> currentSection : sections) {
                int totalWidth = 0;

                for (Waypoint wp : currentSection) {
                    totalWidth += markerWidths.get(wp);
                }

                totalWidth += (currentSection.size() - 1) * (int)MARKER_GAP;

                float sectionCenter = Math.max(
                    screenMargin + totalWidth / 2.0f,
                    Math.min(
                        screenWidth - screenMargin - totalWidth / 2.0f,
                        currentAnchor
                    )
                );

                int selectedRow = 0;
                boolean collision;

                do {
                    collision = false;

                    float cursor = sectionCenter - totalWidth / 2.0f;

                    for (Waypoint wp : currentSection) {
                        float width = markerWidths.get(wp);
                        float proposedX = cursor + width / 2.0f;
                        float halfWidth = width / 2.0f;

                        if (selectedRow < rows.size()) {
                            for (Waypoint other : rows.get(selectedRow)) {
                                float otherX = resolvedPositions.get(other);
                                float otherHalf = markerWidths.get(other) / 2.0f;

                                if (Math.abs(proposedX - otherX)
                                    < halfWidth + otherHalf + MARKER_GAP) {
                                    collision = true;
                                    break;
                                }
                            }
                        }

                        if (collision) {
                            break;
                        }

                        cursor += width + MARKER_GAP;
                    }

                    if (collision) {
                        selectedRow++;
                    }

                } while (collision);

                while (rows.size() <= selectedRow) {
                    rows.add(new ArrayList<>());
                }

                float cursor = sectionCenter - totalWidth / 2.0f;

                for (Waypoint wp : currentSection) {
                    float width = markerWidths.get(wp);
                    float markerX = cursor + width / 2.0f;

                    resolvedPositions.put(wp, markerX);
                    markerRows.put(wp, selectedRow);
                    rows.get(selectedRow).add(wp);

                    cursor += width + MARKER_GAP;
                }
            }
        }

        float maxWaypointSize = 1.0f;

        for (Waypoint wp : sorted) {
            float size = Math.max(0.5f, Math.min(2.0f, wp.size));
            maxWaypointSize = Math.max(maxWaypointSize, size);
        }

        int rowHeight = Math.max(
            (int)BASE_ROW_HEIGHT,
            (int)Math.ceil(21.0f + (8.0f * maxWaypointSize * 0.8f) + 5.0f)
        );

        for (Waypoint wp : sorted) {
            int markerY = barY + markerRows.getOrDefault(wp, 0) * rowHeight;

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

            // Use the final collision-resolved and smoothed position.
            int xPos = Math.round(resolvedPositions.get(wp));

            // Determine whether the waypoint is outside the normal compass range.
            boolean offscreen = Math.abs(diff) >= visibleRange;

            // Keep distant directional indicators visible.
            float angularFade = Math.max(
                0.35f,
                1.0f - Math.abs(diff) / 180.0f
            );

            float opacity = angularFade;

            int alpha = (int)(opacity * 255.0f);
            int colorWithAlpha = alpha << 24 | wp.color & 0xFFFFFF;

            // Render the waypoint icon using its configured size.
            float waypointSize = Math.max(
                0.5f,
                Math.min(2.0f, wp.size)
            );

            wp.icon.render(
                graphics,
                mc.font,
                xPos,
                markerY + 5,
                colorWithAlpha,
                0.8f * waypointSize
            );

            // Show a vertical arrow when the waypoint is within 30 blocks.
            if (dist <= 30.0) {
                String verticalArrow;

                if (wp.y > playerPos.y) {
                    verticalArrow = "↑";
                } else if (wp.y < playerPos.y) {
                    verticalArrow = "↓";
                } else {
                    verticalArrow = "↕";
                }

                // Blink the arrow when the waypoint is within 5 blocks.
                boolean blinkVisible = dist > 5.0
                    || (System.currentTimeMillis() / 350L) % 2L == 0L;

                if (blinkVisible) {
                    int arrowWidth = mc.font.width(verticalArrow);

                    graphics.text(
                        mc.font,
                        Component.literal(verticalArrow),
                        xPos - arrowWidth / 2,
                        markerY - 1,
                        colorWithAlpha,
                        true
                    );
                }
            }

            // Draw a directional arrow for off-screen waypoints.
            if (offscreen && dist > 30.0) {
                String arrow = diff < 0.0f ? "◀" : "▶";

                graphics.text(
                    mc.font,
                    Component.literal(arrow),
                    xPos - 4,
                    markerY - 1,
                    colorWithAlpha,
                    true
                );
            }

            String displayName = fitText(
                wp.name,
                mc.font,
                screenWidth - screenMargin * 2
            );

            int nameX = xPos - mc.font.width(displayName) / 2;

            graphics.text(
                mc.font,
                Component.literal(displayName),
                nameX,
                markerY + 12,
                colorWithAlpha,
                true
            );

            // Draw distance.
            String distStr = formatDistance(dist);

            int distX = xPos - mc.font.width(distStr) / 2;

            graphics.text(
                mc.font,
                Component.literal(distStr),
                distX,
                markerY + 21,
                alpha << 24 | 0xAAAAAA,
                true
            );
        }
    }

    private static void updateCollisionGroups(
        List<Waypoint> activeWaypoints,
        Map<Waypoint, Integer> markerWidths
    ) {
        // Remove inactive waypoints from existing groups.
        for (int i = collisionGroups.size() - 1; i >= 0; i--) {
            List<Waypoint> group = collisionGroups.get(i);

            group.removeIf(wp -> !activeWaypoints.contains(wp));

            group.sort(
                Comparator.comparingDouble(
                    wp -> layoutPositions.get(wp)
                )
            );

            if (group.isEmpty()) {
                collisionGroups.remove(i);
            }
        }

        // Release members when their desired positions have enough clearance.
        for (int i = collisionGroups.size() - 1; i >= 0; i--) {
            List<Waypoint> group = collisionGroups.get(i);

            if (group.size() < 2) {
                continue;
            }

            List<List<Waypoint>> splitGroups = new ArrayList<>();
            List<Waypoint> currentGroup = new ArrayList<>();

            for (Waypoint wp : group) {
                if (!currentGroup.isEmpty()) {
                    Waypoint previous = currentGroup.get(currentGroup.size() - 1);

                    float requiredDistance = (
                        markerWidths.get(wp) + markerWidths.get(previous)
                    ) / 2.0f + COLLISION_RELEASE_GAP;

                    float actualDistance = Math.abs(
                        layoutPositions.get(wp) - layoutPositions.get(previous)
                    );

                    if (actualDistance >= requiredDistance) {
                        splitGroups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                    }
                }

                currentGroup.add(wp);
            }

            if (!currentGroup.isEmpty()) {
                splitGroups.add(currentGroup);
            }

            if (splitGroups.size() > 1) {
                collisionGroups.remove(i);
                collisionGroups.addAll(splitGroups);
            }
        }

        // Add waypoints that do not currently belong to a group.
        for (Waypoint wp : activeWaypoints) {
            boolean found = false;

            for (List<Waypoint> group : collisionGroups) {
                if (group.contains(wp)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                List<Waypoint> group = new ArrayList<>();
                group.add(wp);
                collisionGroups.add(group);
            }
        }

        // Merge groups when their markers collide.
        boolean merged;

        do {
            merged = false;

            for (int i = 0; i < collisionGroups.size() && !merged; i++) {
                List<Waypoint> first = collisionGroups.get(i);

                for (int j = i + 1; j < collisionGroups.size(); j++) {
                    List<Waypoint> second = collisionGroups.get(j);

                    boolean collision = false;

                    for (Waypoint a : first) {
                        for (Waypoint b : second) {
                            float requiredDistance = (
                                markerWidths.get(a) + markerWidths.get(b)
                            ) / 2.0f + MARKER_GAP;

                            if (Math.abs(
                                layoutPositions.get(a) - layoutPositions.get(b)
                            ) < requiredDistance) {
                                collision = true;
                                break;
                            }
                        }

                        if (collision) {
                            break;
                        }
                    }

                    if (collision) {
                        List<Waypoint> combined = new ArrayList<>(first);
                        combined.addAll(second);

                        combined.sort(
                            Comparator.comparingDouble(
                                wp -> layoutPositions.get(wp)
                            )
                        );

                        collisionGroups.set(i, combined);
                        collisionGroups.remove(j);

                        merged = true;
                        break;
                    }
                }
            }
        } while (merged);

        // Keep groups ordered by their leftmost member.
        collisionGroups.sort(
            Comparator.comparingDouble(
                group -> layoutPositions.get(group.get(0))
            )
        );

        // Remove obsolete group anchors.
        List<Waypoint> activeAnchors = new ArrayList<>();

        for (List<Waypoint> group : collisionGroups) {
            if (!group.isEmpty()) {
                activeAnchors.add(group.get(0));
            }
        }

        groupAnchorPositions.keySet().retainAll(activeAnchors);
    }

    private static int getMarkerWidth(
        Waypoint wp,
        net.minecraft.client.gui.Font font,
        double dist,
        int maxWidth
    ) {
        String displayName = fitText(wp.name, font, maxWidth - (int)MARKER_GAP);

        int nameWidth = font.width(displayName);
        int distanceWidth = font.width(formatDistance(dist));
        float waypointSize = Math.max(0.5f, Math.min(2.0f, wp.size));

        int iconWidth = Math.max(
            10,
            (int)Math.ceil(12.0f * waypointSize)
        );

        int arrowWidth = Math.max(
            Math.max(font.width("◀"), font.width("▶")),
            Math.max(
                font.width("↑"),
                Math.max(font.width("↓"), font.width("↕"))
            )
        );

        int contentWidth = Math.max(
            Math.max(nameWidth, distanceWidth),
            Math.max(iconWidth, arrowWidth)
        );

        return Math.min(contentWidth + (int)MARKER_GAP, maxWidth);
    }

    private static String fitText(
        String text,
        net.minecraft.client.gui.Font font,
        int maxWidth
    ) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int availableWidth = maxWidth - font.width(ellipsis);

        if (availableWidth <= 0) {
            return ellipsis;
        }

        StringBuilder result = new StringBuilder(text);

        while (!result.isEmpty() && font.width(result.toString()) > availableWidth) {
            result.deleteCharAt(result.length() - 1);
        }

        return result + ellipsis;
    }

    private static String formatDistance(double dist) {
        if (dist < 1000.0) {
            return (int)dist + "m";
        }
        return String.format("%.1fkm", dist / 1000.0);
    }
}