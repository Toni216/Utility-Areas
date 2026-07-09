package com.cipollomods.utilityareas.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Representación plana de un {@link com.cipollomods.utilityareas.area.Area}
 * usada únicamente para (de)serializar con Gson. Gson no maneja bien el
 * polimorfismo de clases abstractas, así que en vez de serializar cada
 * subclase de Area directamente, {@link AreaPersistenceManager} convierte
 * cada área a/desde esta estructura, que contiene todos los campos posibles
 * de todos los tipos (los que no aplican a un tipo concreto quedan a su
 * valor por defecto y se ignoran al reconstruir el área).
 */
class AreaData {

    String id;
    String type;   // nombre de AreaType
    String shape;  // nombre de AreaShape
    boolean active;

    // Geometría circular
    double centerX;
    double centerZ;
    double radius;

    // Geometría rectangular
    double x1, z1, x2, z2;

    // SignArea
    String messageEnter;
    String messageExit;
    int signCooldownSeconds;

    // ChestRefillArea
    int refillIntervalMinutes;
    String chestGroupId;
    List<String> chestPositions = new ArrayList<>();

    // PotionArea
    List<PotionEffectData> effects = new ArrayList<>();
    boolean removeOnExit;

    // DamageArea
    float damageAmount;
    int damageIntervalTicks;
    String damageSourceType;

    // TeleportArea
    String teleportMode;
    double destX, destY, destZ;
    float destYaw, destPitch;
    String destAreaId;
    Double randomCenterX;
    Double randomCenterZ;
    double randomRadius;
    int teleportCooldownSeconds;
}