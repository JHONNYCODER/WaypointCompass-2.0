package net.waypointcompass.mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.waypointcompass.mod.WaypointBarOverlay;
import net.waypointcompass.mod.WaypointManager;
import net.waypointcompass.mod.screen.NewWaypointScreen;
import net.waypointcompass.mod.screen.WaypointMenuScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class TopCompassClient
implements ClientModInitializer {
    public static final String MOD_ID = "waypointcompass";
    private static final KeyMapping.Category CATEGORY =
    KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );
    private static KeyMapping newWaypointKey;
    private static KeyMapping menuKey;
    private static KeyMapping toggleBarKey;

    public void onInitializeClient() {
        newWaypointKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.waypointcompass.new", InputConstants.KEY_N, CATEGORY)
        );

        menuKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.waypointcompass.menu", InputConstants.KEY_M, CATEGORY)
        );

        toggleBarKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.waypointcompass.togglebar", InputConstants.KEY_H, CATEGORY)
        );
        HudElementRegistry.attachElementBefore((Identifier)VanillaHudElements.HOTBAR, (Identifier)Identifier.fromNamespaceAndPath((String)MOD_ID, (String)"waypoint_bar"), WaypointBarOverlay::extract);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (newWaypointKey.consumeClick()) {
                client.gui.setScreen((Screen)new NewWaypointScreen());
            }
            while (menuKey.consumeClick()) {
                if (client.gui.screen() instanceof WaypointMenuScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen((Screen)new WaypointMenuScreen());
                }

                client.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
            }
            while (toggleBarKey.consumeClick()) {
                WaypointBarOverlay.toggleVisibility();
                client.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WaypointManager.getInstance().init());
    }
}