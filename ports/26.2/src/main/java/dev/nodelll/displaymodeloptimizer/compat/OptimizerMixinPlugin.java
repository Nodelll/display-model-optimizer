package dev.nodelll.displaymodeloptimizer.compat;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Optional FRAPI integration must not load its API classes on a vanilla Fabric installation. */
public final class OptimizerMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.endsWith(".FabricBlockGeometryMixin")) return true;
        return FabricLoader.getInstance().getModContainer("fabric-renderer-api-v1").map(mod -> {
            try { return mod.getMetadata().getVersion().compareTo(Version.parse("14.1.4")) >= 0; }
            catch (VersionParsingException e) { return false; }
        }).orElse(false);
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
