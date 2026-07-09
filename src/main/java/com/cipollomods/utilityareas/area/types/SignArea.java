package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignArea extends Area {

    private String messageEnter;
    private String messageExit;
    private int cooldownSeconds;

    // Última vez (en ticks) que se envió un mensaje a cada jugador
    private final Map<UUID, Long> lastMessageTick = new HashMap<>();

    public SignArea(String id, AreaShape shape) {
        super(id, AreaType.SIGN, shape);
        this.messageEnter = "Has entrado en " + id;
        this.messageExit = "Has salido de " + id;
        this.cooldownSeconds = 0;
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (!messageEnter.isEmpty() && canSendMessage(player)) {
            player.sendSystemMessage(Component.literal(messageEnter));
        }
    }

    @Override
    public void onPlayerExit(Player player) {
        if (!messageExit.isEmpty() && canSendMessage(player)) {
            player.sendSystemMessage(Component.literal(messageExit));
        }
    }

    /**
     * Comprueba si ha pasado el cooldown desde el último mensaje enviado a
     * este jugador por esta área. Si es así, actualiza la marca de tiempo
     * y devuelve true; en caso contrario devuelve false sin modificar nada.
     */
    private boolean canSendMessage(Player player) {
        if (cooldownSeconds <= 0) return true;

        long now = player.level().getGameTime();
        long cooldownTicks = (long) cooldownSeconds * 20;
        Long last = lastMessageTick.get(player.getUUID());

        if (last != null && now - last < cooldownTicks) {
            return false;
        }

        lastMessageTick.put(player.getUUID(), now);
        return true;
    }

    @Override
    public void onTick(ServerLevel level) {
        // SignArea no necesita lógica de tick
        // El tracking de jugadores se gestiona en el EventHandler
    }

    // Getters y setters
    public String getMessageEnter() { return messageEnter; }
    public void setMessageEnter(String messageEnter) { this.messageEnter = messageEnter; }

    public String getMessageExit() { return messageExit; }
    public void setMessageExit(String messageExit) { this.messageExit = messageExit; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    @Override
    protected String describeSpecific() {
        return "  Mensaje entrada: " + messageEnter + "\n"
                + "  Mensaje salida: " + messageExit + "\n"
                + "  Cooldown: " + cooldownSeconds + "s\n";
    }
}