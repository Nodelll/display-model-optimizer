<!-- <p align="center"> -->
<!--  <img src="src/main/resources/assets/display_model_optimizer/icon.png" width="112" height="112" alt="Display Model Optimizer icon"> -->
<!-- </p> -->

# Display Model Optimizer

**Client-side rendering optimization for Minecraft display models.**

[Русский](README.ru.md) · [Download](https://github.com/Nodelll/display-model-optimizer/releases) · [Report a bug](https://github.com/Nodelll/display-model-optimizer/issues) · [Build status](https://github.com/Nodelll/display-model-optimizer/actions)

Display Model Optimizer reduces repeated rendering work for `item_display` and `block_display` entities. It is designed for decorative builds, custom furniture, showcases and other areas containing many display models, in singleplayer or on servers.

**Prerelease ports:** Minecraft **26.2** and **1.21.11** builds are available separately as **0.8.0-beta.1**. See [dependencies, version-specific behavior and build instructions](ports/README.md). The stable build and FPS results below target **26.3**.

## Features

- **Block model caching:** prepares identical supported block models once per frame.
- **Persistent block geometry cache (0.8.0):** reuses converted geometry across frames for visible block displays through optional Fabric Renderer API.
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

Install the mod on the **client**. Model caching, draw lookups and the distance limit work without Entity Culling. Fabric API is optional; the new persistent geometry cache needs **Fabric Renderer API 17.0.16+**. Other features work without it.

## Installation and settings

1. Download the JAR from [Releases](https://github.com/Nodelll/display-model-optimizer/releases).
2. Remove the previous version from your instance's `mods` folder, place the new JAR there and restart Minecraft.
3. Open **Mod Menu → Display Model Optimizer**, or edit `config/display-model-optimizer.properties` manually and restart the game.

If DMO Benchmark 1.1.0 was installed for the alpha tests, remove its helper JAR after testing: it requires the exact alpha build and is a separate companion, not part of the stable mod.

**The default maximum display range is 18 blocks.** Increase `displayRenderDistance` for more distant models, or set it to **0** to keep the original game/server range. The limit applies to both `item_display` and `block_display`.

Off-screen culling uses Minecraft's normal camera frustum and works independently of Entity Culling. The new `cullOffscreenDisplays` option is enabled by default and is separate from `cullOccludedDisplays`. Actual model extents, transformations and conservative interpolation bounds determine visibility. See [culling details](docs/CULLING.md).

The occlusion option uses the installed Entity Culling mod. Visibility follows its transparency rules; full opaque blocks can hide models, while glass and invisible barriers do not count as opaque walls. Animated item models, moving display entities and models with unknown bounds may remain visible.

## Performance tests

Two complete **48-phase** singleplayer runs were recorded on **15 October 2026**, using **DMO 0.8.0-alpha.1** and **DMO Benchmark 1.1.0**, with and without **Complementary Unbound r5.9.3, High preset**. The High preset was reported by the tester; Iris confirms the shader pack and whether shaders are enabled. Release **0.8.0** promotes the measured implementation; the release verification compares its production class bytes with the tested build.

### Test setup and method

- **Hardware:** AMD Ryzen 5 5600, NVIDIA GeForce RTX 3070 8 GB, 16 GB RAM.
- **Client:** Minecraft 26.3, Fabric Loader 0.19.5, Microsoft Java 25.0.1, Fabric API 0.163.0+26.3, Sodium 0.9.2, Iris 1.11.7 and Entity Culling 1.11.3.
- **Scene:** 600 displays — **300 item displays and 300 block displays**; no additional resource pack; **1920 × 1080**, render distance 16 chunks, simulation distance 12 chunks, FOV 90, maximum Java heap 4 GiB.
- **Each run:** 60-second initial warm-up; 8-second settling before later phases; two **30-second** measurements per mode in an **A → B → B → A** sequence. Separate 2-second diagnostic windows are excluded from timed FPS. Both runs took about 33 minutes, completed all phases and restored the original settings.
- **Calculation:** FPS is total recorded frames divided by total measured time across the two repetitions. Percentages use unrounded results; tables are rounded.

**OFF** disables DMO at runtime; Sodium, Iris and Entity Culling remain installed. **FULL** enables the combined DMO features with extra display distance set to **0**. These are comparisons within the same modpack, retaining the original display range. The isolated tests change exactly one DMO option; the distance comparison is explicitly separate.

### Without shaders

| Scene | DMO OFF, FPS | DMO FULL, FPS | Change |
| --- | ---: | ---: | ---: |
| In front of the camera | 292.9 | 304.2 | **+3.8%** |
| Behind the camera | 1022.7 | 1251.7 | **+22.4%** |
| Opaque wall | 293.4 | 639.8 | **+118.1%** |
| Glass | 288.2 | 298.4 | **+3.5%** |

### Complementary Unbound r5.9.3 — High preset

| Scene | DMO OFF, FPS | DMO FULL, FPS | Change |
| --- | ---: | ---: | ---: |
| In front of the camera | 111.3 | 114.5 | **+2.9%** |
| Behind the camera | 180.8 | 325.9 | **+80.3%** |
| Opaque wall | 117.2 | 165.3 | **+41.1%** |
| Glass | 114.9 | 117.2 | **+2.0%** |

### Individual optimizations

These comparisons keep the master switch enabled and toggle one option, with the other optimization options disabled. The geometry tests use one repeated block state, six different states, or states changed every second; the arena still includes 300 item displays.

| Optimization | No shaders, FPS | Change | Unbound High, FPS | Change |
| --- | ---: | ---: | ---: | ---: |
| Draw-group index | 287.3 → 294.2 | +2.4% | 116.9 → 118.2 | +1.1% |
| Per-frame model cache* | 267.0 → 265.9 | −0.4% | 105.1 → 107.0 | +1.8% |
| Geometry cache: repeated blocks | 262.8 → 278.7 | +6.1% | 107.1 → 109.9 | +2.7% |
| Geometry cache: varied blocks | 285.2 → 298.8 | +4.7% | 116.1 → 120.2 | +3.5% |
| Geometry cache: changing blocks | 283.1 → 296.1 | +4.6% | 115.7 → 119.3 | +3.1% |
| Off-screen culling | 1018.0 → 1227.1 | +20.5% | 185.6 → 332.7 | +79.3% |
| Opaque-wall culling | 283.3 → 573.5 | +102.4% | 114.7 → 152.1 | +32.7% |
| Distance limit: 0 → 18 blocks** | 283.7 → 1417.4 | +399.6% | 114.4 → 368.3 | +222.0% |

\* **Per-frame cache: BYPASS in this modpack.** Renderer-added state is conservatively left on the original preparation path. No snapshot hits were recorded, so its small FPS differences cannot be attributed to effective cache reuse.

\*\* The distance test places all 600 displays beyond 18 blocks. They are all excluded when the limit is enabled; that gain describes reduced visibility in this scene.

### Frame times behind the opaque wall

| Renderer | Mean frame time OFF → FULL | 1% low FPS OFF → FULL |
| --- | ---: | ---: |
| No shaders | 3.41 → 1.56 ms | 137.8–141.7 → 231.7–234.7 |
| Complementary Unbound r5.9.3 — High | 8.54 → 6.05 ms | 66.3–66.4 → 87.4–87.8 |

The 1% low values above are the **range of the two individual repetitions**, not a pooled percentile. Each phase uses the reciprocal mean duration of its slowest 1% of frames.

### Diagnostics and limits

- The new geometry cache recorded **1,046,400 mesh replays without shaders** and **846,600 with Unbound High** during its isolated diagnostic windows. Cache hits were observed in the warmed scenes. Actual client-side block-state changes and resource-reload cache invalidation passed their checks; all ten automatic in-game checks passed in both runs.
- Behind the opaque wall, prepared item-display states fell **300 → 0**, while block-display states stayed **300 → 300**. Behind the camera the counts changed **30 items + 30 blocks → 0 items + 30 blocks**. The tested FRAPI/Sodium path did not provide usable DMO block-model bounds: off-screen/occlusion culling of block displays is a known limitation here. Caching and the distance limit still apply to blocks. Prepared-state counts are not GPU draw-call counts.
- In the changing-block shader comparison, mean phase 1% low fell **64.9 → 60.0 FPS (−7.6%)** despite higher average FPS. Long frames coincide with GC pauses of about **11–15 ms**; more repetitions are needed to establish whether this is a repeatable regression.
- The first measured frame in the no-shader run took **75 ms**, affecting that phase's 1% low. Sun position also changes between screenshots: time and weather were not frozen. Small percentage differences are indicative; ABBA ordering reduces drift but does not eliminate it.
- The tested scenes showed no steadily growing Java private-memory usage over either 33-minute run. With shaders, measured Java private memory was about **3.9–4.2 GiB**, GPU VRAM **3.0–3.2 GiB**, and peak GPU temperature **75 °C**. GPU readings describe the whole adapter.

Results cover **one PC, one synthetic arena and two repetitions per mode**. They do not establish a universal FPS gain, pixel-perfect output or compatibility with every shader/resource pack. Animated models, moving displays and unknown bounds may remain visible. Further visual checks with the intended server models remain useful.

[Source measurements and provenance](docs/benchmarks/2026-10-15/results.json) · [No-shader phase CSV](docs/benchmarks/2026-10-15/summary-no-shaders.csv) · [Unbound High phase CSV](docs/benchmarks/2026-10-15/summary-shaders-high.csv) · [Geometry-cache algorithm](docs/BLOCK_GEOMETRY_CACHE.md) · [Historical 0.7.1 measurements](docs/benchmarks/0.7.1.md)

## Build from source

Install JDK 25 and Python 3, then run:

```sh
python3 build.py
```

The script downloads checksum-verified compile dependencies, compiles the mod, runs the automated checks and writes the JAR to `build/libs/`. Minecraft and third-party libraries are not bundled in the output.

To select a particular Java executable, set `DISPLAY_MODEL_OPTIMIZER_JAVA`. A Gradle build is also available with `gradle build` when Gradle and a Java 25 toolchain are installed.

## Status and testing

**0.8.0 is a stable release for Minecraft 26.3.** Automated checks cover draw lookup equivalence, cache ownership and lifetime, configuration transfer, bounds calculations and scenes using the real Entity Culling backend. The renamed mixins have also been applied to official Minecraft 26.3 bytecode alongside the original Entity Culling mixins.

Two full in-game runs with and without Complementary Unbound r5.9.3 High are recorded above. Automated checks and these runs do not establish compatibility with every modpack, custom model or shader pack. See [testing notes](docs/TESTING.md) and [validation details](VALIDATION.json).

## Credits

Thanks to the authors and contributors of these projects:

- [CullDisplayEntities](https://github.com/JustAlittleWolf/CullDisplayEntities) — for the model-based bounding-box approach to display-entity culling that inspired DMO's off-screen culling.
- [EntityCulling](https://github.com/tr7zw/EntityCulling) — for the occlusion-culling implementation that DMO integrates with to hide display models behind opaque blocks.

## Development transparency

Generative AI was used to produce the implementation, documentation, translations and icon. The mod does not use AI at runtime. Its source is available for review and contributions.

Maintained by **Nodelll**. Licensed under [MIT](LICENSE).
