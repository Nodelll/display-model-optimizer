package dev.nodelll.displaymodeloptimizer;

import java.util.List;
import java.util.Objects;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/** First-match index for one append-only draw list, owned by one render group. */
public final class DrawLookupIndex {
    private final int minimumSize;
    private final int verifyEvery;
    private final Runnable mismatchListener;
    private List<?> source;
    private Object2IntOpenHashMap<Object> firstIndex;
    private int indexedSize;
    private int callsUntilVerify;
    private Object firstValue;
    private Object lastIndexedValue;
    private boolean disabled;

    public DrawLookupIndex(int minimumSize, int verifyEvery, Runnable mismatchListener) {
        if (minimumSize < 0 || verifyEvery < 0) {
            throw new IllegalArgumentException("Negative lookup setting");
        }
        this.minimumSize = minimumSize;
        this.verifyEvery = verifyEvery;
        this.callsUntilVerify = verifyEvery;
        this.mismatchListener = Objects.requireNonNull(mismatchListener);
    }

    public int findFirst(List<?> values, Object query) {
        final int size = values.size();
        if (disabled || size < minimumSize) {
            return values.indexOf(query);
        }

        // Vanilla appends to this list; these checks also detect common list resets.
        if (source != values || size < indexedSize
                || (indexedSize > 0 && (values.get(0) != firstValue
                || values.get(indexedSize - 1) != lastIndexedValue))) {
            source = values;
            indexedSize = 0;
            firstIndex = new Object2IntOpenHashMap<>(Math.max(16, Math.min(size, 4096)));
            firstIndex.defaultReturnValue(-1);
        }

        for (; indexedSize < size; indexedSize++) {
            // Non-consolidatable draws can introduce duplicate keys. Keep the first.
            firstIndex.putIfAbsent(values.get(indexedSize), indexedSize);
        }
        if (size > 0) {
            firstValue = values.get(0);
            lastIndexedValue = values.get(size - 1);
        }

        final int result = firstIndex.getInt(query);

        // Never return a draw whose current prepared state differs from the query.
        if (result >= size || (result >= 0 && !Objects.equals(query, values.get(result)))) {
            return failSafe(values, query);
        }

        // Sample the original lookup to detect unexpected changes made by other mods.
        if (verifyEvery > 0 && --callsUntilVerify <= 0) {
            callsUntilVerify = verifyEvery;
            final int original = values.indexOf(query);
            if (original != result) {
                disable();
                return original;
            }
        }
        return result;
    }

    public boolean usesSettings(int minimum, int interval) {
        return minimumSize == minimum && verifyEvery == interval;
    }

    private int failSafe(List<?> values, Object query) {
        disable();
        return values.indexOf(query);
    }

    private void disable() {
        disabled = true;
        source = null;
        firstIndex = null;
        indexedSize = 0;
        mismatchListener.run();
    }
}
