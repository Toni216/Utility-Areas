package com.cipollomods.utilityareas.event;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.types.DamageArea;
import com.cipollomods.utilityareas.area.types.PotionArea;
import com.cipollomods.utilityareas.area.types.SafeArea;
import com.cipollomods.utilityareas.data.AreaManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Manejador de eventos de Forge para la lógica de las áreas.
 * Gestiona la detección de entradas y salidas de jugadores,
 * la aplicación de efectos y daño, y el cancelado de spawns hostiles.
 */
@Mod.EventBusSubscriber(modid = "utilityareas", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AreaEventHandler {

    // Guarda en qué áreas estaba cada jugador en el tick anterior
    private static final Map<UUID, Set<String>> previousAreas = new HashMap<>();

    // Contador de ticks por jugador para DamageArea
    private static final Map<UUID, Map<String, Integer>> damageTicks = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Player player : level.players()) {
                handlePlayer(player, level);
                if (player instanceof ServerPlayer serverPlayer) {
                    AreaVisualizer.getInstance().tick(serverPlayer);
                }
            }

            // Tick de cada área activa
            for (Area area : AreaManager.getInstance().getActiveAreas()) {
                area.onTick(level);
            }
        }
    }

    /**
     * Procesa la posición de un jugador, detectando entradas y salidas de áreas
     * y aplicando los efectos correspondientes.
     */
    private static void handlePlayer(Player player, ServerLevel level) {
        UUID uuid = player.getUUID();
        double x = player.getX();
        double z = player.getZ();

        List<Area> currentAreasList = AreaManager.getInstance().getAreasContaining(x, z);
        Set<String> currentAreaIds = new HashSet<>();
        for (Area area : currentAreasList) {
            currentAreaIds.add(area.getId());
        }

        Set<String> previous = previousAreas.getOrDefault(uuid, new HashSet<>());

        // Detectar entradas
        for (Area area : currentAreasList) {
            if (!previous.contains(area.getId())) {
                area.onPlayerEnter(player);
            }
        }

        // Detectar salidas
        for (String areaId : previous) {
            if (!currentAreaIds.contains(areaId)) {
                AreaManager.getInstance().getArea(areaId).ifPresent(area -> area.onPlayerExit(player));
            }
        }

        // Aplicar efectos de PotionArea
        for (Area area : currentAreasList) {
            if (area instanceof PotionArea potionArea) {
                potionArea.applyEffects(player);
            }
        }

        // Aplicar daño de DamageArea
        Map<String, Integer> playerDamageTicks = damageTicks.computeIfAbsent(uuid, k -> new HashMap<>());
        for (Area area : currentAreasList) {
            if (area instanceof DamageArea damageArea) {
                int ticks = playerDamageTicks.getOrDefault(area.getId(), 0) + 1;
                if (ticks >= damageArea.getIntervalTicks()) {
                    damageArea.applyDamage(player, level);
                    ticks = 0;
                }
                playerDamageTicks.put(area.getId(), ticks);
            }
        }

        previousAreas.put(uuid, currentAreaIds);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster)) return;

        double x = event.getEntity().getX();
        double z = event.getEntity().getZ();

        for (Area area : AreaManager.getInstance().getAreasContaining(x, z)) {
            if (area instanceof SafeArea) {
                event.setSpawnCancelled(true);
                return;
            }
        }
    }
}