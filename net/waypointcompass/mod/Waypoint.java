/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package net.waypointcompass.mod;

import net.minecraft.world.phys.Vec3;
import net.waypointcompass.mod.IconType;

public class Waypoint {
    public String name;
    public double x;
    public double y;
    public double z;
    public int color;
    public IconType icon;
    public String dimension;
    public boolean visible = true;

    public Waypoint(String name, double x, double y, double z, int color, IconType icon, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.icon = icon;
        this.dimension = dimension;
    }

    public float getBearing(Vec3 playerPos) {
        double dx = this.x - playerPos.x;
        double dz = this.z - playerPos.z;
        double angle = Math.toDegrees(Math.atan2(dx, dz));
        float bearing = (float)(-angle + 180.0);
        return (bearing + 360.0f) % 360.0f;
    }

    public double distanceTo(Vec3 pos) {
        double dx = this.x - pos.x;
        double dy = this.y - pos.y;
        double dz = this.z - pos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

