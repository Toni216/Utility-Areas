package com.cipollomods.utilityareas.data;

/**
 * Una entrada dentro de un {@link LootGroup}: un item con su peso
 * (probabilidad relativa frente a las demás entradas del grupo) y el rango
 * de cantidad que se le asigna si sale elegido.
 */
class LootEntry {
    String item;
    int weight = 1;
    int minAmount = 1;
    int maxAmount = 1;
}