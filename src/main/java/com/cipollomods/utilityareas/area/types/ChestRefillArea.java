package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.data.LootGroupManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Área que rellena automáticamente los cofres dentro de su zona cada cierto
 * intervalo, sacando el contenido de un {@link LootGroupManager grupo de
 * loot} configurable (uno solo, compartido por todos los cofres del área).
 *
 * <p>Al escanear la zona ({@link #scanChests}), los cofres que ya tenían
 * objetos se dejan tal cual hasta el primer relleno programado; a partir de
 * ahí, todos los cofres registrados usan el grupo configurado por igual.</p>
 */
public class ChestRefillArea extends Area {

    private int refillIntervalMinutes;
    private long lastRefillTick;
    private String groupId;

    // Posiciones de los cofres que gestiona esta área (registrados al escanear)
    private final Set<BlockPos> chestPositions = new HashSet<>();

    public ChestRefillArea(String id, AreaShape shape) {
        super(id, AreaType.CHEST_REFILL, shape);
        this.refillIntervalMinutes = 10;
        this.lastRefillTick = 0;
        this.groupId = null;
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
     * Rellena todos los cofres registrados con un sorteo fresco del grupo de
     * loot configurado. Si no hay grupo configurado, no hace nada (pero sí
     * actualiza el contador de tiempo, para no reintentar cada tick).
     */
    public void refill(ServerLevel level) {
        lastRefillTick = level.getGameTime();
        if (groupId == null) return;

        for (BlockPos pos : chestPositions) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                fillContainer(chest, level);
            }
        }
    }

    /**
     * Escanea la caja delimitadora de esta área, en todas las alturas del
     * mundo, en busca de cofres. Los vacíos se rellenan inmediatamente con
     * el grupo configurado; los que ya tienen contenido se dejan igual hasta
     * el siguiente relleno programado. Sustituye el registro de cofres
     * anterior por completo. Devuelve cuántos cofres se encontraron.
     */
    public int scanChests(ServerLevel level) {
        chestPositions.clear();

        int minX = (int) Math.floor(shape == AreaShape.CIRCLE ? centerX - radius : Math.min(x1, x2));
        int maxX = (int) Math.ceil(shape == AreaShape.CIRCLE ? centerX + radius : Math.max(x1, x2));
        int minZ = (int) Math.floor(shape == AreaShape.CIRCLE ? centerZ - radius : Math.min(z1, z2));
        int maxZ = (int) Math.ceil(shape == AreaShape.CIRCLE ? centerZ + radius : Math.max(z1, z2));

        int found = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!contains(x + 0.5, z + 0.5)) continue;

                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                        chestPositions.add(pos.immutable());
                        found++;
                        if (isEmpty(chest)) {
                            fillContainer(chest, level);
                        }
                    }
                }
            }
        }
        return found;
    }

    private boolean isEmpty(ChestBlockEntity chest) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (!chest.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    /** Vacía el cofre y coloca el sorteo del grupo en huecos aleatorios distintos. */
    private void fillContainer(ChestBlockEntity chest, ServerLevel level) {
        if (groupId == null) return;
        List<ItemStack> rolled = LootGroupManager.getInstance().rollItems(groupId, level.getRandom());

        int size = chest.getContainerSize();
        for (int i = 0; i < size; i++) {
            chest.setItem(i, ItemStack.EMPTY);
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) slots.add(i);
        // Fisher-Yates manual, usando el RandomSource del propio mundo
        for (int i = slots.size() - 1; i > 0; i--) {
            int j = level.getRandom().nextInt(i + 1);
            Collections.swap(slots, i, j);
        }

        for (int i = 0; i < rolled.size() && i < slots.size(); i++) {
            chest.setItem(slots.get(i), rolled.get(i));
        }
    }

    public Set<BlockPos> getChestPositions() { return chestPositions; }

    public void setChestPositions(Set<BlockPos> positions) {
        chestPositions.clear();
        chestPositions.addAll(positions);
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public int getRefillIntervalMinutes() { return refillIntervalMinutes; }
    public void setRefillIntervalMinutes(int minutes) { this.refillIntervalMinutes = minutes; }

    public long getLastRefillTick() { return lastRefillTick; }
    public void setLastRefillTick(long tick) { this.lastRefillTick = tick; }

    @Override
    protected String describeSpecific() {
        return "  Grupo de loot: " + (groupId != null ? groupId : "(sin configurar)") + "\n"
                + "  Intervalo de reposición: " + refillIntervalMinutes + " min\n"
                + "  Cofres registrados: " + chestPositions.size() + "\n";
    }
}