# Display visibility

The renderer-bounds approach is inspired by [CullDisplayEntities](https://github.com/JustAlittleWolf/CullDisplayEntities). Display Model Optimizer uses its own existing model-geometry cache and Minecraft's normal frustum test.

| Mechanism | Setting | Default | Dependency |
| --- | --- | --- | --- |
| Outside the camera view | `cullOffscreenDisplays` | `true` | Minecraft only |
| Behind opaque blocks | `cullOccludedDisplays` | `true` | Entity Culling |
| Beyond the configured distance | `displayRenderDistance` | `18` blocks | Minecraft only |

All three are controlled by the main optimization switch. The two visibility switches are independent. A seven-option configuration upgrades to eight options automatically, preserving explicit values and unknown properties.

## Model bounds

Block bounds come from the prepared model's quads and special-renderer extents. Static item bounds come from the prepared item model, including its display context and facing. Geometry is captured when needed, cached on its display entity and invalidated when models or resources change.

The world-space box accounts for translation, negative scale, rotation, billboard constraints and transformation interpolation. Rotating/interpolating models use a conservative swept bound. An unchanged pose reuses its immutable Minecraft AABB. A visible part of a large model keeps that model eligible for rendering even when its entity anchor is outside the view.

The first model preparation establishes its geometry. Unknown geometry, animated item bounds and moving anchors retain visibility until a reliable box is available. Item and block displays are supported; text displays retain their original behavior.

**Observed limitation in the 0.8.0 benchmark:** on Fabric Renderer API 17.0.16 / Sodium 0.9.2, block-display geometry is stored in renderer mesh state and DMO did not obtain usable local block bounds. Item culling was observed; prepared block states did not decrease when these culling switches were enabled. Such block models remain visible conservatively. The persistent geometry cache and display-distance limit still work on that path. See [the measured counts](../README.md#performance-tests).

## Camera and wall checks

Camera culling uses `DisplayRenderer`'s normal visibility hooks and works with zero server-provided width/height. The entity's physical bounding box, coordinates and server metadata are preserved.

Entity Culling uses separate optional renderer bridge hooks for eligibility and model bounds. Turning off the wall option cannot enable wall culling through the camera option, and turning off the camera option does not disable wall culling. The optional methods are skipped when Entity Culling is absent. When the optimizer is disabled, the renderer keeps its original behavior.

## Verification

The build tests the official Minecraft 26.3 Frustum against the production cached AABBs: camera turns, all screen edges, translated models, large custom extents, negative scale and transformed vertices. The same test also ran with Entity Culling, Mod Menu and Fabric Loader excluded from its runtime classpath.

The standalone Mixin harness applies the hooks both without Entity Culling and together with the original Entity Culling 1.11.3 renderer/extraction mixins. It verifies injection points, original frustum operations and method stacks. Two full 48-phase game runs with and without Complementary Unbound r5.9.3 High are documented in the README; visual coverage of every modpack/shader/model is still outside their scope.

## Русский

Вычисление границ для проверки видимости вдохновлено принципом [CullDisplayEntities](https://github.com/JustAlittleWolf/CullDisplayEntities). Используется собственный кэш реальной геометрии моделей Display Model Optimizer.

**Скрытие вне кадра** (`cullOffscreenDisplays`) работает обычной проверкой камеры Minecraft и не требует Entity Culling. **Скрытие за блоками** (`cullOccludedDisplays`) настраивается отдельно и использует установленный Entity Culling. Дополнительный лимит дальности по умолчанию — **18 блоков**.

Учитываются реальные размеры модели, перенос, отрицательный масштаб, поворот, billboard-режим и интерполяция. Если часть крупной модели видна, её отрисовка сохраняется. При неизменной позе готовые границы используются повторно. Неизвестные и анимированные модели предметов, а также движущиеся опорные точки сохраняют видимость. Физические хитбоксы и серверные данные не меняются.

В замерах 0.8.0 с Fabric Renderer API 17.0.16 / Sodium 0.9.2 пригодные границы block_display не были получены из mesh-состояния рендерера. Отсечение предметов наблюдалось, а подготовка блоков при включении этих переключателей не уменьшилась. Такие модели блоков сохраняют видимость; межкадровый кэш геометрии и лимит дальности продолжают работать.

В Mod Menu новый переключатель находится во вкладке **«Основное»**. Он включён по умолчанию; старые настройки сохраняются при автоматическом добавлении восьмого параметра. Для использования этого механизма отдельный CullDisplayEntities не требуется.
