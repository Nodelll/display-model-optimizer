# Testing notes

The current implementation targets Minecraft 26.3, Fabric Loader 0.19.5 or later, and Java 25.

## Automated build checks

Run `python3 build.py` with JDK 25. The build executes:

| Check | Assertions in 0.8.0-alpha.1 |
| --- | ---: |
| Draw lookup equivalence and fallback | 180346 |
| Model cache ownership, lifetime and distance | 28 |
| Settings, validation and configuration transfer | 76 |
| Model bounds and actual Entity Culling scene checks | 9614 |
| GitHub updates: version order, Minecraft compatibility, channels and malformed feeds | 33 |
| Actual Minecraft frustum with cached model boxes, camera turns and custom-model extents | 236 |
| Actual Minecraft screen background order, double-blur guard and 128 frame resets | 135 |
| Persistent geometry cache: weighted LRU, capacity, randomized parity and lifecycle | 7365 |

GitHub Actions runs this build and publishes the resulting JAR as a workflow artifact. This alpha branch does not publish a release automatically.

CI also runs `python3 validation/run_geometry_checks.py`. It downloads checksum-pinned validation dependencies, verifies existing transformations without Fabric Renderer API, repeats the checks alongside the real Entity Culling mixins, then applies the actual FRAPI wrapper overwrite and DMO geometry redirect together. Actual Sodium 0.9.2 meshes are used for 129 packed-output, metadata, caller-transform, random-seed, model-identity, invalidation and bypass assertions. No graphical client is started.

Add `--benchmark` to that command to time ordinary Minecraft baked-quad conversion against warmed geometry replay. The timings cover only CPU geometry preparation; they do not measure FPS. See [the algorithm and in-game comparison](BLOCK_GEOMETRY_CACHE.md).

`GuiBackgroundTest` reads the actual Minecraft 26.3 screen bytecode, checks that the vanilla wrapper extracts the background before the content, and exercises the real GUI render state's once-per-frame blur guard. Its optional first argument is a previous mod JAR; passing 0.7.0 reproduces the background-order regression.

## Additional offline verification

`validation/MixinSmokeTest.java` was run with the official Minecraft 26.3 client, Sponge Mixin 0.8.7 and ASM 9.11 under Java 25. It applies ten project mixin components to nine Minecraft classes, checks method stacks and original operations, and instantiates a transformed model state to exercise the generated accessors and independent model copies.

The same harness also applies the original EntityRendererMixin and WorldRendererMixin from Entity Culling 1.11.3. For 0.7.0 it was run both without Entity Culling and alongside the original Entity Culling mixins. It verifies that the optional bridge hooks apply only when provided, vanilla frustum checks remain present, and frustum eligibility uses its own switch. The combined run covers one additional Minecraft target (LevelExtractor). `validation/ModMenuSmokeTest.java` was rerun for 0.7.0: it resolves the official Mod Menu 21.0.0 API, creates the configuration factory and update checker, and confirms that the integration does not register factories/checkers or modpack badges for other mods. These standalone harnesses are separate from the default Python build and require their supporting launcher/ASM classpath.

`DisplayFrustumTest` also ran with Entity Culling, Mod Menu and Fabric Loader JARs excluded from its runtime classpath. It uses the official Minecraft Frustum and the production cached AABB conversion; it does not launch a Minecraft client.

The bounds checks use the actual OcclusionCulling backend for opaque-wall, transparent-cell, window, wall-edge and opposite-side cases. They are fixtures, rather than a launched Minecraft world.

## In-game validation

A complete game launch and a controlled FPS comparison are still needed. Useful cases include:

- Running with and without Entity Culling and Mod Menu.
- Dense item and block displays, repeated block models and custom resource-pack models.
- Opaque walls, glass, invisible barriers, moving displays and animated item models.
- Changing render distance and settings, reconnecting, and reloading resources.
- Visiting all Mod Menu settings tabs in English and Russian, changing GUI scale, saving/cancelling/resetting values and opening project links.
- Checking Mod Menu notifications with update checks enabled/disabled and with stable/beta/alpha preferences.
- Testing both the normal renderer and the intended shader/mod combination.

No fixed FPS increase or universal shader-pack compatibility is claimed.
