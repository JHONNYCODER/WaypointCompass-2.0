package net.waypointcompass.mod.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.waypointcompass.mod.Waypoint;
import net.waypointcompass.mod.WaypointManager;

public class WaypointSizeScreen extends Screen {
    private final Waypoint waypoint;
    private final Screen parent;

    private boolean draggingSlider;

    private static final float MIN_SIZE = 0.5f;
    private static final float MAX_SIZE = 2.0f;

    private static final int SLIDER_WIDTH = 180;
    private static final int SLIDER_HEIGHT = 8;

    public WaypointSizeScreen(Waypoint waypoint, Screen parent) {
        super(Component.literal("Waypoint Size"));
        this.waypoint = waypoint;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(
            Button.builder(
                Component.literal("Reset"),
                button -> {
                    this.waypoint.size = 1.0f;
                    WaypointManager.getInstance().save();
                }
            ).bounds(cx - 65, cy + 35, 60, 20).build()
        );

        this.addRenderableWidget(
            Button.builder(
                Component.literal("Back"),
                button -> this.goBack()
            ).bounds(cx + 5, cy + 35, 60, 20).build()
        );
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        graphics.centeredText(
            this.font,
            this.getTitle(),
            cx,
            cy - 55,
            0xFFFFFF
        );

        graphics.centeredText(
            this.font,
            Component.literal(waypoint.name),
            cx,
            cy - 30,
            waypoint.color | 0xFF000000
        );

        float size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, waypoint.size));

        graphics.centeredText(
            this.font,
            Component.literal(String.format("Size: %.2fx", size)),
            cx,
            cy - 5,
            0xFFFFFF
        );

        this.drawSlider(
            graphics,
            cx - SLIDER_WIDTH / 2,
            cy + 15
        );
    }

    private void drawSlider(
        GuiGraphicsExtractor graphics,
        int x,
        int y
    ) {

        float size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, waypoint.size));

        int fillWidth = Math.round(
            ((size - MIN_SIZE) / (MAX_SIZE - MIN_SIZE))
                * SLIDER_WIDTH
        );

        graphics.fill(
            x,
            y,
            x + SLIDER_WIDTH,
            y + SLIDER_HEIGHT,
            0xFF555555
        );

        graphics.fill(
            x,
            y,
            x + fillWidth,
            y + SLIDER_HEIGHT,
            0xFF55AAFF
        );

        int thumbX = x + fillWidth;

        graphics.fill(
            thumbX - 3,
            y - 3,
            thumbX + 4,
            y + SLIDER_HEIGHT + 3,
            0xFFFFFFFF
        );

        graphics.text(
            this.font,
            Component.literal("0.5x"),
            x,
            y + 13,
            0xAAAAAA,
            true
        );

        graphics.text(
            this.font,
            Component.literal("2.0x"),
            x + SLIDER_WIDTH - this.font.width("2.0x"),
            y + 13,
            0xAAAAAA,
            true
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
        if (event.button() != 1) {
            return false;
        }

        if (super.mouseClicked(event, bool)) {
            return true;
        }

        int x = this.width / 2 - SLIDER_WIDTH / 2;
        int y = this.height / 2 + 15;

        if (event.x() >= x - 4
            && event.x() <= x + SLIDER_WIDTH + 4
            && event.y() >= y - 5
            && event.y() <= y + SLIDER_HEIGHT + 5) {

            this.draggingSlider = true;
            this.updateSizeFromMouse(event.x());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(
        MouseButtonEvent event,
        double dx,
        double dy
    ) {
        if (this.draggingSlider && event.button() == 1) {
            this.updateSizeFromMouse(event.x());
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1 && this.draggingSlider) {
            this.draggingSlider = false;
            WaypointManager.getInstance().save();
            return true;
        }

        return super.mouseReleased(event);
    }

    private void updateSizeFromMouse(double mouseX) {
        int x = this.width / 2 - SLIDER_WIDTH / 2;

        float progress = (float)(
            (mouseX - x) / SLIDER_WIDTH
        );

        progress = Math.max(0.0f, Math.min(1.0f, progress));

        this.waypoint.size = MIN_SIZE
            + progress * (MAX_SIZE - MIN_SIZE);
    }

    private void goBack() {
        WaypointManager.getInstance().save();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.goBack();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}