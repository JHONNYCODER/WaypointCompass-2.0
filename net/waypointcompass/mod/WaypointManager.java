/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  net.fabricmc.loader.api.FabricLoader
 */
package net.waypointcompass.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.waypointcompass.mod.IconType;
import net.waypointcompass.mod.Waypoint;

public class WaypointManager {
    private static WaypointManager instance;
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File saveFile;

    private WaypointManager() {
    }

    public static WaypointManager getInstance() {
        if (instance == null) {
            instance = new WaypointManager();
        }
        return instance;
    }

    public void init() {
        this.saveFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "waypointcompass/waypoints.json");
        this.load();
    }

    public void load() {
        if (this.saveFile != null && this.saveFile.exists()) {
            try (FileReader reader = new FileReader(this.saveFile);){
                Type type = new TypeToken<List<WaypointData>>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }
                }.getType();
                List dataList = (List)this.gson.fromJson((Reader)reader, type);
                this.waypoints.clear();
                if (dataList != null) {
                    for (WaypointData data : dataList) {
                        this.waypoints.add(data.toWaypoint());
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        if (this.saveFile == null) {
            return;
        }
        ArrayList<WaypointData> dataList = new ArrayList<WaypointData>();
        for (Waypoint wp : this.waypoints) {
            dataList.add(new WaypointData(wp));
        }
        this.saveFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(this.saveFile);){
            this.gson.toJson(dataList, (Appendable)writer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Waypoint> getWaypoints() {
        return this.waypoints;
    }

    public void addWaypoint(Waypoint wp) {
        this.waypoints.add(wp);
        this.save();
    }

    public void removeWaypoint(Waypoint wp) {
        this.waypoints.remove(wp);
        this.save();
    }

    public void clearAll() {
        this.waypoints.clear();
        this.save();
    }

    public Waypoint getNearest(double x, double y, double z) {
        Waypoint nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Waypoint wp : this.waypoints) {
            double dist = Math.sqrt(Math.pow(wp.x - x, 2.0) + Math.pow(wp.y - y, 2.0) + Math.pow(wp.z - z, 2.0));
            if (!(dist < nearestDist)) continue;
            nearestDist = dist;
            nearest = wp;
        }
        return nearest;
    }

    private static class WaypointData {
        String name;
        double x;
        double y;
        double z;
        int color;
        String icon;
        String dimension;
        Boolean visible;

        WaypointData(Waypoint wp) {
            this.name = wp.name;
            this.x = wp.x;
            this.y = wp.y;
            this.z = wp.z;
            this.color = wp.color;
            this.icon = wp.icon != null ? wp.icon.name() : "SQUARE";
            this.dimension = wp.dimension;
            this.visible = wp.visible ? null : Boolean.valueOf(false);
        }

        Waypoint toWaypoint() {
            Waypoint wp = new Waypoint(this.name, this.x, this.y, this.z, this.color, IconType.fromName(this.icon), this.dimension);
            wp.visible = this.visible == null || this.visible != false;
            return wp;
        }
    }
}

