package dev.nodelll.displaymodeloptimizer;

import java.util.HashMap;
import java.util.Map;

/** A bounded cache whose entries cannot escape one extraction pass or one thread. */
public final class FrameModelCache<V> {
    private final Map<Key, V> entries = new HashMap<>();
    private int limit;
    private boolean active;

    public FrameModelCache(int limit) {
        this.limit = Math.max(0, limit);
    }

    public void begin() {
        entries.clear();
        active = true;
    }

    public void begin(int nextLimit) {
        limit = Math.max(0, nextLimit);
        begin();
    }

    public void finish() {
        active = false;
        entries.clear();
    }

    public boolean isActive() {
        return active;
    }

    public boolean canStore() {
        return active && entries.size() < limit;
    }

    public V get(Object model, Object state, Object context, long seed) {
        return active ? entries.get(new Key(model, state, context, seed)) : null;
    }

    public void put(Object model, Object state, Object context, long seed, V value) {
        if (canStore() && value != null) {
            entries.put(new Key(model, state, context, seed), value);
        }
    }

    int size() {
        return entries.size();
    }

    private record Key(Object model, Object state, Object context, long seed) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && model == key.model && state == key.state
                    && context == key.context && seed == key.seed;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(model);
            result = 31 * result + System.identityHashCode(state);
            result = 31 * result + System.identityHashCode(context);
            return 31 * result + Long.hashCode(seed);
        }
    }
}
