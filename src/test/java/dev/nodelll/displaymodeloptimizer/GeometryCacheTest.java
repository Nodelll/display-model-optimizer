package dev.nodelll.displaymodeloptimizer;

import java.util.LinkedHashMap;
import java.util.Random;

public final class GeometryCacheTest {
    private static int assertions;

    public static void main(String[] args) {
        GeometryCache<String, String> cache = new GeometryCache<>(10);
        cache.configure(3);
        cache.put("a", "first", 4); cache.put("b", "second", 4);
        check(cache.get("a").equals("first"), "hit");
        cache.put("c", "third", 4);
        check(cache.get("b") == null && cache.size() == 2 && cache.weight() == 8, "weighted LRU eviction");
        cache.put("a", "replacement", 1);
        check(cache.weight() == 5 && cache.get("a").equals("replacement"), "replacement adjusts cost");
        cache.put("huge", "ignored", 11);
        check(cache.get("huge") == null && cache.weight() == 5, "oversized rejected");
        cache.configure(1);
        check(cache.size() == 1 && cache.get("a").equals("replacement"), "live limit reduction");
        cache.configure(0); cache.put("zero", "ignored", 0);
        check(cache.size() == 0 && cache.weight() == 0, "zero releases all entries");
        cache.configure(3);
        for (int i = 0; i < 20; i++) cache.put("marker-" + i, "skip", 0);
        check(cache.size() == 3 && cache.weight() == 0, "zero-weight markers still bounded");
        cache.clear();
        check(cache.size() == 0 && cache.weight() == 0, "invalidation releases entries");

        // Compare eviction and values against an independent map implementation under churn.
        var expected = new LinkedHashMap<Integer, Integer>(16, .75f, true);
        var actual = new GeometryCache<Integer, Integer>(24);
        actual.configure(7);
        var random = new Random(803);
        int limit = 7;
        for (int i = 0; i < 3000; i++) {
            int key = random.nextInt(16);
            if (i % 19 == 0) {
                limit = random.nextInt(10); actual.configure(limit);
            } else if (i % 37 == 0) {
                actual.clear(); expected.clear();
            } else if (random.nextBoolean()) {
                int cost = random.nextInt(12);
                actual.put(key, cost, cost);
                if (limit > 0) expected.put(key, cost);
            } else {
                check(java.util.Objects.equals(actual.get(key), expected.get(key)), "random lookup parity");
            }
            while (expected.size() > limit || expected.values().stream().mapToInt(Integer::intValue).sum() > 24)
                expected.remove(expected.keySet().iterator().next());
            check(actual.size() == expected.size(), "random capacity parity");
            check(actual.weight() == expected.values().stream().mapToInt(Integer::intValue).sum(), "random weight parity");
        }
        Object first = new Object(), second = new Object();
        long generation = GeometryCacheLifecycle.generation();
        GeometryCacheLifecycle.beginFrame(first);
        check(GeometryCacheLifecycle.generation() == generation + 1, "new world invalidates");
        GeometryCacheLifecycle.beginFrame(first);
        check(GeometryCacheLifecycle.generation() == generation + 1, "same world retains");
        GeometryCacheLifecycle.beginFrame(second);
        GeometryCacheLifecycle.beginFrame(null);
        check(GeometryCacheLifecycle.generation() == generation + 3, "world switch and disconnect invalidate");
        GeometryCacheLifecycle.invalidate();
        check(GeometryCacheLifecycle.generation() == generation + 4, "explicit resource/settings invalidation");
        System.out.println("PASS: " + assertions + " persistent geometry budget, LRU, random parity and invalidation assertions.");
    }

    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }
}
