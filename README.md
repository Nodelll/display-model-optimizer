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
- **Mod Menu integration:** English/Russian settings tabs, project and download links, dependency status and compatible GitHub update notifications.

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

[Version 0.7.1](https://github.com/Nodelll/display-model-optimizer/releases/tag/v0.7.1) fixes the Minecraft 26.3 crash when opening DMO settings through Mod Menu (`Can only blur once per frame`).

1. Download the JAR from [Releases](https://github.com/Nodelll/display-model-optimizer/releases).
2. Remove the previous version from your instance's `mods` folder, place the new JAR there and restart Minecraft.
3. Open **Mod Menu → Display Model Optimizer**, or edit `config/display-model-optimizer.properties` manually and restart the game.

**The default maximum display range is 18 blocks.** Increase `displayRenderDistance` for more distant models, or set it to **0** to keep the original game/server range. The limit applies to both `item_display` and `block_display`.

Off-screen culling uses Minecraft's normal camera frustum and works independently of Entity Culling. The new `cullOffscreenDisplays` option is enabled by default and is separate from `cullOccludedDisplays`. Actual model extents, transformations and conservative interpolation bounds determine visibility. See [culling details](docs/CULLING.md).

The occlusion option uses the installed Entity Culling mod. Visibility follows its transparency rules; full opaque blocks can hide models, while glass and invisible barriers do not count as opaque walls. Animated item models, moving display entities and models with unknown bounds may remain visible.

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

## Development transparency

Generative AI was used to produce the implementation, documentation, translations and icon. The mod does not use AI at runtime. Its source is available for review and contributions.

Maintained by **Nodelll**. Licensed under [MIT](LICENSE).
