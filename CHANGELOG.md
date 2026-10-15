# Changelog

## 0.8.0-beta.1 — Minecraft 26.2 / 1.21.11 prerelease ports — 2026-10-15

- Add independently built Fabric client ports for Minecraft 26.2 (Java 25) and 1.21.11 (Java 21), keeping 26.3 stable at 0.8.0.
- Adapt 26.2 display culling, resource reload invalidation and optional FRAPI 14 geometry caching.
- Port 1.21.11 preparation caching to vanilla model-part collection and FRAPI 8 geometry emission, preserving queued display scope and original lighting/tint/outline passes. Remap production classes and mixin selectors to intermediary.
- Omit the unavailable 26.x draw-group hook on 1.21.11 and hide its advanced settings; adapt the settings screen to its GuiGraphics API with one background pass.
- Validate each target with its own Minecraft/renderer binaries, production-JAR mixins and optional-dependency combinations. Add a CI gate for prerelease JARs and checksums.
- Document dependency versions and conservative culling behavior. Existing 26.3 FPS results are not reused as measurements of these ports.

## 0.8.0 — 2026-10-15

- Add an optional, bounded cache of converted `block_display` geometry across frames using Fabric Renderer API geometry keys and renderer-owned immutable meshes.
- Retain current tint, lighting, transform and submission paths; bypass models without keys and oversized meshes. Invalidate on resource/world/settings/graphics changes.
- Add an independent Advanced settings switch and migrate eight-option configurations without losing preferences; retain the benchmark helper's eight-argument constructor.
- Verify the real Fabric overwrite together with DMO and packed Sodium mesh parity, in addition to the existing checks. Run optional-dependency and Entity Culling transformation checks in CI.
- Derive the startup log version from mod metadata.
- Publish 0.8.0 as a stable GitHub release, with the same production classes as the benchmarked 0.8.0-alpha.1 build.
- Add complete 48-phase in-game benchmark results with and without Complementary Unbound r5.9.3, High preset, on Ryzen 5 5600 / RTX 3070 at 1920 × 1080.
- Record isolated optimization comparisons, per-kind culling limitations, the per-frame cache BYPASS on the tested FRAPI/Sodium path, and frame-time caveats. Retain the 0.7.1 results as historical documentation.

## 0.7.1 — 2026-10-15

- Fix the Mod Menu settings crash `IllegalStateException: Can only blur once per frame` on Minecraft 26.3 by letting the game extract the screen background once.
- Retain widget rendering and all existing settings, rendering optimizations and optional integrations.
- Add a regression check against the actual Minecraft screen wrapper, double-blur guard and 128 frame resets; run it in both Python and Gradle builds.

## 0.7.0 — 2026-10-15

- Enable vanilla camera-frustum culling for item and block displays using their actual cached model bounds, including displays whose server width/height are zero.
- Make off-screen culling work without Entity Culling, with an independent `cullOffscreenDisplays` switch enabled by default.
- Keep wall occlusion independent through optional Entity Culling renderer bridge hooks; verify transformation with and without the mod.
- Reuse immutable cached world-space AABBs for unchanged poses and keep conservative bounds for large, transformed and interpolated models.
- Upgrade seven-option configurations to eight while retaining preferences, unknown UTF-8 properties and the 18-block distance default.
- Add official Minecraft Frustum scenes for visible parts of large custom models and camera turns.

## 0.6.0 — 2026-10-15

- Expand Mod Menu configuration into General, Advanced and About tabs while retaining all seven options, draft validation, save/cancel/reset and the 18-block distance default.
- Show installed versions and Entity Culling availability, with project, issue, download and guide buttons.
- Add Fabric homepage/source/issue contacts and localized download, changelog and guide links on the mod card.
- Supply all three Mod Menu translation keys: name, summary and single-sentence description in English and Russian.
- Register the official Mod Menu UpdateChecker API for public GitHub releases. Respect user channel preferences, require a matching Minecraft binary, compare semantic versions and ignore unavailable feeds.
- Add offline update-selection checks; preserve the optional nature of Mod Menu and Entity Culling.

## 0.5.1 — 2026-10-15

- Use `display_model_optimizer`, `dev.nodelll.displaymodeloptimizer` and `display-model-optimizer.properties` consistently throughout the project.
- Transfer a unique complete previous configuration while preserving unknown properties and the original file.
- Retain frame-local block model caching, accelerated draw lookups, the configurable 18-block default range and optional Entity Culling integration.
- Include Mod Menu settings, English/Russian descriptions and the project icon.
- Validate configuration transfer and apply all nine mixin components to official Minecraft 26.3 bytecode, alongside the original Entity Culling hooks.
