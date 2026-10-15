# Persistent geometry cache for block displays

Experimental implementation in **0.8.0-alpha.1**, Minecraft **26.3**.

## What it saves

The earlier cache prepares supported block model state once per extraction frame. Fabric Renderer API adds mesh state that the earlier snapshot intentionally does not copy. In that configuration, a visible block display still converts its baked quads through the renderer each frame.

The new cache stores the renderer's immutable, converted geometry **across frames**. Repeated displays and later frames replay that geometry into each display's own output mesh. This targets visible `block_display` entities, including displays directly in front of the camera and behind glass.

It reduces CPU model conversion work. Each display still has its own render state and geometry submission. This implementation does not combine GPU draw calls or introduce GPU instancing. A GPU-bound shader scene may show little FPS change.

## Correctness and limits

- Uses Fabric's `FabricBlockStateModel.createGeometryKey` contract. A null key follows normal model emission; changing keys selects different geometry. Key creation uses a separately seeded random source so it cannot change weighted model selection.
- Cache entries additionally distinguish the model and block state. Camera pose, interpolation, entity light, tint calculation and submission remain on the existing paths.
- Uses the active renderer's `Mesh.outputTo` implementation to preserve quad attributes and apply the destination emitter's current transforms. No renderer-owned mesh arrays are accessed by production code.
- The hook is limited to FRAPI's `BlockStateModelWrapper.update` in `DisplayRenderer.BLOCK_DISPLAY_CONTEXT`, with its empty level, zero position and always-false face-culling predicate. Falling blocks and other block contexts keep normal emission.
- Resource reload, world changes, disconnects, settings saves, renderer replacement and the cutout-leaves option invalidate cached geometry. Generation changes clear entries on the next geometry-cache use.
- LRU capacity defaults to **1024 entries**, controlled by `modelCacheLimit`. A separate hard budget limits stored meshes to **65536 quads**. Sodium 0.9.2 uses 144 bytes of packed data per quad, so this is approximately **9 MiB of quad arrays**, plus entries and renderer overhead.
- Models above **4096 quads** receive bounded skip markers and use ordinary emission after their first encounter. Zero capacity disables storage. Markers count towards the entry limit.

The new optimization needs **Fabric Renderer API 17.0.16 or newer**, supplied by the tested Fabric API **0.163.0+26.3** installation. It works through the API; Sodium is optional. Offline mesh verification uses Sodium **0.9.2**. Without a supported renderer API, the optional mixin is skipped and existing DMO features remain available.

## Settings

**Mod Menu → Display Model Optimizer → Advanced → Persistent block geometry cache**

```properties
cacheBlockDisplayGeometry=true
modelCacheLimit=1024
```

This switch is independent of the per-frame `cacheBlockDisplayModels` option. Older configurations gain the new switch without losing existing values. The default display distance remains 18 blocks.

## Verification

```sh
python3 build.py
python3 validation/run_geometry_checks.py --benchmark
```

The first command runs the normal automated suites. The second applies the actual Fabric wrapper overwrite together with the DMO redirect, verifies method stacks and unchanged tint/setup operations, checks DMO transformations without renderer API binaries and alongside Entity Culling, then runs parity checks on actual Sodium meshes.

The mesh checks compare complete packed quad data for cache misses and hits, vanilla `BakedQuad` conversion, caller transforms, tint indices, UVs, normals, emission, animation flags, render layers, key changes, seed selection, invalidation, unknown models, zero capacity and oversized models. Test-only texture metadata fixtures avoid image allocation and do not start OpenGL.

CPU preparation timings use the actual Fabric `VanillaBlockModelPartEncoder` with Minecraft baked quads and Sodium's packed mesh implementation. They compare ordinary conversion with warmed cache replay, excluding lighting, tint evaluation, extraction and GPU rendering. Each size has four warm-up pairs and nine measured pairs of 12000 calls, alternating measurement order; the reported statistic is the median. These are **CPU microbenchmarks, not in-game FPS**. Recorded results and their scope are in [VALIDATION.json](../VALIDATION.json).

## In-game comparison

1. Install the test JAR in place of 0.7.1 and keep the same renderer, resource pack, resolution and other DMO settings.
2. Use a scene with many visible `block_display` entities; keep the camera fixed. Set the extra distance limit to 0 so the same entities remain visible in both modes.
3. Compare only `cacheBlockDisplayGeometry=false` and `true`, leaving the master switch on. Warm up each mode and use an **off → on → on → off** sequence with equal measurement times.
4. Check opaque, translucent, tinted and animated block textures, transformed displays, changing block states, F3+T, world switches and settings changes. Repeat with the intended shader pack.

The existing DMO Benchmark `quick` plan still measures the combined OFF/FULL difference. Its eight-argument settings constructor enables the new geometry-cache default, so that plan **does not isolate this feature**. Use the manual switch comparison above for an isolated test. No in-game FPS result is claimed for this experimental build yet.

## Sources

- [Fabric model geometry-key contract](https://github.com/FabricMC/fabric-api/blob/26.3/fabric-renderer-api-v1/src/client/java/net/fabricmc/fabric/api/client/renderer/v1/model/FabricBlockStateModel.java)
- [Fabric block-model wrapper](https://github.com/FabricMC/fabric-api/blob/26.3/fabric-renderer-api-v1/src/client/java/net/fabricmc/fabric/mixin/client/renderer/block/render/BlockStateModelWrapperMixin.java)
- [Fabric mesh API](https://github.com/FabricMC/fabric-api/tree/26.3/fabric-renderer-api-v1/src/client/java/net/fabricmc/fabric/api/client/renderer/v1/mesh)
- [Sodium](https://github.com/CaffeineMC/sodium), exact tested binary pinned in `validation/geometry-dependencies.json`.
