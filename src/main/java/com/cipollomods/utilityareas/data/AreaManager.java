package com.cipollomods.utilityareas.data;

import com.cipollomods.utilityareas.UtilityAreas;
import com.cipollomods.utilityareas.area.Area;
import java.util.*;

/**
 * Gestor singleton de todas las áreas del mod.
 * Proporciona métodos para registrar, eliminar y consultar áreas en memoria.
 */
public class AreaManager {

    private static final AreaManager INSTANCE = new AreaManager();
    private final Map<String, Area> areas = new HashMap<>();

    private AreaManager() {}

    public static AreaManager getInstance() {
        return INSTANCE;
    }

    public void addArea(Area area) {
        areas.put(area.getId(), area);
        UtilityAreas.LOGGER.info("Area añadida: {}", area.getId());
    }

    /**
     * Registra un área sin loguearla como "añadida". Se usa exclusivamente
     * durante la carga desde areas.json, para no confundir esa operación
     * con una creación real hecha por un operador.
     */
    public void addAreaSilent(Area area) {
        areas.put(area.getId(), area);
    }

    public void removeArea(String id) {
        areas.remove(id);
        UtilityAreas.LOGGER.info("Area eliminada: {}", id);
    }

    /**
     * Delega el guardado en {@link AreaPersistenceManager}. Los comandos que
     * modifican el estado de una o varias áreas deben llamar a este método
     * al terminar, para que los cambios sobrevivan a un reinicio del servidor.
     */
    public void save() {
        AreaPersistenceManager.getInstance().save();
    }

    public Optional<Area> getArea(String id) {
        return Optional.ofNullable(areas.get(id));
    }

    public Collection<Area> getAllAreas() {
        return areas.values();
    }

    public List<Area> getActiveAreas() {
        return areas.values().stream()
                .filter(Area::isActive)
                .toList();
    }

    /**
     * Devuelve todas las áreas activas que contienen las coordenadas dadas.
     */
    public List<Area> getAreasContaining(double x, double z) {
        return getActiveAreas().stream()
                .filter(area -> area.contains(x, z))
                .toList();
    }

    public boolean exists(String id) {
        return areas.containsKey(id);
    }

    public void clear() {
        areas.clear();
    }

    public List<Area> getAreasContaining(double x, double y, double z) {
        return getActiveAreas().stream()
                .filter(area -> area.contains(x, y, z))
                .toList();
    }

}