/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package net.waypointcompass.mod;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public enum IconType {
    STAR("\u2605"),
    DIAMOND("\u2666"),
    CIRCLE("\u25cf"),
    CROSS("\u271a"),
    TRIANGLE("\u25b2"),
    SQUARE("\u25a0"),
    HEART("\u2665"),
    ARROW("\u27a4"),
    FLAG("\u2691"),
    HOME("\u2302");

    private final String symbol;

    private IconType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }

    private static int argbFromColor(int color) {
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int x, int y, int color, float scale) {
        if (scale <= 0.0f) {
            scale = 1.0f;
        }
        switch (this.ordinal()) {
            case 1: {
                this.renderDiamond(graphics, x, y, color, scale);
                break;
            }
            case 2: {
                this.renderCircle(graphics, x, y, color, scale);
                break;
            }
            case 3: {
                this.renderCross(graphics, x, y, color, scale);
                break;
            }
            case 4: {
                this.renderTriangle(graphics, x, y, color, scale);
                break;
            }
            case 5: {
                this.renderSquare(graphics, x, y, color, scale);
                break;
            }
            case 6: {
                this.renderHeart(graphics, x, y, color, scale);
                break;
            }
            default: {
                int alpha = color >> 24 & 0xFF;
                if (alpha == 0) {
                    alpha = 255;
                }
                int c = alpha << 24 | color & 0xFFFFFF;
                graphics.text(font, this.symbol, x - 4, y - 4, c, true);
            }
        }
    }

    private void renderDiamond(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int s = (int)(5.0f * scale);
        int argb = IconType.argbFromColor(color);
        graphics.fill(x - s, y, x, y + s, argb);
        graphics.fill(x, y, x + s, y + s, argb);
        graphics.fill(x - s + 1, y - s + 1, x + s, y, argb);
        graphics.fill(x - s + 1, y + s, x + s, y + 2 * s - 1, argb);
    }

    private void renderCircle(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int radius = (int)(4.0f * scale);
        int argb = IconType.argbFromColor(color);
        for (int dy = -radius; dy <= radius; ++dy) {
            for (int dx = -radius; dx <= radius; ++dx) {
                if (dx * dx + dy * dy > radius * radius) continue;
                graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, argb);
            }
        }
    }

    private void renderCross(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int s = (int)(4.0f * scale);
        int t = Math.max(1, (int)(1.5f * scale));
        int argb = IconType.argbFromColor(color);
        graphics.fill(x - s, y - t, x + s + 1, y + t + 1, argb);
        graphics.fill(x - t, y - s, x + t + 1, y + s + 1, argb);
    }

    private void renderTriangle(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int s = (int)(5.0f * scale);
        int argb = IconType.argbFromColor(color);
        for (int row = 0; row <= s; ++row) {
            int half = (row * s + s / 2) / s;
            int left = -half;
            int right = half;
            graphics.fill(x + left, y + row, x + right + 1, y + row + 1, argb);
            graphics.fill(x + left, y - row, x + right + 1, y - row + 1, argb);
        }
    }

    private void renderSquare(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int s = (int)(4.0f * scale);
        int argb = IconType.argbFromColor(color);
        graphics.fill(x - s, y - s, x + s + 1, y + s + 1, argb);
    }

    private void renderHeart(GuiGraphicsExtractor graphics, int x, int y, int color, float scale) {
        int s = (int)(4.0f * scale);
        int argb = IconType.argbFromColor(color);
        for (int dy = -s; dy <= s; ++dy) {
            for (int dx = -s; dx <= s; ++dx) {
                float nx = (float)dx / (float)s;
                float ny = (float)dy / (float)s;
                float heart = nx * nx + ny * ny - 1.0f;
                if (!((heart = heart * heart * heart - nx * nx * ny * ny * ny) <= 0.0f)) continue;
                graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, argb);
            }
        }
    }

    public static IconType fromOrdinal(int ordinal) {
        IconType[] values = IconType.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return SQUARE;
    }

    public static IconType fromName(String name) {
        try {
            return IconType.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            return SQUARE;
        }
    }
}

