package dev.nodelll.displaymodeloptimizer.culling;

import net.minecraft.world.phys.AABB;

/** Owned by the display entity, so unloading it also releases its cached geometry. */
public final class DisplayBoundsCache {
    public record Geometry(Object model, Object context, long generation, BoundsMath.Box box) {}
    public record Pose(Object state, double x, double y, double z,
                       float yaw0, float yaw1, float pitch0, float pitch1, BoundsMath.Box box, AABB aabb) {
        public Pose(Object state, double x, double y, double z,
                    float yaw0, float yaw1, float pitch0, float pitch1, BoundsMath.Box box) {
            this(state, x, y, z, yaw0, yaw1, pitch0, pitch1, box,
                    new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }
    }
    private volatile Geometry geometry;
    private volatile Pose pose;

    public boolean matches(Object model, Object context, long generation) {
        Geometry value = geometry;
        return value != null && value.model == model && value.context == context && value.generation == generation;
    }
    public void capture(Object model, Object context, long generation, BoundsMath.Box box) {
        geometry = new Geometry(model, context, generation, box);
        pose = null;
    }
    public Geometry geometry() { return geometry; }
    public Pose pose() { return pose; }
    public void pose(Pose value) { pose = value; }
}
