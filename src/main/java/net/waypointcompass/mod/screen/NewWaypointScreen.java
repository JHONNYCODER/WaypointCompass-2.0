/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.network.chat.Component
 */
package net.waypointcompass.mod.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.waypointcompass.mod.IconType;
import net.waypointcompass.mod.Waypoint;
import net.waypointcompass.mod.WaypointManager;

public class NewWaypointScreen
extends Screen {
    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private int selectedColor = 0xFF5555;
    private Waypoint existing;

    public NewWaypointScreen() {
        super((Component)Component.literal((String)"New Waypoint"));
    }

    public NewWaypointScreen(Waypoint edit) {
        super((Component)Component.literal((String)"Edit Waypoint"));
        this.existing = edit;
        this.selectedColor = edit.color;
    }

    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.nameField = new EditBox(this.font, cx - 80, cy - 70, 160, 20, (Component)Component.literal((String)"Waypoint Name"));
        this.nameField.setMaxLength(32);
        this.nameField.setHint((Component)Component.literal((String)"Waypoint name..."));
        if (this.existing != null) {
            this.nameField.setValue(this.existing.name);
        }
        this.addRenderableWidget(this.nameField);
        this.xField = new EditBox(this.font, cx - 70, cy - 42, 48, 18, (Component)Component.literal((String)"X"));
        this.yField = new EditBox(this.font, cx - 18, cy - 42, 48, 18, (Component)Component.literal((String)"Y"));
        this.zField = new EditBox(this.font, cx + 34, cy - 42, 48, 18, (Component)Component.literal((String)"Z"));
        if (this.existing != null) {
            this.xField.setValue(String.format("%.0f", this.existing.x));
            this.yField.setValue(String.format("%.0f", this.existing.y));
            this.zField.setValue(String.format("%.0f", this.existing.z));
        } else if (this.minecraft.player != null) {
            this.xField.setValue(String.format("%.0f", this.minecraft.player.getX()));
            this.yField.setValue(String.format("%.0f", this.minecraft.player.getY()));
            this.zField.setValue(String.format("%.0f", this.minecraft.player.getZ()));
        }
        this.addRenderableWidget(this.xField);
        this.addRenderableWidget(this.yField);
        this.addRenderableWidget(this.zField);
        String label = this.existing != null ? "Save" : "Create";
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)label), this::onSave).bounds(cx - 60, cy + 60, 120, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Cancel"), b -> this.close()).bounds(cx - 40, cy + 85, 80, 20).build());
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.centeredText(this.font, this.getTitle(), cx, cy - 95, 0xFFFFFF);
        graphics.text(this.font, (Component)Component.literal((String)"Name:"), cx - 88, cy - 68, 0xAAAAAA, true);
        graphics.text(this.font, (Component)Component.literal((String)"Coords:"), cx - 88, cy - 40, 0xAAAAAA, true);
        graphics.text(this.font, (Component)Component.literal((String)"Color:"), cx - 88, cy - 10, 0xAAAAAA, true);
        this.drawColorSlider(graphics, cx - 80, cy + 4, 160, 14, mouseX, mouseY);
        graphics.fill(cx - 80, cy + 24, cx - 64, cy + 40, this.selectedColor | 0xFF000000);
        graphics.text(this.font, (Component)Component.literal((String)String.format("#%06X", this.selectedColor)), cx - 58, cy + 27, 0xAAAAAA, true);
    }

    private void drawColorSlider(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        for (int i = 0; i < w; ++i) {
            float hue = (float)i / (float)w;
            int color = NewWaypointScreen.hsvToRgb(hue, 1.0f, 1.0f);
            graphics.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | color);
        }
        int thumbX = x + (int)(NewWaypointScreen.hueOf(this.selectedColor) * (float)w);
        graphics.fill(thumbX - 2, y - 2, thumbX + 3, y + h + 2, -1);
        graphics.fill(thumbX - 1, y - 1, thumbX + 2, y + h + 1, -16777216);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
        super.mouseClicked(event, bool);
        int cx = this.width / 2;
        int cy = this.height / 2;
        double mx = event.x();
        double my = event.y();
        int sx = cx - 80;
        int sy = cy + 4;
        int sw = 160;
        int sh = 14;
        if (mx >= (double)sx && mx <= (double)(sx + sw) && my >= (double)sy && my <= (double)(sy + sh)) {
            float hue = (float)((mx - (double)sx) / (double)sw);
            this.selectedColor = NewWaypointScreen.hsvToRgb(hue, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    private void onSave(Button button) {
        String dim;
        double z;
        double y;
        double x;
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        try {
            x = Double.parseDouble(this.xField.getValue());
            y = Double.parseDouble(this.yField.getValue());
            z = Double.parseDouble(this.zField.getValue());
        }
        catch (NumberFormatException e) {
            return;
        }
        String string = dim = this.minecraft.level != null ? this.minecraft.level.dimension().identifier().toString() : "minecraft:overworld";
        if (this.existing != null) {
            this.existing.name = name;
            this.existing.x = x;
            this.existing.y = y;
            this.existing.z = z;
            this.existing.color = this.selectedColor;
            WaypointManager.getInstance().save();
        } else {
            Waypoint wp = new Waypoint(name, x, y, z, this.selectedColor, IconType.SQUARE, dim);
            WaypointManager.getInstance().addWaypoint(wp);
        }
        this.close();
    }

    public void close() {
        this.minecraft.gui.setScreen(null);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static int hsvToRgb(float h, float s, float v) {
        int g;
        int r;
        int hi = (int)(h * 6.0f);
        float f = h * 6.0f - (float)hi;
        int p = (int)(v * (1.0f - s) * 255.0f);
        int q = (int)(v * (1.0f - s * f) * 255.0f);
        int t = (int)(v * (1.0f - s * (1.0f - f)) * 255.0f);
        return r << 16 | g << 8 | (switch (hi % 6) {
            case 0 -> {
                r = (int)(v * 255.0f);
                g = t;
                yield p;
            }
            case 1 -> {
                r = q;
                g = (int)(v * 255.0f);
                yield p;
            }
            case 2 -> {
                r = p;
                g = (int)(v * 255.0f);
                yield t;
            }
            case 3 -> {
                r = p;
                g = q;
                yield (int)(v * 255.0f);
            }
            case 4 -> {
                r = t;
                g = p;
                yield (int)(v * 255.0f);
            }
            case 5 -> {
                r = (int)(v * 255.0f);
                g = p;
                yield q;
            }
            default -> {
                r = 0;
                g = 0;
                yield 0;
            }
        });
    }

    private static float hueOf(int rgb) {
        int min;
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max == (min = Math.min(r, Math.min(g, b)))) {
            return 0.0f;
        }
        float diff = max - min;
        float h = max == r ? (float)(g - b) / diff + (float)(g < b ? 6 : 0) : (max == g ? (float)(b - r) / diff + 2.0f : (float)(r - g) / diff + 4.0f);
        return h / 6.0f;
    }
}

