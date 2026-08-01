package com.cipollomods.utilityareas.data;

import com.cipollomods.utilityareas.UtilityAreas;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestor singleton de los grupos de loot configurables para
 * {@link com.cipollomods.utilityareas.area.types.ChestRefillArea}, cargados
 * desde {@code <mundo>/serverconfig/utilityareas/loot_groups.json}.
 *
 * <p>Se guarda dentro de la carpeta del propio mundo (en vez de en la
 * carpeta global {@code config/}) para que los grupos de loot viajen con
 * el save, igual que {@link AreaPersistenceManager}.</p>
 *
 * <p>Si el fichero no existe, se genera uno de ejemplo la primera vez, a
 * modo de plantilla editable.</p>
 */
public class LootGroupManager {

    private static final LootGroupManager INSTANCE = new LootGroupManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "loot_groups.json";

    private Map<String, LootGroup> groups = new LinkedHashMap<>();

    private LootGroupManager() {}

    public static LootGroupManager getInstance() {
        return INSTANCE;
    }

    /**
     * Devuelve la ruta de loot_groups.json dentro de
     * {@code <mundo>/serverconfig/utilityareas/}. Si no hay un servidor
     * corriendo todavía, cae de vuelta a la carpeta config/ global como
     * red de seguridad para no crashear.
     */
    private Path getFilePath() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Path base = (server != null)
                ? server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("utilityareas")
                : FMLPaths.CONFIGDIR.get().resolve("utilityareas");
        return base.resolve(FILE_NAME);
    }

    /** Carga (o recarga) los grupos desde disco. Genera un fichero de ejemplo si no existe todavía. */
    public void load() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                writeExampleFile(path);
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                Type mapType = new TypeToken<Map<String, LootGroup>>() {}.getType();
                Map<String, LootGroup> loaded = GSON.fromJson(reader, mapType);
                groups = loaded != null ? loaded : new LinkedHashMap<>();
            }
            UtilityAreas.LOGGER.info("Cargados {} grupo(s) de loot desde loot_groups.json", groups.size());
        } catch (IOException | com.google.gson.JsonParseException e) {
            UtilityAreas.LOGGER.error(
                    "Error al cargar loot_groups.json (¿fichero corrupto?). "
                            + "Los grupos de loot quedan vacíos hasta que se corrija el fichero y se recargue.", e);
            groups = new LinkedHashMap<>();
        }
    }

    private void writeExampleFile(Path path) throws IOException {
        Map<String, LootGroup> example = new LinkedHashMap<>();

        LootGroup comida = new LootGroup();
        comida.minItems = 2;
        comida.maxItems = 4;

        LootEntry pan = new LootEntry();
        pan.item = "minecraft:bread";
        pan.weight = 10;
        pan.minAmount = 1;
        pan.maxAmount = 4;

        LootEntry manzana = new LootEntry();
        manzana.item = "minecraft:apple";
        manzana.weight = 8;
        manzana.minAmount = 1;
        manzana.maxAmount = 3;

        LootEntry manzanaDorada = new LootEntry();
        manzanaDorada.item = "minecraft:golden_apple";
        manzanaDorada.weight = 1;
        manzanaDorada.minAmount = 1;
        manzanaDorada.maxAmount = 1;

        comida.items.add(pan);
        comida.items.add(manzana);
        comida.items.add(manzanaDorada);
        example.put("comida_basica", comida);

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(example, writer);
        }
    }

    /** Comprueba si existe un grupo con ese id. */
    public boolean exists(String groupId) {
        return groups.containsKey(groupId);
    }

    /** IDs de todos los grupos cargados, para autocompletado. */
    public Set<String> getGroupIds() {
        return groups.keySet();
    }

    /**
     * Sortea el contenido de un relleno: entre {@code minItems} y
     * {@code maxItems} pilas, cada una elegida por peso entre las entradas
     * del grupo. Devuelve una lista vacía si el grupo no existe, no tiene
     * entradas, o los pesos suman 0.
     */
    public List<ItemStack> rollItems(String groupId, RandomSource random) {
        LootGroup group = groups.get(groupId);
        if (group == null || group.items.isEmpty()) return List.of();

        int totalWeight = group.items.stream().mapToInt(e -> Math.max(e.weight, 0)).sum();
        if (totalWeight <= 0) return List.of();

        int span = Math.max(1, group.maxItems - group.minItems + 1);
        int count = group.minItems + random.nextInt(span);
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            LootEntry chosen = pickWeighted(group.items, totalWeight, random);
            if (chosen == null) continue;

            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(chosen.item));
            if (item == null) {
                UtilityAreas.LOGGER.warn("Item desconocido '{}' en un grupo de loot, se omite.", chosen.item);
                continue;
            }
            int amountSpan = Math.max(1, chosen.maxAmount - chosen.minAmount + 1);
            int amount = chosen.minAmount + random.nextInt(amountSpan);
            result.add(new ItemStack(item, Math.max(1, amount)));
        }
        return result;
    }

    private LootEntry pickWeighted(List<LootEntry> entries, int totalWeight, RandomSource random) {
        int roll = random.nextInt(totalWeight);
        int acc = 0;
        for (LootEntry entry : entries) {
            acc += Math.max(entry.weight, 0);
            if (roll < acc) return entry;
        }
        return null;
    }
}