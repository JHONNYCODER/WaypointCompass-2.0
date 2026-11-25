/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 */
package net.waypointcompass.mod.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.waypointcompass.mod.Waypoint;
import net.waypointcompass.mod.WaypointBarOverlay;
import net.waypointcompass.mod.WaypointManager;
import net.waypointcompass.mod.screen.NewWaypointScreen;

public class WaypointMenuScreen
extends Screen {
    private int scrollOffset = 0;
    private static final int ENTRY_H = 20;

    public WaypointMenuScreen() {
        super((Component)Component.literal((String)"Waypoints"));
    }

    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"+ New"), b -> this.minecraft.gui.setScreen((Screen)new NewWaypointScreen())).bounds(cx - 80, this.height - 45, 70, 20).build());
        String t = WaypointBarOverlay.visible ? "Hide Bar" : "Show Bar";
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)t), b -> {
            WaypointBarOverlay.visible = !WaypointBarOverlay.visible;
            this.init();
        }).bounds(cx - 5, this.height - 45, 80, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Close"), b -> this.close()).bounds(cx + 80, this.height - 45, 60, 20).build());
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.centeredText(this.font, (Component)Component.literal((String)("Waypoints (" + (WaypointBarOverlay.visible ? "shown" : "hidden") + ")")), cx, 8, 0xFFFFFF);
        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();
        int startY = 22;
        int endY = this.height - 55;
        int listW = this.width - 4;
        if (waypoints.isEmpty()) {
            graphics.centeredText(this.font, (Component)Component.literal((String)"No waypoints yet. Press N to add one."), cx, startY + 20, 0x888888);
            return;
        }
        int contentHeight = waypoints.size() * 20;
        if (contentHeight > endY - startY) {
            int sbH = Math.max(16, (int)((float)(endY - startY) / (float)contentHeight * (float)(endY - startY)));
            int sbY = startY + (int)((float)this.scrollOffset / (float)contentHeight * (float)(endY - startY));
            graphics.fill(this.width - 6, sbY, this.width - 2, sbY + sbH, 0x66FFFFFF);
        }
        graphics.enableScissor(0, startY, this.width, endY);
        int colName = 22;
        int colCoords = 140;
        int colDist = 270;
        int btnRight = listW - 12;
        graphics.text(this.font, (Component)Component.literal((String)"Name"), colName, startY - 10, 0x888888, true);
        graphics.text(this.font, (Component)Component.literal((String)"Coords"), colCoords, startY - 10, 0x888888, true);
        for (int i = 0; i < waypoints.size(); ++i) {
            Waypoint wp = waypoints.get(i);
            int y = startY + i * 20 - this.scrollOffset;
            if (y < startY - 20 || y > endY) continue;
            boolean hovered = mouseY >= y && mouseY <= y + 20 && mouseX > 2 && mouseX < listW;
            graphics.fill(2, y, listW, y + 20, hovered ? 0x44888888 : 0x22666666);
            int a = wp.visible ? 255 : 80;
            int nameColor = a << 24 | wp.color & 0xFFFFFF;
            int muted = a << 24 | 0x999999;
            wp.icon.render(graphics, this.font, 14, y + 10, nameColor, 1.0f);
            Object name = wp.name;
            int maxNameW = colCoords - colName - 4;
            if (this.font.width((String)name) > maxNameW) {
                name = this.font.plainSubstrByWidth((String)name, maxNameW - 2) + "...";
            }
            graphics.text(this.font, (Component)Component.literal((String)name), colName, y + 6, nameColor, true);
            String coords = String.format("%.0f %.0f %.0f", wp.x, wp.y, wp.z);
            graphics.text(this.font, (Component)Component.literal((String)coords), colCoords, y + 6, muted, true);
            int dist = this.getDistanceToPlayer(wp);
            Object distStr = dist >= 0 ? dist + "m" : "?";
            graphics.text(this.font, (Component)Component.literal((String)distStr), colDist, y + 6, muted, true);
            int by = y + 3;
            this.drawBtn(graphics, "Edit", btnRight - 110, by, 36, mouseX, mouseY, -2008774401);
            this.drawBtn(graphics, wp.visible ? "Hide" : "Show", btnRight - 72, by, 36, mouseX, mouseY, wp.visible ? -1996510720 : 0x66666666);
            this.drawBtn(graphics, "Del", btnRight - 34, by, 28, mouseX, mouseY, -1996536764);
        }
        graphics.disableScissor();
    }

    private void drawBtn(GuiGraphicsExtractor g, String label, int x, int y, int w, int mx, int my, int bg) {
        boolean over = mx >= x && mx <= x + w && my >= y && my <= y + 14;
        g.fill(x, y, x + w, y + 14, over ? -6710887 : bg);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, over ? -11184811 : 0x44000000);
        g.centeredText(this.font, (Component)Component.literal((String)label), x + w / 2, y + 3, -1);
    }

    public boolean mouseScrolled(double mx, double my, double ha, double va) {
        List<Waypoint> wps = WaypointManager.getInstance().getWaypoints();
        int max = Math.max(0, wps.size() * 20 - (this.height - 60));
        this.scrollOffset = Mth.clamp((int)((int)((double)this.scrollOffset - va * 20.0 * 0.5)), (int)0, (int)max);
        return true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
        super.mouseClicked(event, bool);
        if (this.minecraft == null) {
            return false;
        }
        List<Waypoint> wps = WaypointManager.getInstance().getWaypoints();
        int startY = 22;
        int listW = this.width - 4;
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < wps.size(); ++i) {
            Waypoint wp = wps.get(i);
            int y = startY + i * 20 - this.scrollOffset;
            if (y < startY - 20 || y > this.height - 55) continue;
            int by = y + 3;
            int bx = listW - 12;

            String coords = String.format("%.0f %.0f %.0f", wp.x, wp.y, wp.z);
            int coordsWidth = this.font.width(coords);

            if (mx >= 138 && mx <= 140 + coordsWidth && my >= y && my <= y + 20) {
                String command = String.format("/tp %.0f %.0f %.0f", wp.x, wp.y, wp.z);
                this.minecraft.keyboardHandler.setClipboard(command);
                this.minecraft.gui.setOverlayMessage(Component.literal("Copied: " + command), false);
                return true;
            }
            if (mx >= (double)(bx - 110) && mx <= (double)(bx - 74) && my >= (double)by && my <= (double)(by + 14)) {
                this.minecraft.gui.setScreen((Screen)new NewWaypointScreen(wp));
                return true;
            }
            if (mx >= (double)(bx - 72) && mx <= (double)(bx - 36) && my >= (double)by && my <= (double)(by + 14)) {
                wp.visible = !wp.visible;
                WaypointManager.getInstance().save();
                return true;
            }
            if (!(mx >= (double)(bx - 34)) || !(mx <= (double)(bx - 6)) || !(my >= (double)by) || !(my <= (double)(by + 14))) continue;
            WaypointManager.getInstance().removeWaypoint(wp);
            return true;
        }
        return false;
    }

    private int getDistanceToPlayer(Waypoint wp) {
        if (this.minecraft.player == null) {
            return -1;
        }
        double dx = wp.x - this.minecraft.player.getX();
        double dy = wp.y - this.minecraft.player.getY();
        double dz = wp.z - this.minecraft.player.getZ();
        return (int)Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void close() {
        this.minecraft.gui.setScreen(null);
    }

    public boolean isPauseScreen() {
        return false;
    }
}

