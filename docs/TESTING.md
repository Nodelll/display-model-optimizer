# Testing notes

The current implementation targets Minecraft 26.3, Fabric Loader 0.19.5 or later, and Java 25.

## Automated build checks

Run `python3 build.py` with JDK 25. The build executes:

| Check | Assertions in 0.6.0 |
| --- | ---: |
| Draw lookup equivalence and fallback | 180346 |
| Model cache ownership, lifetime and distance | 28 |
| Settings, validation and configuration transfer | 54 |
| Model bounds and actual Entity Culling scene checks | 9614 |
| GitHub updates: version order, Minecraft compatibility, channels and malformed feeds | 33 |

GitHub Actions runs this build and publishes the resulting JAR as a workflow artifact.

## Additional offline verification

`validation/MixinSmokeTest.java` was run with the official Minecraft 26.3 client, Sponge Mixin 0.8.7 and ASM 9.11 under Java 25. It applies nine project mixin components to eight Minecraft classes, checks method stacks and original operations, and instantiates a transformed model state to exercise the generated accessors and independent model copies.

The same harness also applies the original EntityRendererMixin and WorldRendererMixin from Entity Culling 1.11.3. Rendering mixins were last verified in 0.5.1 and are unchanged in 0.6.0. `validation/ModMenuSmokeTest.java` was rerun for 0.6.0: it resolves the official Mod Menu 21.0.0 API, creates the configuration factory and update checker, and confirms that the integration does not register factories/checkers or modpack badges for other mods. These standalone harnesses are separate from the default Python build and require their supporting launcher/ASM classpath.

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
