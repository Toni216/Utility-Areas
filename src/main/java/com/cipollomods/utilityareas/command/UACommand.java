package com.cipollomods.utilityareas.command;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import com.cipollomods.utilityareas.area.types.*;
import com.cipollomods.utilityareas.data.AreaManager;
import com.cipollomods.utilityareas.event.AreaVisualizer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registro y lógica de todos los subcomandos del comando principal {@code /ua}.
 * Requiere nivel de operador 4 para todos los subcomandos.
 */
public class UACommand {

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
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                area.setActive(true);
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
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            return AreaManager.getInstance().getArea(id).map(area -> {
                                area.setActive(false);
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
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!AreaManager.getInstance().exists(id)) {
                                ctx.getSource().sendFailure(Component.literal("Área no encontrada: " + id));
                                return 0;
                            }
                            AreaManager.getInstance().removeArea(id);
                            ctx.getSource().sendSuccess(() -> Component.literal("Área eliminada: " + id), false);
                            return 1;
                        }));
    }

    // /ua who <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerWho() {
        return Commands.literal("who")
                .then(Commands.argument("id", StringArgumentType.word())
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
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerCreate() {
        return Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .then(Commands.literal("circle")
                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("radio", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> {
                                                                    String id = StringArgumentType.getString(ctx, "id");
                                                                    String tipo = StringArgumentType.getString(ctx, "tipo");
                                                                    double x = DoubleArgumentType.getDouble(ctx, "x");
                                                                    double z = DoubleArgumentType.getDouble(ctx, "z");
                                                                    double radio = DoubleArgumentType.getDouble(ctx, "radio");
                                                                    return createArea(ctx.getSource(), id, tipo, AreaShape.CIRCLE, x, z, radio, 0, 0, 0, 0);
                                                                })))))
                                .then(Commands.literal("rect")
                                        .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                        .executes(ctx -> {
                                                                            String id = StringArgumentType.getString(ctx, "id");
                                                                            String tipo = StringArgumentType.getString(ctx, "tipo");
                                                                            double x1 = DoubleArgumentType.getDouble(ctx, "x1");
                                                                            double z1 = DoubleArgumentType.getDouble(ctx, "z1");
                                                                            double x2 = DoubleArgumentType.getDouble(ctx, "x2");
                                                                            double z2 = DoubleArgumentType.getDouble(ctx, "z2");
                                                                            return createArea(ctx.getSource(), id, tipo, AreaShape.RECT, 0, 0, 0, x1, z1, x2, z2);
                                                                        }))))))
                        ));
    }

    /**
     * Lógica compartida de creación de área para los subcomandos circle y rect.
     * Valida el id y el tipo antes de instanciar el área correspondiente.
     */
    private static int createArea(CommandSourceStack source, String id, String tipo,
                                  AreaShape shape, double x, double z, double radio,
                                  double x1, double z1, double x2, double z2) {
        if (AreaManager.getInstance().exists(id)) {
            source.sendFailure(Component.literal("Ya existe un área con id: " + id));
            return 0;
        }

        AreaType type;
        try {
            type = AreaType.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Tipo desconocido: " + tipo + ". Tipos válidos: safe, sign, chest_refill, potion, damage, teleport"));
            return 0;
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
        source.sendSuccess(() -> Component.literal("Área creada: " + id + " [" + type + "] [" + shape + "]"), false);
        return 1;
    }

    // /ua show <id>
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerShow() {
        return Commands.literal("show")
                .then(Commands.argument("id", StringArgumentType.word())
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
}