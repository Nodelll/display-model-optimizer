package dev.nodelll.displaymodeloptimizer;

import java.util.LinkedHashMap;

/** Render-thread-owned, weighted LRU. Both keys and geometry are released on invalidation. */
public final class GeometryCache<K, V> {
    private record Entry<V>(V value, int weight) {}
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private final int maximumWeight;
    private int limit, weight;

    public GeometryCache(int maximumWeight) {
        if (maximumWeight < 1) throw new IllegalArgumentException("Invalid geometry budget");
        this.maximumWeight = maximumWeight;
    }

    public void configure(int limit) {
        if (limit < 0) throw new IllegalArgumentException("Negative geometry capacity");
        this.limit = limit;
        trim();
    }

    public V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value();
    }

    public void put(K key, V value, int cost) {
        if (key == null || value == null) throw new NullPointerException();
        if (cost < 0) throw new IllegalArgumentException("Negative geometry cost");
        if (limit == 0 || cost > maximumWeight) return;
        Entry<V> old = entries.put(key, new Entry<>(value, cost));
        if (old != null) weight -= old.weight();
        weight += cost;
        trim();
    }

    private void trim() {
        while (entries.size() > limit || weight > maximumWeight) {
            Entry<V> removed = entries.pollFirstEntry().getValue();
            weight -= removed.weight();
        }
    }

    public void clear() { entries.clear(); weight = 0; }
    public int size() { return entries.size(); }
    public int weight() { return weight; }
}
