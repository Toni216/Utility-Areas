package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class SignArea extends Area {

    private String messageEnter;
    private String messageExit;
    private int cooldownSeconds;

    public SignArea(String id, AreaShape shape) {
        super(id, AreaType.SIGN, shape);
        this.messageEnter = "Has entrado en " + id;
        this.messageExit = "Has salido de " + id;
        this.cooldownSeconds = 0;
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (!messageEnter.isEmpty()) {
            player.sendSystemMessage(Component.literal(messageEnter));
        }
    }

    @Override
    public void onPlayerExit(Player player) {
        if (!messageExit.isEmpty()) {
            player.sendSystemMessage(Component.literal(messageExit));
        }
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
}