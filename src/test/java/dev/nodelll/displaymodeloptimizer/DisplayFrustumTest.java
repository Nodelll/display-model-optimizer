package dev.nodelll.displaymodeloptimizer;

import dev.nodelll.displaymodeloptimizer.culling.BoundsMath;
import dev.nodelll.displaymodeloptimizer.culling.DisplayBoundsCache;
import java.util.Random;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual Minecraft frustum with our model geometry, without a client or Entity Culling instance. */
public final class DisplayFrustumTest {
    private static int assertions;

    public static void main(String[] args) {
        Frustum front = camera(0, 0, 0, 0);
        var unit = new BoundsMath.Box(-.5, -.5, -.5, .5, .5, .5);
        check(front.isVisible(aabb(unit.move(0, 0, -8))), "Model in front stays visible");
        check(!front.isVisible(aabb(unit.move(0, 0, 8))), "Model behind camera is culled without Entity Culling");
        check(!front.isVisible(aabb(unit.move(30, 0, -8))), "Model fully beyond right edge is culled");
        check(!front.isVisible(aabb(unit.move(-30, 0, -8))), "Model fully beyond left edge is culled");
        check(!front.isVisible(aabb(unit.move(0, 30, -8))), "Model fully above view is culled");
        check(!front.isVisible(aabb(unit.move(0, -30, -8))), "Model fully below view is culled");
        var large = new BoundsMath.Box(-20, -1, -1, 20, 1, 1);
        check(!front.isVisible(aabb(unit.move(18, 0, -12))), "Large-model anchor alone is outside view");
        check(front.isVisible(aabb(large.move(18, 0, -12))), "Visible portion of a large custom model keeps it rendered");
        var displaced = BoundsMath.transform(unit, new Matrix4f().translation(0, 0, -24)).move(0, 0, 12);
        check(front.isVisible(aabb(displaced)), "Translated model remains visible even when its entity anchor is behind camera");
        check(camera(0, 0, 0, (float)Math.PI).isVisible(aabb(unit.move(0, 0, 8))), "Turning camera restores visibility");
        check(camera(5000, 90, -3000, 0).isVisible(aabb(unit.move(5000, 90, -3008))), "World-space bounds account for camera position");
        var flat = new BoundsMath.Box(-1, -1, 0, 1, 1, 0).expand(.125).move(0, 0, -8);
        check(front.isVisible(aabb(flat)), "Flat item model has a usable visible box");

        Random random = new Random(73);
        int visibleVertices = 0;
        for (int trial = 0; trial < 240; trial++) {
            double cx = 1000 + random.nextDouble() * 100, cy = 80, cz = -1000;
            Frustum view = camera(cx, cy, cz, random.nextFloat() * 6.28f);
            Matrix4f transform = new Matrix4f().translation(random.nextFloat() * 70 - 35, random.nextFloat() * 30 - 15,
                    random.nextFloat() * 80 - 40).rotateXYZ(random.nextFloat() * 6, random.nextFloat() * 6, random.nextFloat() * 6)
                    .scale(random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8);
            BoundsMath.Box box = BoundsMath.transform(large, transform).move(cx, cy, cz).expand(.001);
            for (int corner = 0; corner < 8; corner++) {
                Vector3f point = new Vector3f((corner & 1) == 0 ? -20 : 20,
                        (corner & 2) == 0 ? -1 : 1, (corner & 4) == 0 ? -1 : 1).mulPosition(transform);
                if (view.pointInFrustum(point.x + cx, point.y + cy, point.z + cz)) {
                    visibleVertices++;
                    check(view.isVisible(aabb(box)), "Visible transformed vertex cannot be lost by computed model bounds");
                }
            }
        }
        check(visibleVertices > 100, "Property scenes include enough visible model vertices");
        System.out.println("PASS: " + assertions + " actual Minecraft frustum, custom-model extent, negative scale and camera-view assertions.");
    }

    private static Frustum camera(double x, double y, double z, float yaw) {
        Frustum result = new Frustum(new Matrix4f().rotationY(yaw),
                new Matrix4f().perspective((float)Math.toRadians(70), 1f, .1f, 256f));
        result.prepare(x, y, z);
        return result;
    }

    private static AABB aabb(BoundsMath.Box box) {
        return new DisplayBoundsCache.Pose(null, 0, 0, 0, 0, 0, 0, 0, box).aabb();
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
