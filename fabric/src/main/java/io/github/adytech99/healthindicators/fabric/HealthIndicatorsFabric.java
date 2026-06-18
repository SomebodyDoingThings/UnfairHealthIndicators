package io.github.adytech99.healthindicators.fabric;

import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.fabric.commands.ModCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.security.Key;

import static io.github.adytech99.healthindicators.HealthIndicatorsCommon.HEALTH_INDICATORS_CATEGORY;

@Environment(EnvType.CLIENT)
public class HealthIndicatorsFabric implements ClientModInitializer {
    public static final String MOD_ID = HealthIndicatorsCommon.MOD_ID;

    public static final KeyMapping HEARTS_RENDERING_ENABLED = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".renderingEnabled",
            GLFW.GLFW_KEY_LEFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping ARMOR_RENDERING_ENABLED = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".armorRenderingEnabled",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping OVERRIDE_ALL_FILTERS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".overrideAllFilters",
            GLFW.GLFW_KEY_RIGHT,
            HEALTH_INDICATORS_CATEGORY
    ));
    public static final KeyMapping INCREASE_HEART_OFFSET = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".increaseHeartOffset",
            GLFW.GLFW_KEY_UP,
            HEALTH_INDICATORS_CATEGORY
    ));
    public static final KeyMapping DECREASE_HEART_OFFSET = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".decreaseHeartOffset",
            GLFW.GLFW_KEY_DOWN,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping OPEN_CONFIG_SCREEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".openModMenuConfig",
            GLFW.GLFW_KEY_I,
            HEALTH_INDICATORS_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        HealthIndicatorsCommon.init();
        if(ModConfig.HANDLER.instance().enable_commands) ModCommands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HealthIndicatorsCommon.tick();

            while (HEARTS_RENDERING_ENABLED.consumeClick()) {
                HealthIndicatorsCommon.enableHeartsRendering();
            }

            while (ARMOR_RENDERING_ENABLED.consumeClick()) {
                HealthIndicatorsCommon.enableArmorRendering();
            }

            while (INCREASE_HEART_OFFSET.consumeClick()) {
                HealthIndicatorsCommon.increaseOffset();
            }

            while (DECREASE_HEART_OFFSET.consumeClick()) {
                HealthIndicatorsCommon.decreaseOffset();
            }
            if (OVERRIDE_ALL_FILTERS.isDown()) {
                HealthIndicatorsCommon.overrideFilters();
            }
            else if(Config.getOverrideAllFiltersEnabled()) {
                HealthIndicatorsCommon.disableOverrideFilters();
            }

            if(OPEN_CONFIG_SCREEN.consumeClick()) HealthIndicatorsCommon.openConfigScreen();
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            RenderTracker.removeFromUUIDS(entity);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.HANDLER.save();
        });
    }
}
