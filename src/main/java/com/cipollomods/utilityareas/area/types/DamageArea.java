package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Área que aplica daño periódico a los jugadores dentro de su zona.
 * El intervalo y la fuente de daño son configurables.
 * La lógica de tick se gestiona en {@link com.cipollomods.utilityareas.event.AreaEventHandler}.
 */
public class DamageArea extends Area {

    private float damageAmount;
    private int intervalTicks;
    private String damageSourceType;

    public DamageArea(String id, AreaShape shape) {
        super(id, AreaType.DAMAGE, shape);
        this.damageAmount = 1.0f;
        this.intervalTicks = 20; // cada segundo por defecto
        this.damageSourceType = "generic";
    }

    @Override
    public void onPlayerEnter(Player player) {
        // DamageArea no hace nada especial al entrar
    }

    @Override
    public void onPlayerExit(Player player) {
        // DamageArea no hace nada especial al salir
    }

    @Override
    public void onTick(ServerLevel level) {
        // La aplicación de daño a jugadores se gestiona en el EventHandler
    }

    /**
     * Aplica daño al jugador usando la fuente de daño configurada.
     * Fuentes soportadas: generic, fire, magic, void, starve.
     */
    public void applyDamage(Player player, ServerLevel level) {
        DamageSource source = switch (damageSourceType) {
            case "fire" -> level.damageSources().inFire();
            case "magic" -> level.damageSources().magic();
            case "void" -> level.damageSources().fellOutOfWorld();
            case "starve" -> level.damageSources().starve();
            default -> level.damageSources().generic();
        };
        player.hurt(source, damageAmount);
    }

    public float getDamageAmount() { return damageAmount; }
    public void setDamageAmount(float damageAmount) { this.damageAmount = damageAmount; }

    public int getIntervalTicks() { return intervalTicks; }
    public void setIntervalTicks(int intervalTicks) { this.intervalTicks = intervalTicks; }

    public String getDamageSourceType() { return damageSourceType; }
    public void setDamageSourceType(String damageSourceType) { this.damageSourceType = damageSourceType; }
}