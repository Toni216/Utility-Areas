package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * Área que rellena automáticamente los cofres dentro de su zona cada cierto intervalo.
 * El contenido de cada cofre se guarda como snapshot en el momento de su registro.
 */
public class ChestRefillArea extends Area {

    private int refillIntervalMinutes;
    private long lastRefillTick;

    // Snapshot del contenido de cada cofre: posición -> items serializados
    private final Map<BlockPos, net.minecraft.world.item.ItemStack[]> snapshots = new HashMap<>();

    public ChestRefillArea(String id, AreaShape shape) {
        super(id, AreaType.CHEST_REFILL, shape);
        this.refillIntervalMinutes = 10;
        this.lastRefillTick = 0;
    }

    @Override
    public void onPlayerEnter(Player player) {
        // ChestRefillArea no hace nada al entrar
    }

    @Override
    public void onPlayerExit(Player player) {
        // ChestRefillArea no hace nada al salir
    }

    @Override
    public void onTick(ServerLevel level) {
        if (refillIntervalMinutes <= 0) return;

        long currentTick = level.getGameTime();
        long intervalTicks = (long) refillIntervalMinutes * 60 * 20;

        if (currentTick - lastRefillTick >= intervalTicks) {
            refill(level);
        }
    }

    /**
     * Rellena todos los cofres registrados con su snapshot guardado.
     */
    public void refill(ServerLevel level) {
        for (Map.Entry<BlockPos, net.minecraft.world.item.ItemStack[]> entry : snapshots.entrySet()) {
            if (level.getBlockEntity(entry.getKey()) instanceof ChestBlockEntity chest) {
                for (int i = 0; i < entry.getValue().length && i < chest.getContainerSize(); i++) {
                    chest.setItem(i, entry.getValue()[i].copy());
                }
            }
        }
        lastRefillTick = level.getGameTime();
    }

    public void takeSnapshot(ServerLevel level) {
        snapshots.clear();
        // El escaneo de cofres se hará desde el EventHandler al crear el área
    }

    /**
     * Registra el snapshot de un cofre en una posición concreta.
     */
    public void addSnapshot(BlockPos pos, net.minecraft.world.item.ItemStack[] items) {
        snapshots.put(pos, items);
    }

    public Map<BlockPos, net.minecraft.world.item.ItemStack[]> getSnapshots() { return snapshots; }

    public int getRefillIntervalMinutes() { return refillIntervalMinutes; }
    public void setRefillIntervalMinutes(int minutes) { this.refillIntervalMinutes = minutes; }

    public long getLastRefillTick() { return lastRefillTick; }
    public void setLastRefillTick(long tick) { this.lastRefillTick = tick; }
}