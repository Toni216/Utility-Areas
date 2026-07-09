package com.cipollomods.utilityareas.api;

import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.data.AreaManager;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Public API for UtilityAreas mod.
 *
 * <p>Other mods should interact with UtilityAreas exclusively through this class.
 * Internal classes (Area, AreaManager, AreaType, etc.) are not part of the public
 * contract and may change between versions.</p>
 *
 * <p>All methods are thread-safe for read-only access.</p>
 *
 * <p>Usage example (in your mod):
 * <pre>{@code
 * if (UtilityAreaAPI.isNoSpawnAt(pos.getX(), pos.getZ())) {
 *     // suppress mob spawn
 * }
 * }</pre>
 * </p>
 */
public final class UtilityAPI {

    private UtilityAPI() {}

    // -------------------------------------------------------------------------
    // Spawn suppression — primary use case for Don't Go Too Far and similar mods
    // -------------------------------------------------------------------------

    /**
     * Returns true if there is at least one active SafeArea containing the given
     * (x, z) coordinates. Use this to suppress hostile mob spawning.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return true if the position is inside a no-spawn area
     */
    public static boolean isNoSpawnAt(double x, double z) {
        return AreaManager.getInstance().getAreasContaining(x, z).stream()
                .anyMatch(area -> area.getType() == AreaType.SAFE);
    }

    /**
     * Convenience overload accepting a {@link BlockPos}.
     */
    public static boolean isNoSpawnAt(BlockPos pos) {
        return isNoSpawnAt(pos.getX(), pos.getZ());
    }

    // -------------------------------------------------------------------------
    // General area presence queries
    // -------------------------------------------------------------------------

    /**
     * Returns true if the position is inside at least one active area of any type.
     */
    public static boolean isInsideAnyArea(double x, double z) {
        return !AreaManager.getInstance().getAreasContaining(x, z).isEmpty();
    }

    /**
     * Returns true if the position is inside at least one active area of the
     * given type. The type is identified by its string name (case-insensitive),
     * e.g. {@code "SAFE"}, {@code "DAMAGE"}, {@code "POTION"}.
     *
     * @param x        world X coordinate
     * @param z        world Z coordinate
     * @param typeName area type name, case-insensitive
     * @return true if a matching area contains the position
     * @throws IllegalArgumentException if the type name is unknown
     */
    public static boolean isInsideAreaOfType(double x, double z, String typeName) {
        AreaType type = AreaType.valueOf(typeName.toUpperCase());
        return AreaManager.getInstance().getAreasContaining(x, z).stream()
                .anyMatch(area -> area.getType() == type);
    }

    // -------------------------------------------------------------------------
    // Area ID queries — useful for logging, events, or conditional logic
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable list of IDs of all active areas that contain the
     * given position. The list is empty if the position is not inside any area.
     *
     * <p>IDs are stable across server restarts (they come from areas.json).</p>
     */
    public static List<String> getAreaIdsAt(double x, double z) {
        return AreaManager.getInstance().getAreasContaining(x, z).stream()
                .map(area -> area.getId())
                .toList();
    }

    /**
     * Returns true if an area with the given ID exists (active or not).
     */
    public static boolean areaExists(String id) {
        return AreaManager.getInstance().exists(id);
    }

    /**
     * Returns true if an area with the given ID exists AND is currently active.
     */
    public static boolean isAreaActive(String id) {
        return AreaManager.getInstance().getArea(id)
                .map(area -> area.isActive())
                .orElse(false);
    }

    /**
     * Returns true if the given position is inside the specific named area.
     * Returns false if the area does not exist or is inactive.
     *
     * @param id area identifier
     * @param x  world X coordinate
     * @param z  world Z coordinate
     */
    public static boolean isInsideArea(String id, double x, double z) {
        return AreaManager.getInstance().getArea(id)
                .filter(area -> area.isActive())
                .map(area -> area.contains(x, z))
                .orElse(false);
    }
}