package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.data.AreaManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Área que teletransporta a los jugadores al entrar en su zona.
 * Soporta tres modos de destino: coordenadas fijas ({@link TeleportMode#FIXED_COORDS}),
 * el centro de otra área existente ({@link TeleportMode#FIXED_AREA}), o un punto
 * aleatorio y seguro dentro de un radio ({@link TeleportMode#RANDOM}).
 * Incluye un cooldown por jugador para evitar bucles de teletransporte, y un
 * sistema anti-apilamiento para que varios jugadores puedan usar la misma zona
 * a la vez sin quedar atascados unos con otros.
 */
public class TeleportArea extends Area {

    /**
     * Modo de cálculo del destino del teletransporte.
     */
    public enum TeleportMode {
        FIXED_COORDS,
        FIXED_AREA,
        RANDOM
    }

    private static final Random RNG = new Random();
    private static final int MAX_RANDOM_ATTEMPTS = 20;
    private static final int MAX_SPIRAL_RADIUS = 12;

    private TeleportMode mode;

    // --- FIXED_COORDS ---
    private double destX, destY, destZ;
    private float destYaw, destPitch;

    // --- FIXED_AREA ---
    private String destAreaId;

    // --- RANDOM ---
    // Si son null, se usa el centro de la propia área como centro del sorteo.
    private Double randomCenterX;
    private Double randomCenterZ;
    private double randomRadius;

    private int cooldownSeconds;

    // Mapa de jugador -> tick en el que fue teletransportado por última vez
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportArea(String id, AreaShape shape) {
        super(id, AreaType.TELEPORT, shape);
        this.mode = TeleportMode.FIXED_COORDS;
        this.destX = 0;
        this.destY = 64;
        this.destZ = 0;
        this.destYaw = 0;
        this.destPitch = 0;
        this.destAreaId = null;
        this.randomCenterX = null;
        this.randomCenterZ = null;
        this.randomRadius = 20;
        this.cooldownSeconds = 5;
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        long currentTick = level.getGameTime();
        long cooldownTicks = (long) cooldownSeconds * 20;
        long lastTeleport = cooldowns.getOrDefault(player.getUUID(), -cooldownTicks);
        if (currentTick - lastTeleport < cooldownTicks) return;

        double[] spot = resolveDestination(level);
        if (spot == null) {
            com.cipollomods.utilityareas.UtilityAreas.LOGGER.warn(
                    "TeleportArea '{}' no encontró un destino seguro para {}", id, player.getName().getString());
            return;
        }

        player.teleportTo(spot[0], spot[1], spot[2]);
        if (mode == TeleportMode.FIXED_COORDS) {
            player.setYRot(destYaw);
            player.setXRot(destPitch);
        }
        cooldowns.put(player.getUUID(), currentTick);
    }

    @Override
    public void onPlayerExit(Player player) {
        // TeleportArea no hace nada al salir
    }

    @Override
    public void onTick(ServerLevel level) {
        // TeleportArea no necesita lógica de tick
    }

    /**
     * Calcula las coordenadas de destino según el modo configurado.
     * Devuelve null si no se pudo encontrar un punto válido.
     */
    private double[] resolveDestination(ServerLevel level) {
        return switch (mode) {
            case FIXED_COORDS -> findFreeSpotNear(level, destX, destY, destZ);
            case FIXED_AREA -> resolveAreaDestination(level);
            case RANDOM -> findSafeRandomSpot(level);
        };
    }

    private double[] resolveAreaDestination(ServerLevel level) {
        if (destAreaId == null) return null;

        Area target = AreaManager.getInstance().getArea(destAreaId).orElse(null);
        if (target == null || !target.isActive()) return null;

        double centerX, centerZ;
        if (target.getShape() == AreaShape.CIRCLE) {
            centerX = target.getCenterX();
            centerZ = target.getCenterZ();
        } else {
            centerX = (target.getX1() + target.getX2()) / 2.0;
            centerZ = (target.getZ1() + target.getZ2()) / 2.0;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(centerX), (int) Math.floor(centerZ));
        return findFreeSpotNear(level, centerX, surfaceY, centerZ);
    }

    /**
     * Busca un punto aleatorio y seguro dentro del radio configurado, evitando
     * bloques sólidos y lava. Reintenta hasta MAX_RANDOM_ATTEMPTS veces con
     * coordenadas distintas antes de rendirse.
     */
    private double[] findSafeRandomSpot(ServerLevel level) {
        double centerX = randomCenterX != null ? randomCenterX : getShapeCenterX();
        double centerZ = randomCenterZ != null ? randomCenterZ : getShapeCenterZ();

        for (int i = 0; i < MAX_RANDOM_ATTEMPTS; i++) {
            double angle = RNG.nextDouble() * 2 * Math.PI;
            double dist = RNG.nextDouble() * randomRadius;
            double x = centerX + Math.cos(angle) * dist;
            double z = centerZ + Math.sin(angle) * dist;

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            if (isSafeSpot(level, (int) Math.floor(x), surfaceY, (int) Math.floor(z))) {
                return new double[]{x, surfaceY, z};
            }
        }
        return null;
    }

    /**
     * A partir de un punto deseado, busca un hueco libre de bloques y de otras
     * entidades, expandiendo la búsqueda en anillos concéntricos si hace falta.
     * Esto es lo que permite que varios jugadores usen a la vez un destino
     * FIXED_COORDS o FIXED_AREA sin quedar apilados entre sí.
     */
    private double[] findFreeSpotNear(ServerLevel level, double baseX, double baseY, double baseZ) {
        int by = (int) Math.floor(baseY);

        for (int radius = 0; radius <= MAX_SPIRAL_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue; // solo el borde del anillo

                    double x = baseX + dx;
                    double z = baseZ + dz;
                    if (isSafeSpot(level, (int) Math.floor(x), by, (int) Math.floor(z))
                            && !isOccupied(level, x, by, z)) {
                        return new double[]{x, by, z};
                    }
                }
            }
        }

        // Sin hueco libre de entidades tras la búsqueda: mejor apilar jugadores
        // que no teletransportar en absoluto.
        return new double[]{baseX, baseY, baseZ};
    }

    /**
     * Comprueba que el hueco (2 bloques de alto) esté libre de colisión y que
     * no haya lava en los pies ni justo debajo, para evitar asfixia o muerte
     * instantánea al aparecer.
     */
    private boolean isSafeSpot(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();

        boolean feetFree = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty();
        boolean headFree = level.getBlockState(head).getCollisionShape(level, head).isEmpty();

        FluidState feetFluid = level.getFluidState(feet);
        FluidState floorFluid = level.getFluidState(floor);
        boolean noLava = !feetFluid.is(FluidTags.LAVA) && !floorFluid.is(FluidTags.LAVA);

        return feetFree && headFree && noLava;
    }

    /** Comprueba si ya hay otra entidad ocupando el punto dado. */
    private boolean isOccupied(ServerLevel level, double x, double y, double z) {
        AABB box = new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
        return !level.getEntities((Entity) null, box).isEmpty();
    }

    private double getShapeCenterX() {
        return shape == AreaShape.CIRCLE ? centerX : (x1 + x2) / 2.0;
    }

    private double getShapeCenterZ() {
        return shape == AreaShape.CIRCLE ? centerZ : (z1 + z2) / 2.0;
    }

    // --- Getters / setters ---

    public TeleportMode getMode() { return mode; }
    public void setMode(TeleportMode mode) { this.mode = mode; }

    public double getDestX() { return destX; }
    public double getDestY() { return destY; }
    public double getDestZ() { return destZ; }
    public float getDestYaw() { return destYaw; }
    public float getDestPitch() { return destPitch; }

    /**
     * Establece la posición y orientación de destino para el modo FIXED_COORDS.
     */
    public void setDestination(double x, double y, double z, float yaw, float pitch) {
        this.destX = x;
        this.destY = y;
        this.destZ = z;
        this.destYaw = yaw;
        this.destPitch = pitch;
    }

    public String getDestAreaId() { return destAreaId; }
    public void setDestAreaId(String destAreaId) { this.destAreaId = destAreaId; }

    public Double getRandomCenterX() { return randomCenterX; }
    public Double getRandomCenterZ() { return randomCenterZ; }

    /** Fija un centro específico para el sorteo aleatorio (modo RANDOM). */
    public void setRandomCenter(double x, double z) {
        this.randomCenterX = x;
        this.randomCenterZ = z;
    }

    /** Vuelve a usar el centro de la propia área como centro del sorteo. */
    public void resetRandomCenter() {
        this.randomCenterX = null;
        this.randomCenterZ = null;
    }

    public double getRandomRadius() { return randomRadius; }
    public void setRandomRadius(double randomRadius) { this.randomRadius = randomRadius; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    @Override
    protected String describeSpecific() {
        StringBuilder sb = new StringBuilder();
        sb.append("  Modo: ").append(mode).append("\n");
        switch (mode) {
            case FIXED_COORDS -> sb.append("  Destino: (").append(destX).append(", ").append(destY)
                    .append(", ").append(destZ).append(") yaw=").append(destYaw)
                    .append(" pitch=").append(destPitch).append("\n");
            case FIXED_AREA -> sb.append("  Destino: área '").append(destAreaId).append("'\n");
            case RANDOM -> sb.append("  Centro aleatorio: ")
                    .append(randomCenterX != null ? "(" + randomCenterX + ", " + randomCenterZ + ")" : "centro propio del área")
                    .append(", radio=").append(randomRadius).append("\n");
        }
        sb.append("  Cooldown: ").append(cooldownSeconds).append("s\n");
        return sb.toString();
    }
}