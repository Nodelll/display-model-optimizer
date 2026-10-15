package dev.nodelll.displaymodeloptimizer.culling;

/** Owned by the display entity, so unloading it also releases its cached geometry. */
public final class DisplayBoundsCache {
    public record Geometry(Object model, Object context, long generation, BoundsMath.Box box) {}
    public record Pose(Object state, double x, double y, double z,
                       float yaw0, float yaw1, float pitch0, float pitch1, BoundsMath.Box box) {}
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
