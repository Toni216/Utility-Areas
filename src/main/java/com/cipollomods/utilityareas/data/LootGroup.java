package com.cipollomods.utilityareas.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Un grupo de loot configurable, usado por ChestRefillArea. Cada relleno
 * elige un número de pilas de objetos entre {@code minItems} y
 * {@code maxItems}, cada una sorteada por peso entre las entradas de
 * {@code items}.
 */
class LootGroup {
    int minItems = 1;
    int maxItems = 1;
    List<LootEntry> items = new ArrayList<>();
}