package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class SafeArea extends Area {

    public SafeArea(String id, AreaShape shape) {
        super(id, AreaType.SAFE, shape);
    }

    @Override
    public void onPlayerEnter(Player player) {
        // SafeArea no hace nada al entrar
    }

    @Override
    public void onPlayerExit(Player player) {
        // SafeArea no hace nada al salir
    }

    @Override
    public void onTick(ServerLevel level) {
        // SafeArea no necesita lógica de tick
        // El cancelado de spawns se gestiona en el EventHandler via LivingSpawnEvent
    }
}