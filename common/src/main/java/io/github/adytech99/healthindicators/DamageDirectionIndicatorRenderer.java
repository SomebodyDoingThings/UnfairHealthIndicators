package io.github.adytech99.healthindicators;

import io.github.adytech99.healthindicators.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class DamageDirectionIndicatorRenderer {
    private static PlayerEntity player = HealthIndicatorsCommon.client.player;
    private static int timeSinceLastDamage = Integer.MAX_VALUE;
    private static LivingEntity attacker;

    public static void markDamageToPlayer(LivingEntity livingEntity){
        timeSinceLastDamage = 0;
        attacker = livingEntity;
    }

    public static void tick(){
        player = HealthIndicatorsCommon.client.player;
        if(timeSinceLastDamage != Integer.MAX_VALUE) timeSinceLastDamage++;
        if (attacker == null || attacker.isDead() || attacker.isRemoved()){
            timeSinceLastDamage = Integer.MAX_VALUE;
            attacker = null;
        }
        if(timeSinceLastDamage == Integer.MAX_VALUE) attacker = null;
    }

    public static void render(DrawContext drawContext, float tickDelta) {
        if (player == null) return;
        if (timeSinceLastDamage <= ModConfig.HANDLER.instance().damage_direction_indicators_visibility_time * 20 && attacker != null) {
            // Get positions and calculate direction
            Vec3d playerPos = player.getEntityPos();
            Vec3d attackerPos = attacker.getEntityPos();
            double deltaX = attackerPos.x - playerPos.x;
            double deltaZ = attackerPos.z - playerPos.z;

            // Calculate yaw to attacker and delta from player's current view
            float yawToAttacker = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
            yawToAttacker = MathHelper.wrapDegrees(yawToAttacker);
            float deltaYaw = MathHelper.wrapDegrees(yawToAttacker - player.getYaw());

            // Get screen center and setup parameters
            int centerX = drawContext.getScaledWindowWidth() / 2;
            int centerY = drawContext.getScaledWindowHeight() / 2;
            float radius = 24.0f;
            float angle = (float) Math.toRadians(deltaYaw);

            // Calculate indicator position
            float indicatorX = centerX + radius * (float) Math.sin(angle);
            float indicatorY = centerY - radius * (float) Math.cos(angle);

            // Calculate fade-out alpha (0-255)
            int alpha = 255;
            if (ModConfig.HANDLER.instance().damage_direction_indicators_fade_out) {
                int fadeDelay = (ModConfig.HANDLER.instance().damage_direction_indicators_visibility_time - ModConfig.HANDLER.instance().damage_direction_indicators_fade_out_time) * 20;
                if (timeSinceLastDamage >= fadeDelay) {
                    float progress = Math.min((timeSinceLastDamage - fadeDelay) / (float) (ModConfig.HANDLER.instance().damage_direction_indicators_fade_out_time * 20), 1.0f);
                    alpha = 255 - (int) (255 * progress);
                }
            }

            float scale = ModConfig.HANDLER.instance().damage_direction_indicators_scale;
            Color color = ModConfig.HANDLER.instance().damage_direction_indicators_color;
            int argb = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

            // Define triangle vertices in model space (pointing upwards)
            float x1 = -3 * scale;
            float y1 = 4 * scale;
            float x2 = 3 * scale;
            float y2 = 4 * scale;
            float x3 = 0;
            float y3 = -4 * scale;

            // Compute sin and cos for rotation
            float sin = MathHelper.sin(angle);
            float cos = MathHelper.cos(angle);

            // Rotate and translate each vertex
            float v1x = x1 * cos - y1 * sin + indicatorX;
            float v1y = x1 * sin + y1 * cos + indicatorY;
            float v2x = x2 * cos - y2 * sin + indicatorX;
            float v2y = x2 * sin + y2 * cos + indicatorY;
            float v3x = x3 * cos - y3 * sin + indicatorX;
            float v3y = x3 * sin + y3 * cos + indicatorY;

            // Sort vertices by Y coordinate
            float[] xs = {v1x, v2x, v3x};
            float[] ys = {v1y, v2y, v3y};
            sortVerticesByY(xs, ys);

            // Draw filled triangle using scanline algorithm
            drawTriangle(drawContext, xs, ys, argb);
        }
    }

    // Helper: Sort vertices by Y coordinate (ascending)
    private static void sortVerticesByY(float[] xs, float[] ys) {
        if (ys[0] > ys[1]) {
            swap(xs, ys, 0, 1);
        }
        if (ys[0] > ys[2]) {
            swap(xs, ys, 0, 2);
        }
        if (ys[1] > ys[2]) {
            swap(xs, ys, 1, 2);
        }
    }

    // Helper: Swap two vertices in arrays
    private static void swap(float[] xs, float[] ys, int i, int j) {
        float tx = xs[i];
        xs[i] = xs[j];
        xs[j] = tx;
        float ty = ys[i];
        ys[i] = ys[j];
        ys[j] = ty;
    }

    // Helper: Draw a filled triangle using scanline algorithm
    private static void drawTriangle(DrawContext context, float[] xs, float[] ys, int color) {
        float minY = ys[0];
        float midY = ys[1];
        float maxY = ys[2];

        if (minY == maxY) return; // Degenerate triangle

        // Iterate over each scanline
        for (int y = (int) Math.floor(minY); y <= (int) Math.ceil(maxY); y++) {
            if (y < minY || y > maxY) continue;

            boolean inSecondHalf = y > midY;
            float xa, xb;

            if (!inSecondHalf) {
                // Between minY and midY
                xa = interpolate(xs[0], ys[0], xs[1], ys[1], y);
                xb = interpolate(xs[0], ys[0], xs[2], ys[2], y);
            } else {
                // Between midY and maxY
                xa = interpolate(xs[1], ys[1], xs[2], ys[2], y);
                xb = interpolate(xs[0], ys[0], xs[2], ys[2], y);
            }

            // Ensure left to right order
            if (xa > xb) {
                float tmp = xa;
                xa = xb;
                xb = tmp;
            }

            // Draw horizontal line segment
            context.fill((int) xa, y, (int) xb + 1, y + 1, color);
        }
    }

    // Helper: Linear interpolation for edge scanning
    private static float interpolate(float x1, float y1, float x2, float y2, float y) {
        if (y1 == y2) return x1;
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
    }
}