package com.cipollomods.utilityareas.event;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor singleton de visualizaciones de áreas mediante partículas.
 * Cada jugador puede tener activa la visualización de una sola área a la vez.
 * Las partículas se envían únicamente al jugador que solicitó la visualización.
 */
public class AreaVisualizer {

    private static final AreaVisualizer INSTANCE = new AreaVisualizer();
    private final Map<UUID, Area> activeVisualizations = new HashMap<>();

    private AreaVisualizer() {}

    public static AreaVisualizer getInstance() {
        return INSTANCE;
    }

    public void show(ServerPlayer player, Area area) {
        activeVisualizations.put(player.getUUID(), area);
    }

    public void hide(ServerPlayer player) {
        activeVisualizations.remove(player.getUUID());
    }

    /** Renderiza las partículas del área activa para el jugador dado. */
    public void tick(ServerPlayer player) {
        Area area = activeVisualizations.get(player.getUUID());
        if (area == null) return;

        if (area.getShape() == AreaShape.CIRCLE) {
            tickCircle(player, area);
        } else {
            tickRect(player, area);
        }
    }

    private void tickCircle(ServerPlayer player, Area area) {
        double cx = area.getCenterX();
        double cz = area.getCenterZ();
        double r = area.getRadius();
        double y = player.getY();

        // Más puntos cuanto mayor sea el radio para mantener la densidad visual
        int points = (int) Math.max(36, r * 2);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double px = cx + r * Math.cos(angle);
            double pz = cz + r * Math.sin(angle);
            player.serverLevel().sendParticles(player,
                    ParticleTypes.FLAME, true,
                    px, y, pz,
                    1, 0, 0, 0, 0);
        }
    }

    private void tickRect(ServerPlayer player, Area area) {
        double x1 = area.getX1();
        double z1 = area.getZ1();
        double x2 = area.getX2();
        double z2 = area.getZ2();
        double y = player.getY();

        double step = 1.0;

        // Borde norte y sur
        for (double x = x1; x <= x2; x += step) {
            spawnParticle(player, x, y, z1);
            spawnParticle(player, x, y, z2);
        }
        // Borde oeste y este
        for (double z = z1; z <= z2; z += step) {
            spawnParticle(player, x1, y, z);
            spawnParticle(player, x2, y, z);
        }
    }

    private void spawnParticle(ServerPlayer player, double x, double y, double z) {
        player.serverLevel().sendParticles(player,
                ParticleTypes.FLAME, true,
                x, y, z,
                1, 0, 0, 0, 0);
    }

    public boolean isVisualizingAny(ServerPlayer player) {
        return activeVisualizations.containsKey(player.getUUID());
    }
}