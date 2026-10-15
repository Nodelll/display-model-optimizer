<!-- <p align="center"> -->
<!--  <img src="src/main/resources/assets/display_model_optimizer/icon.png" width="112" height="112" alt="Display Model Optimizer icon"> -->
<!-- </p> -->

# Display Model Optimizer

**Client-side rendering optimization for Minecraft display models.**

[Русский](README.ru.md) · [Download](https://github.com/Nodelll/display-model-optimizer/releases) · [Report a bug](https://github.com/Nodelll/display-model-optimizer/issues) · [Build status](https://github.com/Nodelll/display-model-optimizer/actions)

Display Model Optimizer reduces repeated rendering work for `item_display` and `block_display` entities. It is designed for decorative builds, custom furniture, showcases and other areas containing many display models, in singleplayer or on servers.

## Features

- **Block model caching:** prepares identical supported block models once per frame.
- **Faster draw-group lookups:** reduces the cost of grouping models for rendering.
- **Configurable display distance:** an additional render-distance limit of **18 blocks by default**.
- **Off-screen culling:** actual model bounds let Minecraft skip item and block displays outside the camera view, independently of Entity Culling.
- **Entity Culling integration:** supplies model bounds so fully occluded displays can be hidden behind opaque blocks.

## Requirements

| Component | Version / role |
| --- | --- |
| Minecraft | **26.3** |
| Fabric Loader | **0.19.5 or later** |
| Java | **25** |
| Entity Culling | Optional; required for occlusion. Tested with **1.11.3** for Minecraft 26.3. |
| Mod Menu | Optional; required for the settings screen. Tested with **21.0.0**. |

Install the mod on the **client**. Model caching, draw lookups and the distance limit work without Entity Culling. Fabric API is not a dependency of this mod.

## Installation and settings

1. Download the JAR from [Releases](https://github.com/Nodelll/display-model-optimizer/releases).
2. Remove the previous version from your instance's `mods` folder, place the new JAR there and restart Minecraft.
3. Open **Mod Menu → Display Model Optimizer**, or edit `config/display-model-optimizer.properties` manually and restart the game.

**The default maximum display range is 18 blocks.** Increase `displayRenderDistance` for more distant models, or set it to **0** to keep the original game/server range. The limit applies to both `item_display` and `block_display`.

Off-screen culling uses Minecraft's normal camera frustum and works independently of Entity Culling. The new `cullOffscreenDisplays` option is enabled by default and is separate from `cullOccludedDisplays`. Actual model extents, transformations and conservative interpolation bounds determine visibility. See [culling details](docs/CULLING.md).

The occlusion option uses the installed Entity Culling mod. Visibility follows its transparency rules; full opaque blocks can hide models, while glass and invisible barriers do not count as opaque walls. Animated item models, moving display entities and models with unknown bounds may remain visible.

## Performance tests

User-submitted measurements from **15 October 2026**, using **DMO 0.7.1** and the separate DMO Benchmark companion's `quick` plan in a singleplayer arena with **600 item/block display entities**.

### Test setup and method

- **Hardware:** AMD Ryzen 5 5600, NVIDIA GeForce RTX 3070 8 GB, 16 GB RAM.
- **Reported client:** Minecraft 26.3, Fabric Loader 0.19.5, Java 25, Sodium 0.9.2, Iris 1.11.7 and Entity Culling 1.11.3.
- **Settings recorded in the no-shader report:** 1920 × 1009 window, 16-chunk render distance, 12-chunk simulation distance, FOV 85 and a 4 GiB Java heap limit.
- **Shader run:** Complementary Unbound r5.9.3, with `enableShaders=true` confirmed in Iris settings.

Each run contains **16 measured phases of 20 seconds**. For each of the four scenes, the order was **OFF → FULL → FULL → OFF**, giving two measurements per mode after warm-up and settling. FPS below is calculated as **total recorded frames ÷ total measured time** across the two repetitions; displayed values are rounded.

**OFF** disables DMO at runtime while the other installed mods remain active. **FULL** enables DMO with the additional display-distance limit set to **0** for these comparisons. The tables therefore show DMO's additional effect in this modpack, without shortening display distance. This is not a comparison against a clean vanilla installation.

### Without shaders

| Scene | DMO OFF, FPS | DMO FULL, FPS | Change |
| --- | ---: | ---: | ---: |
| Displays in front of the camera | 312.3 | 312.3 | ≈0% |
| Camera turned away from displays | 1037.3 | 1228.0 | +18.4% |
| Opaque wall between camera and displays | 309.4 | 567.6 | **+83.5%** |
| Glass between camera and displays | 308.2 | 307.4 | −0.3% |

### With Complementary Unbound r5.9.3

| Scene | DMO OFF, FPS | DMO FULL, FPS | Change |
| --- | ---: | ---: | ---: |
| Displays in front of the camera | 184.5 | 184.2 | −0.2% |
| Camera turned away from displays | 380.8 | 382.9 | +0.6% |
| Opaque wall between camera and displays | 185.1 | 325.2 | **+75.7%** |
| Glass between camera and displays | 181.7 | 181.3 | −0.2% |

### Frame times and slow frames behind the opaque wall

| Renderer | Mean frame time, OFF → FULL | 1% low FPS, OFF → FULL |
| --- | ---: | ---: |
| No shaders | 3.23 → 1.76 ms | 135.8–137.9 → 199.3–200.0 |
| Complementary Unbound r5.9.3 | 5.40 → 3.08 ms | 104.2–106.5 → 168.8–170.9 |

The 1% low values are the **range of the two individual repetitions**, not a pooled percentile. Each phase calculates 1% low FPS from the mean duration of its slowest 1% of frames. Higher values indicate better performance during slow frames. Mean frame time fell by **45.5% without shaders** and **43.1% with shaders** in this scene.

### What changed

The reported mean number of prepared display render states per frame was the same in both renderer configurations:

| Scene | DMO OFF | DMO FULL |
| --- | ---: | ---: |
| Displays in front of the camera | 600 | 600 |
| Camera turned away from displays | 60 | 30 |
| Opaque wall between camera and displays | 600 | 300 |
| Glass between camera and displays | 600 | 600 |

These are **prepared render states**, rather than GPU draw-call counts or a count of all loaded entities.

The largest measured benefit was behind an opaque wall: **309.4 → 567.6 FPS without shaders** and **185.1 → 325.2 FPS with shaders**. That improvement appeared in both repetitions and was accompanied by better 1% low FPS.

Turning away from the displays gave **+18.4% without shaders**, while the shader run gained only **+0.6%**, despite the lower prepared-state count. Preparing fewer states does not by itself guarantee a proportional FPS increase. The front-facing and glass scenes showed little change in average FPS.

These preliminary results cover **one PC, one synthetic arena and two repetitions per mode**. The quick plan enables several DMO optimizations together, so it does not isolate the contribution of each algorithm. Results depend on the scene, hardware, resource packs and shaders; no universal FPS gain is claimed.

## Build from source

Install JDK 25 and Python 3, then run:

```sh
python3 build.py
```

The script downloads checksum-verified compile dependencies, compiles the mod, runs the automated checks and writes the JAR to `build/libs/`. Minecraft and third-party libraries are not bundled in the output.

To select a particular Java executable, set `DISPLAY_MODEL_OPTIMIZER_JAVA`. A Gradle build is also available with `gradle build` when Gradle and a Java 25 toolchain are installed.

## Status and testing

This is an **experimental beta**. Automated checks cover draw lookup equivalence, cache ownership and lifetime, configuration transfer, bounds calculations and scenes using the real Entity Culling backend. The renamed mixins have also been applied to official Minecraft 26.3 bytecode alongside the original Entity Culling mixins.

These checks do not replace an actual Minecraft client launch or a controlled FPS benchmark. Full modpack and shader-pack behavior still needs testing in game. See [testing notes](docs/TESTING.md) and [validation details](VALIDATION.json).

## Credits

Thanks to the authors and contributors of these projects:

- [CullDisplayEntities](https://github.com/JustAlittleWolf/CullDisplayEntities) — for the model-based bounding-box approach to display-entity culling that inspired DMO's off-screen culling.
- [EntityCulling](https://github.com/tr7zw/EntityCulling) — for the occlusion-culling implementation that DMO integrates with to hide display models behind opaque blocks.

## Development transparency

Generative AI was used to produce the implementation, documentation, translations and icon. The mod does not use AI at runtime. Its source is available for review and contributions.

Maintained by **Nodelll**. Licensed under [MIT](LICENSE).
