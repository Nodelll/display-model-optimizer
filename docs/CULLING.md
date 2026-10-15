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

## Camera and wall checks

Camera culling uses `DisplayRenderer`'s normal visibility hooks and works with zero server-provided width/height. The entity's physical bounding box, coordinates and server metadata are preserved.

Entity Culling uses separate optional renderer bridge hooks for eligibility and model bounds. Turning off the wall option cannot enable wall culling through the camera option, and turning off the camera option does not disable wall culling. The optional methods are skipped when Entity Culling is absent. When the optimizer is disabled, the renderer keeps its original behavior.

## Verification

The build tests the official Minecraft 26.3 Frustum against the production cached AABBs: camera turns, all screen edges, translated models, large custom extents, negative scale and transformed vertices. The same test also ran with Entity Culling, Mod Menu and Fabric Loader excluded from its runtime classpath.

The standalone Mixin harness applies the hooks both without Entity Culling and together with the original Entity Culling 1.11.3 renderer/extraction mixins. It verifies injection points, original frustum operations and method stacks. A complete game/modpack/shader test and an FPS measurement are still required.

## Русский

Вычисление границ для проверки видимости вдохновлено принципом [CullDisplayEntities](https://github.com/JustAlittleWolf/CullDisplayEntities). Используется собственный кэш реальной геометрии моделей Display Model Optimizer.

**Скрытие вне кадра** (`cullOffscreenDisplays`) работает обычной проверкой камеры Minecraft и не требует Entity Culling. **Скрытие за блоками** (`cullOccludedDisplays`) настраивается отдельно и использует установленный Entity Culling. Дополнительный лимит дальности по умолчанию — **18 блоков**.

Учитываются реальные размеры модели, перенос, отрицательный масштаб, поворот, billboard-режим и интерполяция. Если часть крупной модели видна, её отрисовка сохраняется. При неизменной позе готовые границы используются повторно. Неизвестные и анимированные модели предметов, а также движущиеся опорные точки сохраняют видимость. Физические хитбоксы и серверные данные не меняются.

В Mod Menu новый переключатель находится во вкладке **«Основное»**. Он включён по умолчанию; старые настройки сохраняются при автоматическом добавлении восьмого параметра. Для использования этого механизма отдельный CullDisplayEntities не требуется.
