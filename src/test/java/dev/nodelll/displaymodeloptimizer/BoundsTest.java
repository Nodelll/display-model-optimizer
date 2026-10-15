package dev.nodelll.displaymodeloptimizer;

import com.logisticscraft.occlusionculling.DataProvider;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import dev.nodelll.displaymodeloptimizer.culling.BoundsMath;
import dev.nodelll.displaymodeloptimizer.culling.DisplayBoundsCache;
import java.util.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BoundsTest {
    private static int assertions;
    public static void main(String[] args) {
        BoundsMath.Box local = new BoundsMath.Box(-.5, -.25, -.75, .5, 1.25, .75);
        Random random = new Random(42);
        for (int trial = 0; trial < 120; trial++) {
            Vector3f translation = vector(random, 8);
            Quaternionf rotation = new Quaternionf().rotationXYZ(random.nextFloat() * 6,
                    random.nextFloat() * 6, random.nextFloat() * 6);
            Vector3f scale = vector(random, 4);
            Matrix4f matrix = new Matrix4f().translationRotateScale(translation, rotation, scale);
            var result = BoundsMath.transform(local, matrix).expand(.0001);
            for (int sample = 0; sample < 40; sample++) {
                Vector3f point = point(random, local).mulPosition(matrix);
                check(result.contains(point.x, point.y, point.z), "Affine bounds enclose transformed model sample");
            }
            Vector3f endTranslation = vector(random, 8), endScale = vector(random, 4);
            Quaternionf endRotation = new Quaternionf().rotationXYZ(random.nextFloat() * 6,
                    random.nextFloat() * 6, random.nextFloat() * 6);
            var sweep = BoundsMath.rotationalSweep(local, translation, endTranslation, scale, endScale).expand(.0001);
            for (int sample = 0; sample < 40; sample++) {
                float fraction = random.nextFloat();
                Vector3f t = new Vector3f(translation).lerp(endTranslation, fraction);
                Vector3f s = new Vector3f(scale).lerp(endScale, fraction);
                Quaternionf left = new Quaternionf(rotation).slerp(endRotation, fraction);
                Quaternionf right = new Quaternionf(endRotation).slerp(rotation, fraction);
                Matrix4f intermediate = new Matrix4f().translation(t).rotate(left).scale(s).rotate(right);
                // Billboard/anchor orientation can rotate the whole transformed model.
                intermediate = new Matrix4f().rotate(endRotation).mul(intermediate);
                Vector3f point = point(random, local).mulPosition(intermediate);
                check(sweep.contains(point.x, point.y, point.z), "Swept bounds enclose interpolation and billboard orientation");
            }
        }
        check(BoundsMath.transform(local, new Matrix4f().setPerspective(1, 1, .1f, 100)) == null, "Projective matrix fails open");
        check(BoundsMath.transform(local, new Matrix4f().m00(Float.NaN)) == null, "NaN matrix fails open");
        check(new BoundsMath.Collector().finish() == null, "Missing model vertices stay visible");
        var flat = new BoundsMath.Collector(); flat.add(0, 0, 0); flat.add(1, 1, 0);
        check(flat.finish().expand(.125).valid(), "Flat item models get usable bounds");
        var facing = BoundsMath.itemFacing(local);
        check(facing.minX() == -.5 && facing.minZ() == -.75 && facing.maxY() == 1.25, "Item-display facing preserves model extent");

        var cache = new DisplayBoundsCache(); Object model = new Object(), context = new Object();
        cache.capture(model, context, 1, local);
        check(cache.matches(model, context, 1), "Geometry uses identity and generation");
        check(!cache.matches(new Object(), context, 1) && !cache.matches(model, new Object(), 1), "Changed model or context invalidates geometry");
        check(!cache.matches(model, context, 2), "Resource reload invalidates geometry");
        cache.pose(new DisplayBoundsCache.Pose(model, 0, 0, 0, 0, 0, 0, 0, local));
        cache.capture(model, context, 2, local);
        check(cache.pose() == null, "New geometry discards an old world-space pose");

        var world = new Wall();
        var culler = new OcclusionCullingInstance(64, world);
        Vec3d camera = new Vec3d(.5, .5, -4.5);
        Vec3d min = new Vec3d(.2, .2, 3.2), max = new Vec3d(.8, .8, 3.8);
        check(!culler.isAABBVisible(min, max, camera), "Real Entity Culling backend hides a model behind an opaque wall");
        world.transparent = true; culler.resetCache();
        check(culler.isAABBVisible(min, max, camera), "Non-opaque glass/barrier cells do not hide the model");
        world.transparent = false; world.window = true; culler.resetCache();
        check(culler.isAABBVisible(min, max, camera), "Visible model through a window remains visible");
        world.window = false; culler.resetCache();
        check(culler.isAABBVisible(min, max, new Vec3d(.5, .5, 5.5)), "Approaching from the model side restores visibility");
        culler.resetCache();
        check(culler.isAABBVisible(new Vec3d(6.2, .2, 3.2), new Vec3d(6.8, .8, 3.8), camera), "A model visible around a wall edge remains visible");
        System.out.println("PASS: " + assertions + " model bounds, interpolation, cache invalidation and actual Entity Culling scene assertions.");
    }

    private static Vector3f vector(Random random, float range) {
        return new Vector3f((random.nextFloat() * 2 - 1) * range,
                (random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range);
    }
    private static Vector3f point(Random random, BoundsMath.Box box) {
        return new Vector3f((float) (box.minX() + random.nextDouble() * (box.maxX() - box.minX())),
                (float) (box.minY() + random.nextDouble() * (box.maxY() - box.minY())),
                (float) (box.minZ() + random.nextDouble() * (box.maxZ() - box.minZ())));
    }
    private static void check(boolean valid, String message) {
        assertions++; if (!valid) throw new AssertionError(message);
    }
    private static final class Wall implements DataProvider {
        boolean transparent, window;
        public boolean prepareChunk(int x, int z) { return true; }
        public boolean isOpaqueFullCube(int x, int y, int z) {
            return !transparent && z == 0 && x >= -2 && x <= 2 && y >= -5 && y <= 5
                    && !(window && x == 0 && y == 0);
        }
    }
}
