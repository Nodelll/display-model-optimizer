# Changelog

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
