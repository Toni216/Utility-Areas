package com.cipollomods.utilityareas.data;

import com.cipollomods.utilityareas.UtilityAreas;
import com.cipollomods.utilityareas.area.Area;

import java.util.*;

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

    public void removeArea(String id) {
        areas.remove(id);
        UtilityAreas.LOGGER.info("Area eliminada: {}", id);
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
}