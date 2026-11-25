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
    private boolean draggingColorSlider;

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
        if (this.existing != null) {
            this.addRenderableWidget(Button.builder(
                Component.literal("⚙"),
                b -> this.minecraft.gui.setScreen(new WaypointSizeScreen(this.existing, this))
            ).bounds(cx + 85, cy - 95, 20, 20).build());
        }

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
        if (event.button() != 0) {
            return false;
        }

        if (super.mouseClicked(event, bool)) {
            return true;
        }

        int cx = this.width / 2;
        int cy = this.height / 2;
        double mx = event.x();
        double my = event.y();

        int sx = cx - 80;
        int sy = cy + 4;
        int sw = 160;
        int sh = 14;

        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
            this.draggingColorSlider = true;
            this.updateColorFromMouse(mx);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingColorSlider && event.button() == 0) {
            this.updateColorFromMouse(event.x());
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.draggingColorSlider = false;
        }

        return super.mouseReleased(event);
    }

    private void updateColorFromMouse(double mouseX) {
        int sx = this.width / 2 - 80;
        int sw = 160;

        float hue = (float)((mouseX - sx) / sw);
        hue = Math.max(0.0f, Math.min(1.0f, hue));

        this.selectedColor = NewWaypointScreen.hsvToRgb(hue, 1.0f, 1.0f);
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

            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                return;
            }
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
        int hi = (int)(h * 6.0f);
        float f = h * 6.0f - hi;

        int value = (int)(v * 255.0f);
        int p = (int)(v * (1.0f - s) * 255.0f);
        int q = (int)(v * (1.0f - s * f) * 255.0f);
        int t = (int)(v * (1.0f - s * (1.0f - f)) * 255.0f);

        return switch (hi % 6) {
            case 0 -> (value << 16) | (t << 8) | p;
            case 1 -> (q << 16) | (value << 8) | p;
            case 2 -> (p << 16) | (value << 8) | t;
            case 3 -> (p << 16) | (q << 8) | value;
            case 4 -> (t << 16) | (p << 8) | value;
            case 5 -> (value << 16) | (p << 8) | q;
            default -> 0;
        };
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