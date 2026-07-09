package com.cipollomods.utilityareas.command;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.area.types.*;
import com.cipollomods.utilityareas.data.AreaManager;
import com.cipollomods.utilityareas.event.AreaVisualizer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class UACommand {

    /** Fuentes de daño válidas para DamageArea. Declarado aquí arriba porque
     *  el SuggestionProvider DAMAGE_SOURCES lo usa, y Java no permite que un
     *  campo estático se refiera (por nombre simple) a otro campo declarado
     *  más abajo en la misma clase. */
    private static final Set<String> VALID_DAMAGE_SOURCES = Set.of("generic", "fire", "magic", "void", "starve");

    // ============================================================
    // Proveedores de sugerencias (Tab) para argumentos de texto libre
    // ============================================================

    /** Sugiere los IDs de todas las áreas registradas actualmente. */
    private static final SuggestionProvider<CommandSourceStack> AREA_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    AreaManager.getInstance().getAllAreas().stream().map(Area::getId), builder);

    /** Sugiere los tipos de área válidos (safe, sign, chest_refill, potion, damage, teleport). */
    private static final SuggestionProvider<CommandSourceStack> AREA_TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(AreaType.values()).map(t -> t.name().toLowerCase()), builder);

    /** Sugiere los registry names de todos los efectos de poción disponibles. */
    private static final SuggestionProvider<CommandSourceStack> MOB_EFFECTS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(ForgeRegistries.MOB_EFFECTS.getKeys(), builder);

    /** Sugiere las fuentes de daño válidas para DamageArea. */
    private static final SuggestionProvider<CommandSourceStack> DAMAGE_SOURCES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(VALID_DAMAGE_SOURCES, builder);

    /** Sugiere los grupos de loot configurados en loot_groups.json. */
    private static final SuggestionProvider<CommandSourceStack> LOOT_GROUPS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    com.cipollomods.utilityareas.data.LootGroupManager.getInstance().getGroupIds(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ua")
                .requires(source -> source.hasPermission(4))
                .then(registerList())
                .then(registerInfo())
                .then(registerEnable())
                .then(registerDisable())
                .then(registerDelete())
                .then(registerWho())
                .then(registerScan())
                .then(registerWhereAmI())
                .then(registerCreate())
                .then(registerShow())
                .then(registerHide())
                .then(registerSet())
                .then(registerCorner1())
                .then(registerCorner2())
                .then(registerReload())
        );
    }

    // /ua list
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerList() {
        return Commands.literal("list")
                .executes(ctx -> {
                    var areas = AreaManager.getInstance().getAllAreas();
                    if (areas.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No hay áreas registradas."), false);
                        return 0;
                    }
                    StringBuilder sb = new StringBuilder("Áreas registradas:\n");
                    for (Area area : areas) {
                        sb.append("- ").append(area.getId())
                                .append(" [").append(area.getType()).append("]")
                                .append(" [").append(area.getShape()).append("]")
                                .append(area.isActive() ? " ✔" : " ✘")
                                .append("\n");
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                });
    }

    // /ua info <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerInfo() {
        return Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                ctx.getSource().sendSuccess(() -> Component.literal(area.toString()), false);
                                return 1;
                            }).orElseGet(() -> {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            });
                        }));
    }

    // /ua enable <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerEnable() {
        return Commands.literal("enable")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                area.setActive(true);
                                AreaManager.getInstance().save();
                                ctx.getSource().sendSuccess(() -> Component.literal("Área activada: " + id), false);
                                return 1;
                            }).orElseGet(() -> {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            });
                        }));
    }

    // /ua disable <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerDisable() {
        return Commands.literal("disable")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                area.setActive(false);
                                AreaManager.getInstance().save();
                                ctx.getSource().sendSuccess(() -> Component.literal("Área desactivada: " + id), false);
                                return 1;
                            }).orElseGet(() -> {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            });
                        }));
    }

    // /ua delete <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerDelete() {
        return Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!AreaManager.getInstance().exists(id)) {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            }
                            AreaManager.getInstance().removeArea(id);
                            AreaManager.getInstance().save();
                            ctx.getSource().sendSuccess(() -> Component.literal("Área eliminada: " + id), false);
                            return 1;
                        }));
    }

    // /ua who <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerWho() {
        return Commands.literal("who")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!AreaManager.getInstance().exists(id)) {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            }
                            Area area = AreaManager.getInstance().getArea(id).get();
                            var players = ctx.getSource().getServer().getPlayerList().getPlayers()
                                    .stream()
                                    .filter(p -> area.contains(p.getX(), p.getZ()))
                                    .map(p -> p.getName().getString())
                                    .toList();

                            String msg = players.isEmpty()
                                    ? "[" + id + "] No hay jugadores dentro."
                                    : "[" + id + "] Jugadores (" + players.size() + "): " + String.join(", ", players);
                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                            return 1;
                        }));
    }

    // /ua scan
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerScan() {
        return Commands.literal("scan")
                .executes(ctx -> {
                    var allPlayers = ctx.getSource().getServer().getPlayerList().getPlayers();
                    StringBuilder sb = new StringBuilder("Áreas con jugadores:\n");
                    boolean any = false;
                    for (Area area : AreaManager.getInstance().getActiveAreas()) {
                        long count = allPlayers.stream()
                                .filter(p -> area.contains(p.getX(), p.getZ()))
                                .count();
                        if (count > 0) {
                            sb.append("- ").append(area.getId()).append(": ").append(count).append(" jugador(es)\n");
                            any = true;
                        }
                    }
                    if (!any) sb.append("Ninguna área tiene jugadores dentro.");
                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                });
    }

    // /ua whereami
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerWhereAmI() {
        return Commands.literal("whereami")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
                        return 0;
                    }
                    var areas = AreaManager.getInstance().getAreasContaining(player.getX(), player.getZ());
                    String msg = areas.isEmpty()
                            ? "No estás dentro de ningún área."
                            : "Estás dentro de: " + String.join(", ", areas.stream().map(Area::getId).toList());
                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                });
    }

    // /ua create <id> <tipo> circle <x> <z> <radio>
    // /ua create <id> <tipo> rect <x1> <z1> <x2> <z2>
    // /ua create <id> <tipo> rect corner1   (marca esquina 1 en tu posición)
    // /ua create <id> <tipo> rect corner2   (marca esquina 2 y crea el área)
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerCreate() {
        var radioNode = Commands.argument("radio", DoubleArgumentType.doubleArg())
                .executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    String tipo = StringArgumentType.getString(ctx, "tipo");
                    double x = DoubleArgumentType.getDouble(ctx, "x");
                    double z = DoubleArgumentType.getDouble(ctx, "z");
                    double radio = DoubleArgumentType.getDouble(ctx, "radio");
                    return createArea(ctx.getSource(), id, tipo, AreaShape.CIRCLE, x, z, radio, 0, 0, 0, 0);
                });
        attachTeleportDestino(radioNode, UACommand::createTeleportCircle);

        var circleNode = Commands.literal("circle")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .then(radioNode)));

        // Esta parte, "rect <x1> <z1> <x2> <z2>", cuelga directamente de rectNode más abajo.
        var z2Node = Commands.argument("z2", DoubleArgumentType.doubleArg())
                .executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    String tipo = StringArgumentType.getString(ctx, "tipo");
                    double x1 = DoubleArgumentType.getDouble(ctx, "x1");
                    double z1 = DoubleArgumentType.getDouble(ctx, "z1");
                    double x2 = DoubleArgumentType.getDouble(ctx, "x2");
                    double z2 = DoubleArgumentType.getDouble(ctx, "z2");
                    return createArea(ctx.getSource(), id, tipo, AreaShape.RECT, 0, 0, 0, x1, z1, x2, z2);
                });
        attachTeleportDestino(z2Node, UACommand::createTeleportRect);

        var rectCoordsNode = Commands.argument("x1", DoubleArgumentType.doubleArg())
                .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                .then(z2Node)));

        var cornerNode = Commands.literal("corner")
                .executes(ctx -> createRectFromMarks(ctx) != null ? 1 : 0);
        attachTeleportDestino(cornerNode, UACommand::createTeleportRectFromMarks);

        var rectNode = Commands.literal("rect")
                .then(rectCoordsNode)
                .then(cornerNode);

        var tipoNode = Commands.argument("tipo", StringArgumentType.word())
                .suggests(AREA_TYPES)
                .then(circleNode)
                .then(rectNode);

        return Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(tipoNode));
    }

    /**
     * Crea el área y la registra/guarda. Devuelve la {@link Area} creada, o
     * null si falló (id repetido o tipo desconocido), para que quien la llame
     * pueda seguir configurándola (p. ej. el destino de un TeleportArea).
     */
    private static Area createAreaAndGet(CommandSourceStack source, String id, String tipo,
                                         AreaShape shape, double x, double z, double radio,
                                         double x1, double z1, double x2, double z2) {
        if (AreaManager.getInstance().exists(id)) {
            source.sendFailure(Component.literal("Ya existe un área con id: " + id));
            return null;
        }

        AreaType type;
        try {
            type = AreaType.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Tipo desconocido: " + tipo + ". Tipos válidos: safe, sign, chest_refill, potion, damage, teleport"));
            return null;
        }

        Area area = switch (type) {
            case SAFE -> new SafeArea(id, shape);
            case SIGN -> new SignArea(id, shape);
            case CHEST_REFILL -> new ChestRefillArea(id, shape);
            case POTION -> new PotionArea(id, shape);
            case DAMAGE -> new DamageArea(id, shape);
            case TELEPORT -> new TeleportArea(id, shape);
        };

        if (shape == AreaShape.CIRCLE) {
            Area.circle(area, x, z, radio);
        } else {
            Area.rect(area, x1, z1, x2, z2);
        }

        AreaManager.getInstance().addArea(area);
        AreaManager.getInstance().save();
        source.sendSuccess(() -> Component.literal("Área creada: " + id + " [" + type + "] [" + shape + "]"), false);
        return area;
    }

    // Wrapper con la firma antigua (devuelve el código de resultado que espera Brigadier)
    private static int createArea(CommandSourceStack source, String id, String tipo,
                                  AreaShape shape, double x, double z, double radio,
                                  double x1, double z1, double x2, double z2) {
        return createAreaAndGet(source, id, tipo, shape, x, z, radio, x1, z1, x2, z2) != null ? 1 : 0;
    }

    // --- Creación de TeleportArea con destino indicado en el propio /ua create ---

    private static Area createTeleportCircle(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String tipo = StringArgumentType.getString(ctx, "tipo");
        if (!"teleport".equalsIgnoreCase(tipo)) {
            ctx.getSource().sendFailure(Component.literal("El destino solo se puede indicar al crear áreas de tipo teleport."));
            return null;
        }
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        double radio = DoubleArgumentType.getDouble(ctx, "radio");
        return createAreaAndGet(ctx.getSource(), id, tipo, AreaShape.CIRCLE, x, z, radio, 0, 0, 0, 0);
    }

    private static Area createTeleportRect(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String tipo = StringArgumentType.getString(ctx, "tipo");
        if (!"teleport".equalsIgnoreCase(tipo)) {
            ctx.getSource().sendFailure(Component.literal("El destino solo se puede indicar al crear áreas de tipo teleport."));
            return null;
        }
        double x1 = DoubleArgumentType.getDouble(ctx, "x1");
        double z1 = DoubleArgumentType.getDouble(ctx, "z1");
        double x2 = DoubleArgumentType.getDouble(ctx, "x2");
        double z2 = DoubleArgumentType.getDouble(ctx, "z2");
        return createAreaAndGet(ctx.getSource(), id, tipo, AreaShape.RECT, 0, 0, 0, x1, z1, x2, z2);
    }

    private static int createTeleportRandom(CommandSourceStack source, Area area) {
        if (area == null) return 0;
        ((TeleportArea) area).setMode(TeleportArea.TeleportMode.RANDOM);
        AreaManager.getInstance().save();
        source.sendSuccess(() -> Component.literal("Destino de '" + area.getId() + "' establecido a aleatorio."), false);
        return 1;
    }

    private static int createTeleportToArea(CommandSourceStack source, Area area, String destAreaId) {
        if (area == null) return 0;
        if (!AreaManager.getInstance().exists(destAreaId)) {
            source.sendFailure(Component.literal(
                    "Área destino no encontrada: " + destAreaId + ". El área se creó, pero sin destino configurado."));
            return 1;
        }
        TeleportArea tp = (TeleportArea) area;
        tp.setDestAreaId(destAreaId);
        tp.setMode(TeleportArea.TeleportMode.FIXED_AREA);
        AreaManager.getInstance().save();
        source.sendSuccess(() -> Component.literal("Destino de '" + area.getId() + "' establecido al área: " + destAreaId), false);
        return 1;
    }

    private static int createTeleportCoords(CommandSourceStack source, Area area,
                                            double x, double y, double z, float yaw, float pitch) {
        if (area == null) return 0;
        TeleportArea tp = (TeleportArea) area;
        tp.setDestination(x, y, z, yaw, pitch);
        tp.setMode(TeleportArea.TeleportMode.FIXED_COORDS);
        AreaManager.getInstance().save();
        source.sendSuccess(() -> Component.literal(
                "Destino de '" + area.getId() + "' actualizado a (" + x + ", " + y + ", " + z + ")."), false);
        return 1;
    }

    /**
     * Añade las ramas opcionales "random", "area <id>" y "coords <x> <y> <z> [yaw pitch]"
     * a un nodo de argumento numérico (el radio del círculo o la z2 del rectángulo),
     * para poder indicar el destino de una TeleportArea en el propio /ua create.
     * Si el área creada no es de tipo teleport, los handlers rechazan el destino
     * en tiempo de ejecución (no se puede comprobar antes, porque "tipo" es texto libre).
     */
    private static void attachTeleportDestino(
            com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> parent,
            java.util.function.Function<CommandContext<CommandSourceStack>, Area> creator) {

        parent.then(Commands.literal("random")
                .executes(ctx -> createTeleportRandom(ctx.getSource(), creator.apply(ctx))));

        parent.then(Commands.literal("area")
                .then(Commands.argument("area_destino", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> createTeleportToArea(ctx.getSource(), creator.apply(ctx),
                                StringArgumentType.getString(ctx, "area_destino")))));

        parent.then(Commands.literal("coords")
                .then(Commands.argument("dx", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("dy", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("dz", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> createTeleportCoords(ctx.getSource(), creator.apply(ctx),
                                                DoubleArgumentType.getDouble(ctx, "dx"),
                                                DoubleArgumentType.getDouble(ctx, "dy"),
                                                DoubleArgumentType.getDouble(ctx, "dz"), 0f, 0f))
                                        .then(Commands.argument("dyaw", FloatArgumentType.floatArg())
                                                .then(Commands.argument("dpitch", FloatArgumentType.floatArg())
                                                        .executes(ctx -> createTeleportCoords(ctx.getSource(), creator.apply(ctx),
                                                                DoubleArgumentType.getDouble(ctx, "dx"),
                                                                DoubleArgumentType.getDouble(ctx, "dy"),
                                                                DoubleArgumentType.getDouble(ctx, "dz"),
                                                                FloatArgumentType.getFloat(ctx, "dyaw"),
                                                                FloatArgumentType.getFloat(ctx, "dpitch")))))))));
    }

    // /ua show <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerShow() {
        return Commands.literal("show")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
                                return 0;
                            }
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                AreaVisualizer.getInstance().show(player, area);
                                ctx.getSource().sendSuccess(() -> Component.literal("Mostrando área: " + id), false);
                                return 1;
                            }).orElseGet(() -> {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            });
                        }));
    }

    // /ua hide
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerHide() {
        return Commands.literal("hide")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
                        return 0;
                    }
                    AreaVisualizer.getInstance().hide(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Visualización ocultada."), false);
                    return 1;
                });
    }

    // ============================================================
    // Marcado de esquinas (estilo WorldEdit) para /ua create ... rect corner
    // ============================================================

    // Posición [x, z] marcada por cada jugador. Independiente de cualquier
    // id/tipo: se marca primero, y se decide el área a crear después.
    private static final Map<UUID, double[]> corner1Marks = new HashMap<>();
    private static final Map<UUID, double[]> corner2Marks = new HashMap<>();

    // /ua corner1 — marca la esquina 1 en tu posición actual
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerCorner1() {
        return Commands.literal("corner1")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
                        return 0;
                    }
                    double x = player.getX();
                    double z = player.getZ();
                    corner1Marks.put(player.getUUID(), new double[]{x, z});
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Esquina 1 marcada en (" + x + ", " + z + "). Ve a la otra esquina y usa /ua corner2."), false);
                    return 1;
                });
    }

    // /ua corner2 — marca la esquina 2 en tu posición actual
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerCorner2() {
        return Commands.literal("corner2")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
                        return 0;
                    }
                    double x = player.getX();
                    double z = player.getZ();
                    corner2Marks.put(player.getUUID(), new double[]{x, z});
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Esquina 2 marcada en (" + x + ", " + z + "). Ya puedes crear el área con: "
                                    + "/ua create <id> <tipo> rect corner"), false);
                    return 1;
                });
    }

    // /ua reload loot — recarga loot_groups.json desde disco sin reiniciar el servidor
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerReload() {
        return Commands.literal("reload")
                .then(Commands.literal("loot")
                        .executes(ctx -> {
                            com.cipollomods.utilityareas.data.LootGroupManager.getInstance().load();
                            int count = com.cipollomods.utilityareas.data.LootGroupManager.getInstance().getGroupIds().size();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "loot_groups.json recargado (" + count + " grupo(s))."), false);
                            return 1;
                        }));
    }

    /**
     * Recupera las dos esquinas marcadas por el jugador. Devuelve null (y avisa)
     * si al jugador o alguna de las dos esquinas le falta por marcar.
     */
    private static double[] resolveMarkedCorners(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
            return null;
        }
        double[] c1 = corner1Marks.get(player.getUUID());
        double[] c2 = corner2Marks.get(player.getUUID());
        if (c1 == null || c2 == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Necesitas marcar las dos esquinas primero: ve a una y usa /ua corner1, "
                            + "ve a la otra y usa /ua corner2."));
            return null;
        }
        return new double[]{c1[0], c1[1], c2[0], c2[1]};
    }

    // /ua create <id> <tipo> rect corner — crea el área usando las esquinas ya marcadas
    private static Area createRectFromMarks(CommandContext<CommandSourceStack> ctx) {
        double[] corners = resolveMarkedCorners(ctx);
        if (corners == null) return null;

        String id = StringArgumentType.getString(ctx, "id");
        String tipo = StringArgumentType.getString(ctx, "tipo");
        Area area = createAreaAndGet(ctx.getSource(), id, tipo, AreaShape.RECT, 0, 0, 0,
                corners[0], corners[1], corners[2], corners[3]);

        if (area != null) {
            ServerPlayer player = (ServerPlayer) ctx.getSource().getEntity();
            corner1Marks.remove(player.getUUID());
            corner2Marks.remove(player.getUUID());
        }
        return area;
    }

    // Variante para cuando además se indica el destino de un TeleportArea en el mismo create
    private static Area createTeleportRectFromMarks(CommandContext<CommandSourceStack> ctx) {
        String tipo = StringArgumentType.getString(ctx, "tipo");
        if (!"teleport".equalsIgnoreCase(tipo)) {
            ctx.getSource().sendFailure(Component.literal("El destino solo se puede indicar al crear áreas de tipo teleport."));
            return null;
        }
        return createRectFromMarks(ctx);
    }

    // ============================================================
    // /ua set <id> <tipo> <propiedad> <valor...>
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> registerSet() {
        return Commands.literal("set")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(AREA_IDS)
                        .then(registerSetSign())
                        .then(registerSetChest())
                        .then(registerSetPotion())
                        .then(registerSetDamage())
                        .then(registerSetTeleport())
                );
    }

    // --- sign ---
    private static LiteralArgumentBuilder<CommandSourceStack> registerSetSign() {
        return Commands.literal("sign")
                .then(Commands.literal("enter")
                        .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> setSignMessage(ctx, true))))
                .then(Commands.literal("exit")
                        .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                .executes(ctx -> setSignMessage(ctx, false))))
                .then(Commands.literal("cooldown")
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(0))
                                .executes(UACommand::setSignCooldown)));
    }

    private static int setSignMessage(CommandContext<CommandSourceStack> ctx, boolean isEnter) {
        String id = StringArgumentType.getString(ctx, "id");
        String mensaje = StringArgumentType.getString(ctx, "mensaje");

        SignArea area = getTypedArea(ctx.getSource(), id, SignArea.class, "sign");
        if (area == null) return 0;

        if (isEnter) {
            area.setMessageEnter(mensaje);
        } else {
            area.setMessageExit(mensaje);
        }
        AreaManager.getInstance().save();

        String tipo = isEnter ? "entrada" : "salida";
        ctx.getSource().sendSuccess(() -> Component.literal("Mensaje de " + tipo + " de '" + id + "' actualizado."), false);
        return 1;
    }

    private static int setSignCooldown(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        int segundos = IntegerArgumentType.getInteger(ctx, "segundos");

        SignArea area = getTypedArea(ctx.getSource(), id, SignArea.class, "sign");
        if (area == null) return 0;

        area.setCooldownSeconds(segundos);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Cooldown de mensajes de '" + id + "' establecido a " + segundos + "s."), false);
        return 1;
    }

    // --- chest_refill ---
    private static LiteralArgumentBuilder<CommandSourceStack> registerSetChest() {
        return Commands.literal("chest")
                .then(Commands.literal("interval")
                        .then(Commands.argument("minutos", IntegerArgumentType.integer(1))
                                .executes(UACommand::setChestInterval)))
                .then(Commands.literal("group")
                        .then(Commands.argument("grupo", StringArgumentType.word())
                                .suggests(LOOT_GROUPS)
                                .executes(UACommand::setChestGroup)))
                .then(Commands.literal("scan")
                        .executes(UACommand::scanChest));
    }

    private static int setChestInterval(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        int minutos = IntegerArgumentType.getInteger(ctx, "minutos");

        ChestRefillArea area = getTypedArea(ctx.getSource(), id, ChestRefillArea.class, "chest_refill");
        if (area == null) return 0;

        area.setRefillIntervalMinutes(minutos);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Intervalo de reposición de '" + id + "' establecido a " + minutos + " min."), false);
        return 1;
    }

    private static int setChestGroup(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String grupo = StringArgumentType.getString(ctx, "grupo");

        ChestRefillArea area = getTypedArea(ctx.getSource(), id, ChestRefillArea.class, "chest_refill");
        if (area == null) return 0;

        if (!com.cipollomods.utilityareas.data.LootGroupManager.getInstance().exists(grupo)) {
            ctx.getSource().sendFailure(Component.literal(
                    "Grupo de loot no encontrado: " + grupo + ". Revisa config/utilityareas/loot_groups.json."));
            return 0;
        }

        area.setGroupId(grupo);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Grupo de loot de '" + id + "' establecido a: " + grupo), false);
        return 1;
    }

    private static int scanChest(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");

        ChestRefillArea area = getTypedArea(ctx.getSource(), id, ChestRefillArea.class, "chest_refill");
        if (area == null) return 0;

        if (area.getGroupId() == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Configura primero un grupo con: /ua set " + id + " chest group <grupo>"));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        int found = area.scanChests(level);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Escaneados " + found + " cofre(s) en '" + id + "'. Los que estaban vacíos ya se han rellenado."), false);
        return 1;
    }

    // --- potion ---
    private static LiteralArgumentBuilder<CommandSourceStack> registerSetPotion() {
        return Commands.literal("potion")
                .then(Commands.literal("add")
                        .then(Commands.argument("efecto", ResourceLocationArgument.id())
                                .suggests(MOB_EFFECTS)
                                .then(Commands.argument("duracion", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("amplificador", IntegerArgumentType.integer(0))
                                                .executes(UACommand::addPotionEffect)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("efecto", ResourceLocationArgument.id())
                                .suggests(MOB_EFFECTS)
                                .executes(UACommand::removePotionEffect)))
                .then(Commands.literal("clear")
                        .executes(UACommand::clearPotionEffects))
                .then(Commands.literal("removeonexit")
                        .then(Commands.argument("valor", BoolArgumentType.bool())
                                .executes(UACommand::setPotionRemoveOnExit)));
    }

    private static int addPotionEffect(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ResourceLocation rl = ResourceLocationArgument.getId(ctx, "efecto");
        int duracionSeg = IntegerArgumentType.getInteger(ctx, "duracion");
        int amplificador = IntegerArgumentType.getInteger(ctx, "amplificador");

        PotionArea area = getTypedArea(ctx.getSource(), id, PotionArea.class, "potion");
        if (area == null) return 0;

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect == null) {
            ctx.getSource().sendFailure(Component.literal("Efecto desconocido: " + rl));
            return 0;
        }

        area.addEffect(effect, duracionSeg * 20, amplificador);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Efecto añadido a '" + id + "': " + rl + " (nivel " + (amplificador + 1) + ", " + duracionSeg + "s)"), false);
        return 1;
    }

    private static int removePotionEffect(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ResourceLocation rl = ResourceLocationArgument.getId(ctx, "efecto");

        PotionArea area = getTypedArea(ctx.getSource(), id, PotionArea.class, "potion");
        if (area == null) return 0;

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect == null) {
            ctx.getSource().sendFailure(Component.literal("Efecto desconocido: " + rl));
            return 0;
        }

        area.removeEffect(effect);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Efecto " + rl + " eliminado de '" + id + "'."), false);
        return 1;
    }

    private static int clearPotionEffects(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");

        PotionArea area = getTypedArea(ctx.getSource(), id, PotionArea.class, "potion");
        if (area == null) return 0;

        area.clearEffects();
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Todos los efectos de '" + id + "' eliminados."), false);
        return 1;
    }

    private static int setPotionRemoveOnExit(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        boolean valor = BoolArgumentType.getBool(ctx, "valor");

        PotionArea area = getTypedArea(ctx.getSource(), id, PotionArea.class, "potion");
        if (area == null) return 0;

        area.setRemoveOnExit(valor);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("removeOnExit de '" + id + "' establecido a " + valor + "."), false);
        return 1;
    }

    // --- damage ---

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetDamage() {
        return Commands.literal("damage")
                .then(Commands.literal("amount")
                        .then(Commands.argument("cantidad", FloatArgumentType.floatArg(0))
                                .executes(UACommand::setDamageAmount)))
                .then(Commands.literal("interval")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                .executes(UACommand::setDamageInterval)))
                .then(Commands.literal("source")
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests(DAMAGE_SOURCES)
                                .executes(UACommand::setDamageSource)));
    }

    private static int setDamageAmount(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        float cantidad = FloatArgumentType.getFloat(ctx, "cantidad");

        DamageArea area = getTypedArea(ctx.getSource(), id, DamageArea.class, "damage");
        if (area == null) return 0;

        area.setDamageAmount(cantidad);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Daño de '" + id + "' establecido a " + cantidad + "."), false);
        return 1;
    }

    private static int setDamageInterval(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");

        DamageArea area = getTypedArea(ctx.getSource(), id, DamageArea.class, "damage");
        if (area == null) return 0;

        area.setIntervalTicks(ticks);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Intervalo de daño de '" + id + "' establecido a " + ticks + " ticks."), false);
        return 1;
    }

    private static int setDamageSource(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String tipo = StringArgumentType.getString(ctx, "tipo").toLowerCase();

        DamageArea area = getTypedArea(ctx.getSource(), id, DamageArea.class, "damage");
        if (area == null) return 0;

        if (!VALID_DAMAGE_SOURCES.contains(tipo)) {
            ctx.getSource().sendFailure(Component.literal(
                    "Tipo de daño inválido: " + tipo + ". Válidos: " + String.join(", ", VALID_DAMAGE_SOURCES)));
            return 0;
        }

        area.setDamageSourceType(tipo);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Fuente de daño de '" + id + "' establecida a: " + tipo), false);
        return 1;
    }

    // --- teleport ---
    private static LiteralArgumentBuilder<CommandSourceStack> registerSetTeleport() {
        return Commands.literal("teleport")
                .then(Commands.literal("dest")
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> setTeleportDest(ctx, false))
                                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg())
                                                                .executes(ctx -> setTeleportDest(ctx, true)))))))
                        .then(Commands.literal("area")
                                .then(Commands.argument("area_destino", StringArgumentType.word())
                                        .suggests(AREA_IDS)
                                        .executes(UACommand::setTeleportDestArea))))
                .then(Commands.literal("here")
                        .executes(UACommand::setTeleportHere))
                .then(Commands.literal("mode")
                        .then(Commands.literal("fixed")
                                .executes(ctx -> setTeleportMode(ctx, TeleportArea.TeleportMode.FIXED_COORDS)))
                        .then(Commands.literal("area")
                                .executes(ctx -> setTeleportMode(ctx, TeleportArea.TeleportMode.FIXED_AREA)))
                        .then(Commands.literal("random")
                                .executes(ctx -> setTeleportMode(ctx, TeleportArea.TeleportMode.RANDOM))))
                .then(Commands.literal("random")
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radio", DoubleArgumentType.doubleArg(1))
                                        .executes(UACommand::setTeleportRandomRadius)))
                        .then(Commands.literal("center")
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(UACommand::setTeleportRandomCenter)))
                                .then(Commands.literal("reset")
                                        .executes(UACommand::resetTeleportRandomCenter))))
                .then(Commands.literal("cooldown")
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(0))
                                .executes(UACommand::setTeleportCooldown)));
    }

    private static int setTeleportDest(CommandContext<CommandSourceStack> ctx, boolean conRotacion) {
        String id = StringArgumentType.getString(ctx, "id");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        float yaw, pitch;
        if (conRotacion) {
            yaw = FloatArgumentType.getFloat(ctx, "yaw");
            pitch = FloatArgumentType.getFloat(ctx, "pitch");
        } else {
            // Conservamos la rotación de destino que ya tuviera el área
            yaw = area.getDestYaw();
            pitch = area.getDestPitch();
        }

        area.setDestination(x, y, z, yaw, pitch);
        area.setMode(TeleportArea.TeleportMode.FIXED_COORDS);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Destino de '" + id + "' actualizado a (" + x + ", " + y + ", " + z + ")."), false);
        return 1;
    }

    private static int setTeleportDestArea(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String areaDestino = StringArgumentType.getString(ctx, "area_destino");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        if (!AreaManager.getInstance().exists(areaDestino)) {
            ctx.getSource().sendFailure(Component.literal("Área destino no encontrada: " + areaDestino));
            return 0;
        }
        if (areaDestino.equals(id)) {
            ctx.getSource().sendFailure(Component.literal("Un área no puede teletransportar a sí misma."));
            return 0;
        }

        area.setDestAreaId(areaDestino);
        area.setMode(TeleportArea.TeleportMode.FIXED_AREA);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Destino de '" + id + "' establecido al área: " + areaDestino), false);
        return 1;
    }

    private static int setTeleportHere(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");

        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Solo jugadores pueden usar este comando."));
            return 0;
        }

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        area.setDestination(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        area.setMode(TeleportArea.TeleportMode.FIXED_COORDS);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Destino de '" + id + "' establecido en tu posición actual."), false);
        return 1;
    }

    private static int setTeleportMode(CommandContext<CommandSourceStack> ctx, TeleportArea.TeleportMode mode) {
        String id = StringArgumentType.getString(ctx, "id");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        if (mode == TeleportArea.TeleportMode.FIXED_AREA && area.getDestAreaId() == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Primero define un área destino con: /ua set " + id + " teleport dest area <area_destino>"));
            return 0;
        }

        area.setMode(mode);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Modo de teletransporte de '" + id + "' establecido a: " + mode), false);
        return 1;
    }

    private static int setTeleportRandomRadius(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        double radio = DoubleArgumentType.getDouble(ctx, "radio");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        area.setRandomRadius(radio);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Radio aleatorio de '" + id + "' establecido a " + radio + "."), false);
        return 1;
    }

    private static int setTeleportRandomCenter(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double z = DoubleArgumentType.getDouble(ctx, "z");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        area.setRandomCenter(x, z);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Centro aleatorio de '" + id + "' establecido a (" + x + ", " + z + ")."), false);
        return 1;
    }

    private static int resetTeleportRandomCenter(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        area.resetRandomCenter();
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Centro aleatorio de '" + id + "' restablecido al centro propio del área."), false);
        return 1;
    }

    private static int setTeleportCooldown(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        int segundos = IntegerArgumentType.getInteger(ctx, "segundos");

        TeleportArea area = getTypedArea(ctx.getSource(), id, TeleportArea.class, "teleport");
        if (area == null) return 0;

        area.setCooldownSeconds(segundos);
        AreaManager.getInstance().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Cooldown de teletransporte de '" + id + "' establecido a " + segundos + "s."), false);
        return 1;
    }

    // --- helper común ---

    /**
     * Obtiene el área con el id dado y comprueba que sea del tipo esperado.
     * Si no existe, o si existe pero es de otro tipo, envía un mensaje de
     * error al jugador y devuelve null.
     */
    private static <T extends Area> T getTypedArea(CommandSourceStack source, String id, Class<T> clazz, String tipoEsperado) {
        Area area = AreaManager.getInstance().getArea(id).orElse(null);
        if (area == null) {
            source.sendFailure(Component.literal("Área no encontrada: " + id));
            return null;
        }
        if (!clazz.isInstance(area)) {
            source.sendFailure(Component.literal(
                    "El área '" + id + "' no es de tipo " + tipoEsperado + " (es " + area.getType() + ")."));
            return null;
        }
        return clazz.cast(area);
    }
}