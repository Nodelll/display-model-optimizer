package dev.nodelll.displaymodeloptimizer;

public final class DisplayDistancePolicy {
    private DisplayDistancePolicy() {}

    public static boolean isOutside(int limit, double squaredDistance) {
        // Bad distances retain vanilla handling; zero explicitly disables the override.
        return limit > 0 && Double.isFinite(squaredDistance) && squaredDistance >= 0
                && squaredDistance > (double) limit * limit;
    }
}
