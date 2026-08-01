package com.cipollomods.utilityareas.data;

import com.cipollomods.utilityareas.UtilityAreas;
import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.area.types.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestor singleton de la persistencia de áreas en disco.
 * Guarda y carga el estado de {@link AreaManager} en
 * {@code <mundo>/serverconfig/utilityareas/areas.json} usando Gson.
 *
 * <p>Se guarda dentro de la carpeta del propio mundo (en vez de en la
 * carpeta global {@code config/}) para que las áreas viajen con el save:
 * cambiar de mundo o restaurar un backup no deja atrás las áreas creadas.</p>
 *
 * <p>Convierte cada {@link Area} a/desde {@link AreaData} porque Gson no
 * reconstruye bien jerarquías de clases abstractas por sí solo.</p>
 */
public class AreaPersistenceManager {

    private static final AreaPersistenceManager INSTANCE = new AreaPersistenceManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "areas.json";

    private AreaPersistenceManager() {}

    public static AreaPersistenceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Devuelve la ruta de areas.json dentro de {@code <mundo>/serverconfig/utilityareas/}.
     * Si por algún motivo no hay un servidor corriendo todavía (no debería
     * pasar en el flujo normal, ya que load()/save() solo se llaman con el
     * server activo), cae de vuelta a la carpeta config/ global como red de
     * seguridad para no crashear.
     */
    private Path getFilePath() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Path base = (server != null)
                ? server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("utilityareas")
                : FMLPaths.CONFIGDIR.get().resolve("utilityareas");
        return base.resolve(FILE_NAME);
    }

    /**
     * Vuelca todas las áreas actuales de {@link AreaManager} a areas.json.
     * Se llama al final de cualquier comando que modifique el estado.
     */
    public void save() {
        Path path = getFilePath();
        Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());

            List<AreaData> dataList = new ArrayList<>();
            for (Area area : AreaManager.getInstance().getAllAreas()) {
                dataList.add(toData(area));
            }

            // Escribimos primero a un fichero temporal y solo sustituimos el
            // real al final, para no dejar areas.json a medias si el proceso
            // se interrumpe (crash, corte de luz) justo durante la escritura.
            try (Writer writer = Files.newBufferedWriter(tmpPath)) {
                GSON.toJson(dataList, writer);
            }
            Files.move(tmpPath, path,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            UtilityAreas.LOGGER.error("Error al guardar areas.json", e);
        }
    }

    /**
     * Carga las áreas desde areas.json y las registra en {@link AreaManager}
     * mediante {@link AreaManager#addAreaSilent(Area)}. Si el fichero no
     * existe todavía (primer arranque), no hace nada.
     */
    public void load() {
        Path path = getFilePath();
        if (!Files.exists(path)) {
            UtilityAreas.LOGGER.info("areas.json no existe todavía, no se carga ninguna área.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<AreaData>>() {}.getType();
            List<AreaData> dataList = GSON.fromJson(reader, listType);
            if (dataList == null) return;

            AreaManager.getInstance().clear();
            int loaded = 0;
            for (AreaData data : dataList) {
                Area area = fromData(data);
                if (area != null) {
                    AreaManager.getInstance().addAreaSilent(area);
                    loaded++;
                }
            }
            UtilityAreas.LOGGER.info("Cargadas {} área(s) desde areas.json", loaded);
        } catch (IOException | com.google.gson.JsonParseException e) {
            UtilityAreas.LOGGER.error(
                    "Error al cargar areas.json (¿fichero corrupto?). El servidor sigue arrancando sin áreas cargadas.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Area -> AreaData
    // -------------------------------------------------------------------------

    private AreaData toData(Area area) {
        AreaData data = new AreaData();
        data.id = area.getId();
        data.type = area.getType().name();
        data.shape = area.getShape().name();
        data.active = area.isActive();

        if (area.getShape() == AreaShape.CIRCLE) {
            data.centerX = area.getCenterX();
            data.centerZ = area.getCenterZ();
            data.radius = area.getRadius();
        } else {
            data.x1 = area.getX1();
            data.z1 = area.getZ1();
            data.x2 = area.getX2();
            data.z2 = area.getZ2();
        }

        switch (area.getType()) {
            case SIGN -> fillSignData(data, (SignArea) area);
            case CHEST_REFILL -> fillChestData(data, (ChestRefillArea) area);
            case POTION -> fillPotionData(data, (PotionArea) area);
            case DAMAGE -> fillDamageData(data, (DamageArea) area);
            case TELEPORT -> fillTeleportData(data, (TeleportArea) area);
            case SAFE -> { /* SafeArea no tiene campos propios */ }
        }

        return data;
    }

    private void fillSignData(AreaData data, SignArea area) {
        data.messageEnter = area.getMessageEnter();
        data.messageExit = area.getMessageExit();
        data.signCooldownSeconds = area.getCooldownSeconds();
    }

    private void fillChestData(AreaData data, ChestRefillArea area) {
        data.refillIntervalMinutes = area.getRefillIntervalMinutes();
        data.chestGroupId = area.getGroupId();
        for (BlockPos pos : area.getChestPositions()) {
            data.chestPositions.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
    }

    private void fillPotionData(AreaData data, PotionArea area) {
        data.removeOnExit = area.isRemoveOnExit();
        for (MobEffectInstance instance : area.getEffects()) {
            ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
            if (rl == null) continue; // efecto de un mod que ya no está cargado
            data.effects.add(new PotionEffectData(rl.toString(), instance.getDuration(), instance.getAmplifier()));
        }
    }

    private void fillDamageData(AreaData data, DamageArea area) {
        data.damageAmount = area.getDamageAmount();
        data.damageIntervalTicks = area.getIntervalTicks();
        data.damageSourceType = area.getDamageSourceType();
    }

    private void fillTeleportData(AreaData data, TeleportArea area) {
        data.teleportMode = area.getMode().name();
        data.destX = area.getDestX();
        data.destY = area.getDestY();
        data.destZ = area.getDestZ();
        data.destYaw = area.getDestYaw();
        data.destPitch = area.getDestPitch();
        data.destAreaId = area.getDestAreaId();
        data.randomCenterX = area.getRandomCenterX();
        data.randomCenterZ = area.getRandomCenterZ();
        data.randomRadius = area.getRandomRadius();
        data.teleportCooldownSeconds = area.getCooldownSeconds();
    }

    // -------------------------------------------------------------------------
    // AreaData -> Area
    // -------------------------------------------------------------------------

    private Area fromData(AreaData data) {
        AreaType type;
        AreaShape shape;
        try {
            type = AreaType.valueOf(data.type);
            shape = AreaShape.valueOf(data.shape);
        } catch (IllegalArgumentException e) {
            UtilityAreas.LOGGER.warn("Área '{}' con tipo/forma desconocidos, se ignora al cargar.", data.id);
            return null;
        }

        Area area = switch (type) {
            case SAFE -> new SafeArea(data.id, shape);
            case SIGN -> new SignArea(data.id, shape);
            case CHEST_REFILL -> new ChestRefillArea(data.id, shape);
            case POTION -> new PotionArea(data.id, shape);
            case DAMAGE -> new DamageArea(data.id, shape);
            case TELEPORT -> new TeleportArea(data.id, shape);
        };

        if (shape == AreaShape.CIRCLE) {
            Area.circle(area, data.centerX, data.centerZ, data.radius);
        } else {
            Area.rect(area, data.x1, data.z1, data.x2, data.z2);
        }
        area.setActive(data.active);

        switch (type) {
            case SIGN -> applySignData(data, (SignArea) area);
            case CHEST_REFILL -> applyChestData(data, (ChestRefillArea) area);
            case POTION -> applyPotionData(data, (PotionArea) area);
            case DAMAGE -> applyDamageData(data, (DamageArea) area);
            case TELEPORT -> applyTeleportData(data, (TeleportArea) area);
            case SAFE -> { /* nada que aplicar */ }
        }

        return area;
    }

    private void applySignData(AreaData data, SignArea area) {
        if (data.messageEnter != null) area.setMessageEnter(data.messageEnter);
        if (data.messageExit != null) area.setMessageExit(data.messageExit);
        area.setCooldownSeconds(data.signCooldownSeconds);
    }

    private void applyChestData(AreaData data, ChestRefillArea area) {
        if (data.refillIntervalMinutes > 0) {
            area.setRefillIntervalMinutes(data.refillIntervalMinutes);
        }
        area.setGroupId(data.chestGroupId);

        if (data.chestPositions != null) {
            Set<BlockPos> positions = new HashSet<>();
            for (String raw : data.chestPositions) {
                String[] parts = raw.split(",");
                if (parts.length != 3) continue;
                try {
                    positions.add(new BlockPos(
                            Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                } catch (NumberFormatException e) {
                    UtilityAreas.LOGGER.warn("Posición de cofre inválida '{}' en el área '{}', se omite.", raw, data.id);
                }
            }
            area.setChestPositions(positions);
        }
    }

    private void applyPotionData(AreaData data, PotionArea area) {
        area.setRemoveOnExit(data.removeOnExit);
        if (data.effects == null) return;
        for (PotionEffectData effectData : data.effects) {
            ResourceLocation rl = ResourceLocation.tryParse(effectData.effectId);
            if (rl == null) continue;
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
            if (effect == null) {
                UtilityAreas.LOGGER.warn("Efecto '{}' no encontrado al cargar el área '{}', se omite.",
                        effectData.effectId, data.id);
                continue;
            }
            area.addEffect(effect, effectData.durationTicks, effectData.amplifier);
        }
    }

    private void applyDamageData(AreaData data, DamageArea area) {
        area.setDamageAmount(data.damageAmount);
        if (data.damageIntervalTicks > 0) area.setIntervalTicks(data.damageIntervalTicks);
        if (data.damageSourceType != null) area.setDamageSourceType(data.damageSourceType);
    }

    private void applyTeleportData(AreaData data, TeleportArea area) {
        area.setDestination(data.destX, data.destY, data.destZ, data.destYaw, data.destPitch);
        area.setDestAreaId(data.destAreaId);
        if (data.randomCenterX != null && data.randomCenterZ != null) {
            area.setRandomCenter(data.randomCenterX, data.randomCenterZ);
        }
        if (data.randomRadius > 0) area.setRandomRadius(data.randomRadius);
        area.setCooldownSeconds(data.teleportCooldownSeconds);

        try {
            area.setMode(TeleportArea.TeleportMode.valueOf(data.teleportMode));
        } catch (IllegalArgumentException | NullPointerException e) {
            area.setMode(TeleportArea.TeleportMode.FIXED_COORDS);
        }
    }
}