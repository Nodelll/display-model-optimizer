package dev.nodelll.displaymodeloptimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Checks public lookup semantics against List.indexOf, including hash collisions. */
public final class DrawLookupIndexTest {
    private static long assertions;

    public static void main(String[] args) {
        firstMatchAndNull();
        incrementalRandomized();
        hashCollisions();
        listReset();
        verificationFallback();
        System.out.println("PASS: " + assertions + " lookup assertions; duplicate, null, append, collision, reset and fallback cases.");
    }

    private static void firstMatchAndNull() {
        AtomicInteger mismatches = new AtomicInteger();
        DrawLookupIndex index = new DrawLookupIndex(0, 1, mismatches::incrementAndGet);
        List<Object> values = new ArrayList<>();
        check(index, values, null);
        values.add("same");
        values.add(new String("same"));
        values.add(null);
        values.add(null);
        check(index, values, new String("same"));
        check(index, values, null);
        check(index, values, "absent");
        require(mismatches.get() == 0, "Unexpected verifier mismatch");
    }

    private static void incrementalRandomized() {
        Random random = new Random(20261014);
        for (int group = 0; group < 30; group++) {
            AtomicInteger mismatches = new AtomicInteger();
            DrawLookupIndex index = new DrawLookupIndex(32, 256, mismatches::incrementAndGet);
            List<Object> values = new ArrayList<>();
            for (int append = 0; append < 1500; append++) {
                int id = random.nextInt(700);
                values.add(id == 0 ? null : state(id));
                for (int probe = 0; probe < 4; probe++) {
                    int wanted = random.nextInt(900);
                    check(index, values, wanted == 0 ? null : state(wanted));
                }
            }
            require(mismatches.get() == 0, "Append-only lookup differed from original");
        }
    }

    private static PreparedState state(int id) {
        return new PreparedState("entity", 1, 2, List.of(new Texture("Sampler0", id), new Texture("Sampler1", 4)));
    }

    private static void hashCollisions() {
        AtomicInteger mismatches = new AtomicInteger();
        DrawLookupIndex index = new DrawLookupIndex(8, 16, mismatches::incrementAndGet);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            values.add(new CollisionKey(i));
            values.add(new CollisionKey(i));
        }
        for (int i = 0; i < 300; i++) {
            check(index, values, new CollisionKey(i));
        }
        require(mismatches.get() == 0, "Hash collisions changed first-match behavior");
    }

    private static void listReset() {
        AtomicInteger mismatches = new AtomicInteger();
        DrawLookupIndex index = new DrawLookupIndex(2, 1, mismatches::incrementAndGet);
        List<Object> values = new ArrayList<>(List.of("a", "b", "c", "d"));
        check(index, values, "d");
        values.clear();
        values.addAll(List.of("x", "y", "z", "w"));
        check(index, values, "w");
        check(index, values, "a");
        check(index, new ArrayList<>(List.of("replacement", "other")), "other");
        require(mismatches.get() == 0, "Detected list reset did not rebuild index");
    }

    private static void verificationFallback() {
        AtomicInteger mismatches = new AtomicInteger();
        DrawLookupIndex index = new DrawLookupIndex(0, 1, mismatches::incrementAndGet);
        MutableKey key = new MutableKey(5);
        List<Object> values = new ArrayList<>(List.of(key, "other"));
        check(index, values, key);
        key.id = 19;
        check(index, values, key);
        require(mismatches.get() == 1, "Mutable-key mismatch did not trigger fallback");
        check(index, values, key);
        require(mismatches.get() == 1, "Disabled index reported repeatedly");
    }

    private static void check(DrawLookupIndex index, List<?> values, Object query) {
        int expected = values.indexOf(query);
        int actual = index.findFirst(values, query);
        require(actual == expected, "Expected first index " + expected + ", got " + actual);
    }

    private static void require(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private record Texture(String name, int view) {}
    private record PreparedState(String name, int pipeline, int transforms, List<Texture> textures) {}
    private record CollisionKey(int id) {
        @Override public int hashCode() { return 7; }
    }
    private static final class MutableKey {
        private int id;
        private MutableKey(int id) { this.id = id; }
        @Override public int hashCode() { return id; }
        @Override public boolean equals(Object other) {
            return other instanceof MutableKey key && key.id == id;
        }
    }
}
