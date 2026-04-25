package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Área que teletransporta a los jugadores al entrar en su zona.
 * Incluye un cooldown por jugador para evitar bucles de teletransporte.
 */
public class TeleportArea extends Area {

    private double destX, destY, destZ;
    private float destYaw, destPitch;
    private int cooldownSeconds;

    // Mapa de jugador -> tick en el que fue teletransportado por última vez
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportArea(String id, AreaShape shape) {
        super(id, AreaType.TELEPORT, shape);
        this.destX = 0;
        this.destY = 64;
        this.destZ = 0;
        this.destYaw = 0;
        this.destPitch = 0;
        this.cooldownSeconds = 5;
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        long currentTick = level.getGameTime();
        long cooldownTicks = (long) cooldownSeconds * 20;
        long lastTeleport = cooldowns.getOrDefault(player.getUUID(), -cooldownTicks);

        if (currentTick - lastTeleport >= cooldownTicks) {
            player.teleportTo(destX, destY, destZ);
            player.setYRot(destYaw);
            player.setXRot(destPitch);
            cooldowns.put(player.getUUID(), currentTick);
        }
    }

    @Override
    public void onPlayerExit(Player player) {
        // TeleportArea no hace nada al salir
    }

    @Override
    public void onTick(ServerLevel level) {
        // TeleportArea no necesita lógica de tick
    }

    public double getDestX() { return destX; }
    public double getDestY() { return destY; }
    public double getDestZ() { return destZ; }
    public float getDestYaw() { return destYaw; }
    public float getDestPitch() { return destPitch; }

    /**
     * Establece la posición y orientación de destino del teletransporte.
     */
    public void setDestination(double x, double y, double z, float yaw, float pitch) {
        this.destX = x;
        this.destY = y;
        this.destZ = z;
        this.destYaw = yaw;
        this.destPitch = pitch;
    }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
}