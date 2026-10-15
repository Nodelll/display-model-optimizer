# Display Model Optimizer

**Display Model Optimizer** is a Fabric client mod that optimizes rendering of `item_display` and `block_display` entities. It is designed for worlds and servers with many display models, including custom furniture, decorative builds and showcases.

## Features

- **Persistent block geometry cache, new in 0.8.0:** reuses supported block-display geometry across frames, with bounded storage and invalidation on resource, world and settings changes. Requires optional **Fabric Renderer API 17.0.16+**.
- **Per-frame block model caching:** reuses preparation of identical supported block models within each frame. Unsupported renderer state follows the original path.
- **Faster draw-group lookups:** reduces lookup work when preparing models for rendering.
- **Off-screen culling:** uses known model bounds to skip displays outside the camera view. Works without Entity Culling.
- **Configurable display distance:** adds a limit of **18 blocks by default**. Set it to **0** to preserve the original Minecraft/server range.

**Entity Culling is optional** and is needed for hiding supported models behind opaque blocks. Model caching, faster lookups, the distance limit and off-screen culling work independently. **Fabric API is optional**; its Renderer API is needed for the persistent geometry cache. **Mod Menu is optional** and provides the settings screen; configuration is also available in `config/display-model-optimizer.properties`.

## Performance

Two complete **48-phase singleplayer runs** tested the **0.8.0 implementation**, using test build **0.8.0-alpha.1** and DMO Benchmark 1.1.0. The arena contained **600 displays: 300 item displays and 300 block displays**.

With an opaque wall between the camera and the displays, the combined DMO comparison produced:

| Rendering | DMO disabled | DMO enabled | Change |
| --- | ---: | ---: | ---: |
| Without shaders | 293.4 FPS | 639.8 FPS | **+118.1%** |
| Complementary Unbound r5.9.3 — **High** | 117.2 FPS | 165.3 FPS | **+41.1%** |

With displays directly in front of the camera, the combined gain was **+3.8% without shaders** and **+2.9% with Unbound High**. Turning away gave **+22.4%** and **+80.3%**, respectively; the glass scene gave **+3.5%** and **+2.0%**.

The new persistent geometry cache was also tested separately, changing only its own switch:

| Block workload | Without shaders | Unbound r5.9.3 — High |
| --- | ---: | ---: |
| Identical block states | **+6.1% FPS** | **+2.7% FPS** |
| Different block states | **+4.7% FPS** | **+3.5% FPS** |
| States changing every second | **+4.6% FPS** | **+3.1% FPS** |

Measured on **15 October 2026** with a **Ryzen 5 5600, RTX 3070 8 GB and 16 GB RAM**, at **1920 × 1080**, FOV 90, render distance 16 chunks and a 4 GiB maximum Java heap. The shader pack was **Complementary Unbound r5.9.3, High preset**; the preset was reported by the tester. No additional resource pack was active.

Each mode combines two **30-second** measurements in an **OFF → ON → ON → OFF** sequence, following warm-up and settling. FPS is total frames divided by total measured time. Separate diagnostic windows are excluded from FPS. Sodium 0.9.2, Iris 1.11.7, Fabric API 0.163.0+26.3 and Entity Culling 1.11.3 remained installed in both modes. The combined comparison used an additional display-distance limit of **0**, retaining model visibility range.

**Limitations observed in these runs:** the older per-frame cache used a safe **BYPASS** on this renderer path. Behind the wall, prepared item-display states fell from 300 to 0, while block-display states remained 300; block culling is therefore limited on the tested FRAPI/Sodium path. Unknown bounds stay visible. In the changing-block shader test, average FPS increased, but mean phase **1% low fell 64.9 → 60.0 FPS (−7.6%)**, with long frames coinciding with GC pauses.

These are results from one PC and one synthetic arena. Time and weather were not frozen, so small differences are indicative. Performance depends on model complexity, hardware, shaders and installed mods. Animated item models, moving displays and models with unknown bounds may remain visible conservatively. Automated checks and counters do not prove pixel-perfect output or compatibility with every shader pack.

[Full results, frame times, diagnostics and methodology](https://github.com/Nodelll/display-model-optimizer#performance-tests)

<details>
<summary>Описание на русском</summary>

**Display Model Optimizer** — клиентский мод для Fabric, оптимизирующий отрисовку сущностей `item_display` и `block_display`. Он подходит для миров и серверов с большим количеством моделей: кастомной мебелью, декорациями, витринами и другими объектами из display-сущностей.

## Возможности

- **Межкадровый кэш геометрии, новое в 0.8.0:** повторно использует геометрию поддерживаемых моделей блоков между кадрами. Объём ограничен, предусмотрен сброс при изменении ресурсов, мира и настроек. Требует необязательный **Fabric Renderer API 17.0.16+**.
- **Кэширование моделей на кадр:** повторно использует подготовку одинаковых поддерживаемых моделей блоков в пределах кадра. Неподдерживаемые данные рендерера обрабатываются исходным способом.
- **Ускоренный поиск групп:** сокращает затраты на поиск при подготовке моделей к отрисовке.
- **Отсечение вне кадра:** использует известные границы моделей, чтобы пропускать дисплеи за пределами обзора камеры. Работает без Entity Culling.
- **Настраиваемая дальность:** дополнительное ограничение **18 блоков по умолчанию**. Значение **0** сохраняет исходную дальность Minecraft и сервера.

**Entity Culling необязателен** и нужен для скрытия поддерживаемых моделей за непрозрачными блоками. Кэширование, ускоренный поиск групп, ограничение дальности и отсечение вне кадра работают самостоятельно. **Fabric API необязателен**; его Renderer API нужен для межкадрового кэша геометрии. **Mod Menu необязателен** и предоставляет экран настроек; конфигурацию также можно изменить в `config/display-model-optimizer.properties`.

## Производительность

Два полных прогона по **48 этапов в одиночном мире** проверили **реализацию 0.8.0** на тестовой сборке **0.8.0-alpha.1**, используя DMO Benchmark 1.1.0. Арена содержала **600 сущностей: 300 item_display и 300 block_display**.

В сцене с непрозрачной стеной между камерой и дисплеями совместное включение функций DMO дало:

| Рендер | DMO выключен | DMO включён | Изменение |
| --- | ---: | ---: | ---: |
| Без шейдеров | 293,4 FPS | 639,8 FPS | **+118,1%** |
| Complementary Unbound r5.9.3 — **High** | 117,2 FPS | 165,3 FPS | **+41,1%** |

Перед камерой общий прирост составил **+3,8% без шейдеров** и **+2,9% с Unbound High**. При развороте от дисплеев — **+22,4%** и **+80,3%** соответственно; за стеклом — **+3,5%** и **+2,0%**.

Новый межкадровый кэш геометрии проверен отдельно, с изменением только его переключателя:

| Нагрузка блоков | Без шейдеров | Unbound r5.9.3 — High |
| --- | ---: | ---: |
| Одинаковые состояния | **+6,1% FPS** | **+2,7% FPS** |
| Разные состояния | **+4,7% FPS** | **+3,5% FPS** |
| Состояния меняются каждую секунду | **+4,6% FPS** | **+3,1% FPS** |

Замеры выполнены **15 октября 2026 года** на **Ryzen 5 5600, RTX 3070 8 ГБ и 16 ГБ оперативной памяти**, при **1920 × 1080**, FOV 90, прорисовке 16 чанков и максимальном heap Java 4 ГиБ. Шейдер-пак — **Complementary Unbound r5.9.3, пресет High**; пресет указан тестировщиком. Сторонний ресурс-пак не использовался.

Результаты объединяют по два замера каждого режима длительностью **30 секунд**, порядок — **выключен → включён → включён → выключен**, после прогрева и стабилизации. FPS рассчитывается делением общего числа кадров на общее время измерений. Отдельные диагностические окна в FPS не входят. В обоих режимах оставались Sodium 0.9.2, Iris 1.11.7, Fabric API 0.163.0+26.3 и Entity Culling 1.11.3. Дополнительная дальность в совместном сравнении установлена в **0**, поэтому дальность видимости моделей сохраняется.

**Ограничения этих прогонов:** старый кэш на кадр использовал безопасный обход **BYPASS** на данном пути рендерера. За стеной число подготовленных состояний предметов уменьшилось с 300 до 0, а блоков осталось 300; отсечение block_display на проверенном пути FRAPI/Sodium ограничено. Модели с неизвестными границами сохраняют видимость. В меняющейся сцене с шейдерами средний FPS вырос, но средний по этапам **1% low снизился 64,9 → 60,0 FPS (−7,6%)**; длинные кадры совпали с паузами GC.

Это результаты одного компьютера и одной синтетической арены. Время суток и погода не фиксировались, поэтому малые различия ориентировочны. Производительность зависит от сложности моделей, оборудования, шейдеров и модов. Анимированные модели предметов, движущиеся дисплеи и модели с неизвестными границами могут оставаться видимыми для сохранения корректной отрисовки. Автоматические проверки и счётчики не доказывают правильность каждого пикселя и совместимость со всеми шейдер-паками.

[Полные результаты, время кадра, диагностика и методика](https://github.com/Nodelll/display-model-optimizer/blob/main/README.ru.md#результаты-тестирования-fps)

</details>

## Credits

Thanks to the authors and contributors of these projects:

- [EntityCulling](https://modrinth.com/mod/entityculling) — for the occlusion-culling implementation that DMO integrates with to hide display models behind opaque blocks.
- [CullDisplayEntities](https://modrinth.com/mod/cull-display-entities) — for the model-based bounding-box approach to display-entity culling that inspired DMO’s off-screen culling.
