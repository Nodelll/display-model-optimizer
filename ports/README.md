# Fabric prerelease ports

DMO **0.8.0-beta.1** adds separate client builds for **Minecraft 26.2** and **Minecraft 1.21.11**. These builds are prereleases. The stable Minecraft 26.3 build remains 0.8.0.

## Installation

Download the JAR for your exact Minecraft version from [the prerelease](https://github.com/Nodelll/display-model-optimizer/releases/tag/v0.8.0-beta.1), and put it into the client's `mods` directory. Install only one DMO JAR.

| Minecraft | DMO JAR suffix | Runtime | Fabric Loader | Optional dependencies used for verification |
| --- | --- | --- | --- | --- |
| **26.2** | `+mc26.2.jar` | **Java 25** | **0.19.5+** | Fabric API **0.161.0+26.2**, Mod Menu **20.0.3**, Entity Culling **1.11.3**, Sodium **0.9.2** |
| **1.21.11** | `+mc1.21.11.jar` | **Java 21** | **0.19.5+** | Fabric API **0.141.6+1.21.11**, Mod Menu **17.0.1**, Entity Culling **1.11.3**, Sodium **0.8.14** |

Use dependency builds for the same Minecraft version. Fabric API enables persistent geometry caching; Entity Culling enables opaque-block occlusion; Mod Menu provides the settings screen. They remain optional. Configuration is also available in `config/display-model-optimizer.properties`.

The default additional display distance is **18 blocks**. Set `displayRenderDistance=0` to retain the original Minecraft/server range. Disabling DMO restores the original rendering paths.

## Version-specific implementation

| Function | Minecraft 26.2 | Minecraft 1.21.11 |
| --- | --- | --- |
| Per-frame block preparation cache | `BlockModelRenderState` snapshots for supported vanilla output | Seed-42 vanilla model-part collection during drawing, scoped to queued block-display commands |
| Persistent block geometry cache | FRAPI **14.1.4+** model-space mesh replay | FRAPI **8.0.3+** model-emission wrapper, before Indigo/Sodium redirects drawing |
| Draw-group lookup optimization | Supported by this version's draw-group pipeline | Not installed: 1.21.11 has a different pipeline and lacks the corresponding lookup target; its two advanced lookup settings are hidden |
| Distance limit and item-model off-screen culling | Supported | Supported |
| Optional Entity Culling bridge | Uses model bounds where available | Uses model bounds where available |
| Resource reload invalidation | Clears caches when reload starts and completes | Clears caches when reload starts and completes |
| Settings UI | GUI extraction API, one background pass | `GuiGraphics.render`, one background pass |
| Production namespace | Unobfuscated | Loom-remapped **intermediary**; Java 21 class files |

Both persistent caches store renderer-owned geometry only. Current lighting, tint, pose and outline passes remain in the original renderer. Geometry keys, world/resource/config generation, graphics settings and renderer identity control reuse. Null geometry keys and oversized meshes retain the original rendering path. Unknown mod models bypass the vanilla parts cache.

Culling stays conservative for animated items, moving displays and unknown bounds. In particular, block models rendered through FRAPI may stay visible when their emitted geometry cannot be bounded safely. No unit-cube substitute is used for unknown custom geometry.

The 26.2 port uses Minecraft/Blaze3D APIs and adds no direct OpenGL calls. The offline checks do not constitute a Vulkan, shader-pack or interactive in-game compatibility test.

## Build and validation

Use **JDK 25**, Python 3 and the checked-in Gradle wrapper. JDK 25 also compiles the 1.21.11 port with `--release 21`.

```sh
cd ports
./gradlew :mc26_2:build :mc1_21_11:build :mc26_2:writeValidationEnvironment :mc1_21_11:writeValidationEnvironment
cd ..
python3 ports/validate.py 26.2
DISPLAY_MODEL_OPTIMIZER_JAVA21=/path/to/java21/bin/java python3 ports/validate.py 1.21.11
```

On Windows use `gradlew.bat`, and set `DISPLAY_MODEL_OPTIMIZER_JAVA21` to the Java 21 executable before validating the older port. JARs and SHA-256 files are generated under `ports/<minecraft>/build/libs/`. Validation logs are under `ports/<minecraft>/build/validation/`.

Validation runs the shared cache/configuration/bounds/version-feed tests against each game's libraries. It transforms the actual production JAR against the target Minecraft classes, including **intermediary names on Java 21** for 1.21.11, checks optional-dependency absence and real Entity Culling/Indigo/Sodium mixin combinations, and compares replayed geometry using each target's real Sodium mesh implementation. Dependencies used by the standalone checks are checksum-pinned. No Minecraft or dependency classes are bundled in the mod.

**No new in-game FPS measurements are claimed for these ports.** The performance tables in the main README describe Minecraft **26.3** only. Opening a world, testing resource-pack reload, transformed/custom block displays, item animations and shader packs on the target game remain important prerelease checks.

## По-русски

Это **pre-release 0.8.0-beta.1** с отдельными JAR для **Minecraft 26.2** и **1.21.11**. Для 26.2 нужна **Java 25**, для 1.21.11 — **Java 21**. В обоих случаях требуется **Fabric Loader 0.19.5 или новее**. Устанавливайте один DMO JAR и зависимости именно для своей версии Minecraft; проверенные версии указаны в таблице выше.

В 1.21.11 кэш подготовки перенесён на сбор частей модели во время отрисовки, а кэш геометрии подключён перед рендерингом Indigo/Sodium. Хуки поиска draw-групп из 26.x в этой версии отсутствуют, поэтому соответствующая оптимизация не устанавливается, а её настройки скрыты. В 26.2 сохранён современный путь подготовки моделей, с адаптацией границ display и перезагрузки ресурсов под её API.

Fabric API, Mod Menu и Entity Culling остаются необязательными. Дальность по умолчанию — **18 блоков**; `displayRenderDistance=0` сохраняет исходную дальность Minecraft и сервера. Для моделей с неизвестными границами используется консервативное поведение: они могут оставаться видимыми.

Автоматические проверки не заменяют запуск мира в игре. **Старые FPS-замеры в README относятся к 26.3**; результаты для 26.2 и 1.21.11 пока не заявлены.
