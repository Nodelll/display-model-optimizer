# Changelog

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
