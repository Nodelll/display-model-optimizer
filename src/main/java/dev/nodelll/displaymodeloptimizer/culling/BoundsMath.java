package dev.nodelll.displaymodeloptimizer.culling;

import org.joml.Matrix4fc;
import org.joml.Vector3fc;

/** Geometry only: bounds always enclose the whole model, including negative scale. */
public final class BoundsMath {
    private BoundsMath() {}

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public boolean valid() {
            return Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(minZ)
                    && Double.isFinite(maxX) && Double.isFinite(maxY) && Double.isFinite(maxZ)
                    && minX <= maxX && minY <= maxY && minZ <= maxZ;
        }
        public Box expand(double margin) {
            return new Box(minX - margin, minY - margin, minZ - margin,
                    maxX + margin, maxY + margin, maxZ + margin);
        }
        public Box move(double x, double y, double z) {
            return new Box(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
        }
        public double radius() {
            double x = Math.max(Math.abs(minX), Math.abs(maxX));
            double y = Math.max(Math.abs(minY), Math.abs(maxY));
            double z = Math.max(Math.abs(minZ), Math.abs(maxZ));
            return Math.sqrt(x * x + y * y + z * z);
        }
        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    public static final class Collector {
        private double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        private double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        private boolean bad;
        public void add(Vector3fc point) { add(point.x(), point.y(), point.z()); }
        public void add(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) { bad = true; return; }
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
        public Box finish() {
            Box result = new Box(minX, minY, minZ, maxX, maxY, maxZ);
            return !bad && result.valid() ? result : null;
        }
        public void include(Box box) {
            if (box == null) { bad = true; return; }
            add(box.minX, box.minY, box.minZ); add(box.maxX, box.maxY, box.maxZ);
        }
    }

    public static Box transform(Box box, Matrix4fc matrix) {
        if (box == null || !box.valid() || matrix.m03() != 0 || matrix.m13() != 0
                || matrix.m23() != 0 || matrix.m33() != 1) return null;
        Collector result = new Collector();
        for (int corner = 0; corner < 8; corner++) {
            double x = (corner & 1) == 0 ? box.minX : box.maxX;
            double y = (corner & 2) == 0 ? box.minY : box.maxY;
            double z = (corner & 4) == 0 ? box.minZ : box.maxZ;
            result.add(matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30(),
                    matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31(),
                    matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32());
        }
        return result.finish();
    }

    public static Box itemFacing(Box box) {
        // ItemDisplayRenderer applies a 180-degree Y rotation before submitting the item.
        return new Box(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
    }

    public static Box rotationalSweep(Box local, Vector3fc translation0, Vector3fc translation1,
                                      Vector3fc scale0, Vector3fc scale1) {
        double scale = Math.max(maxAbs(scale0), maxAbs(scale1));
        double translation = Math.max(translation0.length(), translation1.length());
        double radius = local.radius() * scale + translation;
        Box result = new Box(-radius, -radius, -radius, radius, radius, radius);
        return result.valid() ? result : null;
    }

    private static double maxAbs(Vector3fc vector) {
        return Math.max(Math.abs(vector.x()), Math.max(Math.abs(vector.y()), Math.abs(vector.z())));
    }
}
