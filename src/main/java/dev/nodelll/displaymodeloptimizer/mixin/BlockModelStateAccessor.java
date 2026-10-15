package dev.nodelll.displaymodeloptimizer.mixin;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BlockModelRenderState.class, remap = false)
public interface BlockModelStateAccessor {
    @Accessor("modelParts") List<BlockStateModelPart> displayModelOptimizer$getModelParts();
    @Accessor("modelParts") void displayModelOptimizer$setModelParts(List<BlockStateModelPart> value);
    @Accessor("transformation") Matrix4fc displayModelOptimizer$getTransformation();
    @Accessor("transformation") void displayModelOptimizer$setTransformation(Matrix4fc value);
    @Accessor("renderType") RenderType displayModelOptimizer$getRenderType();
    @Accessor("renderType") void displayModelOptimizer$setRenderType(RenderType value);
    @Accessor("specialRenderer") SpecialModelRenderer<?> displayModelOptimizer$getSpecialRenderer();
    @Accessor("specialRendererTransformation") Matrix4fc displayModelOptimizer$getSpecialTransformation();
    @Accessor("tintLayers") IntList displayModelOptimizer$getTintLayers();
    @Accessor("tintLayers") void displayModelOptimizer$setTintLayers(IntList value);
}
