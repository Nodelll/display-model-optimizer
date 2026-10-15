package dev.nodelll.displaymodeloptimizer.culling;

import com.mojang.math.Transformation;
import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import dev.nodelll.displaymodeloptimizer.compat.EntityCullingCompat;
import dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class DisplayOcclusion {
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final double LOCAL_MARGIN = 0.125;
    private DisplayOcclusion() {}

    public static void invalidateAll() { GENERATION.incrementAndGet(); }
    public static boolean active() {
        return OptimizerSettings.isEnabled() && OptimizerSettings.current().cullOccludedDisplays()
                && EntityCullingCompat.available();
    }

    private static Object model(Display display) {
        if (display instanceof Display.ItemDisplay item && item.itemRenderState() != null)
            return item.itemRenderState().itemStack();
        if (display instanceof Display.BlockDisplay block && block.blockRenderState() != null)
            return block.blockRenderState().blockState();
        return null;
    }
    private static Object context(Display display) {
        if (display instanceof Display.ItemDisplay item && item.itemRenderState() != null)
            return item.itemRenderState().itemTransform();
        return null;
    }
    private static DisplayBoundsCache cache(Display display) {
        return ((DisplayBoundsAccess) display).displayModelOptimizer$getBoundsCache();
    }

    public static void captureItem(Display.ItemDisplay entity, ItemDisplayEntityRenderState state) {
        if (!active()) return;
        DisplayBoundsCache cache = cache(entity);
        Object model = model(entity), context = context(entity);
        long generation = GENERATION.get();
        if (cache.matches(model, context, generation)) return;
        BoundsMath.Box local = null;
        try {
            if (!state.item.isEmpty() && !state.item.isAnimated()) {
                AABB box = state.item.getModelBoundingBox();
                if (!box.hasNaN() && box.getSize() > 0) {
                    local = BoundsMath.itemFacing(new BoundsMath.Box(box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ)).expand(LOCAL_MARGIN);
                }
            }
        } catch (RuntimeException ignored) { /* Unknown model data stays visible. */ }
        cache.capture(model, context, generation, local);
        EntityCullingCompat.showUntilRefreshed(entity);
    }

    public static void captureBlock(Display.BlockDisplay entity, BlockDisplayEntityRenderState state) {
        if (!active()) return;
        DisplayBoundsCache cache = cache(entity);
        Object model = model(entity);
        long generation = GENERATION.get();
        if (cache.matches(model, null, generation)) return;
        BoundsMath.Box local = null;
        try {
            BlockModelStateAccessor access = (BlockModelStateAccessor) state.blockModel;
            BoundsMath.Collector all = new BoundsMath.Collector();
            if (access.displayModelOptimizer$getModelParts() != null) {
                BoundsMath.Collector parts = new BoundsMath.Collector();
                for (var part : access.displayModelOptimizer$getModelParts()) {
                    collectQuads(parts, part.getQuads(null));
                    for (Direction direction : Direction.values()) collectQuads(parts, part.getQuads(direction));
                }
                BoundsMath.Box box = parts.finish();
                if (box != null) {
                    if (access.displayModelOptimizer$getTransformation() != null)
                        box = BoundsMath.transform(box, access.displayModelOptimizer$getTransformation());
                    all.include(box);
                }
            }
            if (access.displayModelOptimizer$getSpecialRenderer() != null) {
                BoundsMath.Collector special = new BoundsMath.Collector();
                access.displayModelOptimizer$getSpecialRenderer().getExtents(special::add);
                BoundsMath.Box box = special.finish();
                if (access.displayModelOptimizer$getSpecialTransformation() != null)
                    box = BoundsMath.transform(box, access.displayModelOptimizer$getSpecialTransformation());
                all.include(box);
            }
            local = all.finish();
            if (local != null) local = local.expand(LOCAL_MARGIN);
        } catch (RuntimeException ignored) { /* Unknown model data stays visible. */ }
        cache.capture(model, null, generation, local);
        EntityCullingCompat.showUntilRefreshed(entity);
    }

    private static void collectQuads(BoundsMath.Collector target,
                                    java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> quads) {
        for (var quad : quads) for (int vertex = 0; vertex < 4; vertex++) target.add(quad.position(vertex));
    }

    public static AABB bounds(Display entity) {
        if (!active() || !(entity instanceof Display.ItemDisplay || entity instanceof Display.BlockDisplay)) return null;
        DisplayBoundsCache cache = cache(entity);
        var geometry = cache.geometry();
        if (geometry == null || geometry.generation() != GENERATION.get() || geometry.box() == null) return null;
        // Entity Culling's normal capture runs on the game thread. Its unsafe mode gets only our published snapshot.
        if (!Minecraft.getInstance().isSameThread()) {
            var pose = cache.pose();
            return pose == null ? null : toAabb(pose.box());
        }
        if (!cache.matches(model(entity), context(entity), GENERATION.get())) return null;
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        // Position paths may have intermediate waypoints; moving anchors stay visible.
        if (x != entity.xOld || y != entity.yOld || z != entity.zOld) {
            EntityCullingCompat.showUntilRefreshed(entity);
            return null;
        }
        var state = entity.renderState();
        if (state == null) return null;
        float yaw0 = entity.getYRot(0), yaw1 = entity.getYRot(1);
        float pitch0 = entity.getXRot(0), pitch1 = entity.getXRot(1);
        var previous = cache.pose();
        if (previous != null && previous.state() == state && previous.x() == x && previous.y() == y && previous.z() == z
                && previous.yaw0() == yaw0 && previous.yaw1() == yaw1
                && previous.pitch0() == pitch0 && previous.pitch1() == pitch1) return toAabb(previous.box());
        BoundsMath.Box box;
        try {
            Transformation first = state.transformation().get(0);
            Transformation last = state.transformation().get(1);
            if (state.billboardConstraints() == Display.BillboardConstraints.FIXED
                    && first.equals(last) && yaw0 == yaw1 && pitch0 == pitch1) {
                Matrix4f matrix = new Matrix4f().rotationYXZ((float) Math.toRadians(-yaw1),
                        (float) Math.toRadians(pitch1), 0).mul(first.getMatrix());
                box = BoundsMath.transform(geometry.box(), matrix);
            } else {
                // Rotation and scale interpolation can extend beyond the endpoint AABBs.
                box = BoundsMath.rotationalSweep(geometry.box(), first.translation(), last.translation(), first.scale(), last.scale());
            }
            if (box == null || !box.valid()) return null;
            box = box.move(x, y, z).expand(0.03);
        } catch (RuntimeException ignored) { return null; }
        cache.pose(new DisplayBoundsCache.Pose(state, x, y, z, yaw0, yaw1, pitch0, pitch1, box));
        EntityCullingCompat.showUntilRefreshed(entity);
        return toAabb(box);
    }

    private static AABB toAabb(BoundsMath.Box box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }
}
