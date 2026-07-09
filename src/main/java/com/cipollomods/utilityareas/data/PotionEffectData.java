package com.cipollomods.utilityareas.data;

/**
 * Representación serializable de un único efecto dentro de una
 * {@link com.cipollomods.utilityareas.area.types.PotionArea}. Guarda el
 * registry name del efecto (no el efecto en sí) para que sea estable entre
 * reinicios y compatible con mods que añadan efectos nuevos.
 */
class PotionEffectData {
    String effectId;
    int durationTicks;
    int amplifier;

    PotionEffectData() {}

    PotionEffectData(String effectId, int durationTicks, int amplifier) {
        this.effectId = effectId;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }
}