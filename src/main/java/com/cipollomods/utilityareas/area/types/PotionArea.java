package com.cipollomods.utilityareas.area.types;

import com.cipollomods.utilityareas.area.Area;
import com.cipollomods.utilityareas.area.AreaShape;
import com.cipollomods.utilityareas.area.AreaType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class PotionArea extends Area {

    private final List<MobEffectInstance> effects = new ArrayList<>();
    private boolean removeOnExit;

    public PotionArea(String id, AreaShape shape) {
        super(id, AreaType.POTION, shape);
        this.removeOnExit = true;
    }

    @Override
    public void onPlayerEnter(Player player) {
        // Los efectos se aplican en onTick continuamente
    }

    @Override
    public void onPlayerExit(Player player) {
        if (removeOnExit) {
            for (MobEffectInstance effect : effects) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    @Override
    public void onTick(ServerLevel level) {
        // La aplicación de efectos a jugadores se gestiona en el EventHandler
        // para tener acceso a la lista de jugadores dentro del área
    }

    public void applyEffects(Player player) {
        for (MobEffectInstance effect : effects) {
            // Creamos una nueva instancia para evitar compartir estado
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    true,  // ambient - partículas reducidas
                    false  // sin partículas visibles
            ));
        }
    }

    public void addEffect(MobEffect effect, int durationTicks, int amplifier) {
        effects.add(new MobEffectInstance(effect, durationTicks, amplifier));
    }

    public void removeEffect(MobEffect effect) {
        effects.removeIf(instance -> instance.getEffect() == effect);
    }

    public void clearEffects() {
        effects.clear();
    }

    public List<MobEffectInstance> getEffects() { return effects; }

    public boolean isRemoveOnExit() { return removeOnExit; }
    public void setRemoveOnExit(boolean removeOnExit) { this.removeOnExit = removeOnExit; }

    @Override
    protected String describeSpecific() {
        StringBuilder sb = new StringBuilder();
        sb.append("  removeOnExit: ").append(removeOnExit).append("\n");
        if (effects.isEmpty()) {
            sb.append("  Efectos: (ninguno)\n");
        } else {
            sb.append("  Efectos:\n");
            for (MobEffectInstance effect : effects) {
                var rl = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                sb.append("    - ").append(rl).append(" (nivel ").append(effect.getAmplifier() + 1)
                        .append(", ").append(effect.getDuration() / 20).append("s)\n");
            }
        }
        return sb.toString();
    }
}