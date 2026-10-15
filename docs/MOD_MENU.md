# Mod Menu integration

Display Model Optimizer 0.7.0 targets the official **Mod Menu 21.0.0 API for Minecraft 26.3**. Mod Menu remains optional. No separate configuration library is needed.

## Mod card

| Feature | Integration |
| --- | --- |
| Icon, author, version, license | Packaged icon, Nodelll, installed mod version and MIT from Fabric metadata |
| Name, summary, description | Official translation keys in `en_us` and `ru_ru`; description stays one sentence |
| Client badge | Automatically supplied by Mod Menu from `environment: client` |
| Website and issue reporting | Standard Fabric `homepage` and `issues` contacts |
| Source code | Standard Fabric `sources` contact, displayed by Mod Menu |
| Additional links | Localized downloads, changelog and user guide in `custom.modmenu.links` |
| Configuration button | Official `getModConfigScreenFactory()` entrypoint |
| Update notification | Official `getUpdateChecker()` and `UpdateInfo` APIs, with translated version message |

The mod is a standalone client optimization tool. Library/deprecated badges, parent modules and modpack ownership do not describe it, so those declarations are omitted. It does not register settings screens or update checkers for other mods.

## Configuration

| Tab | Contents |
| --- | --- |
| General | Main switch, block model cache, off-screen culling, occlusion option, display distance |
| Advanced | Model cache capacity, draw-index threshold and verification interval |
| About | Installed mod/Mod Menu versions, author, Entity Culling availability and version, project links |

Every option has an English/Russian tooltip. Vanilla buttons and edit boxes provide keyboard focus and narration; the screen narration also includes the current tab and validation status. Changes stay in a single draft across tabs and resizing. Save validates the entire draft, including hidden fields, and uses the existing atomic settings writer. Cancel and Escape discard edits. Reset changes the draft only.

The configuration has eight properties; seven-property configurations upgrade automatically while preserving existing values. Off-screen culling is independent of Entity Culling and the occlusion switch. The default distance remains **18 blocks**; existing explicit values are preserved.

## Updates

The checker is invoked by Mod Menu on its worker thread. Mod Menu's global update switch, per-mod exclusion list and preferred channel apply. The checker does not add a startup hook, background timer or automatic installer.

It reads the project's public GitHub release feed, chooses the highest newer semantic version with an uploaded `display-model-optimizer-VERSION+mcMINECRAFT.jar` asset, and requires the asset version to match the release tag. A stable preference excludes pre-releases; beta includes beta/release candidates and stable releases; alpha includes all channels. GitHub pre-releases with a plain numeric version are treated as beta. Unknown pre-release identifiers are treated as alpha.

Only release pages in this repository can be returned to Mod Menu. The request has 5-second connection/read timeouts, disables redirects and limits the body to 2 MiB. Offline operation, HTTP errors, API rate limits and invalid responses return no update notification. Version selection is tested without making network requests.

This custom source works before a Modrinth project exists and replaces Mod Menu's default Modrinth hash lookup for this mod. Releasing to Modrinth does not require changing the metadata links; GitHub releases remain the update source.

## Русский

Интеграция включает локализованную карточку мода, логотип, автора Nodelll, лицензию MIT, стандартный значок клиентского мода, ссылки на сайт, исходники, ошибки, скачивание, историю изменений и инструкцию.

Экран настроек разделён на **«Основное»**, **«Дополнительно»** и **«О моде»**. Доступны все восемь параметров, подсказки, проверка значений, сохранение, отмена и сброс. Редактирование на разных вкладках относится к одному черновику. Дальность по умолчанию — **18 блоков**, настройки предыдущей версии сохраняются. Новый переключатель «Скрывать модели вне кадра» работает независимо от Entity Culling и скрытия за блоками.

Уведомления об обновлениях поступают из релизов GitHub. Учитываются запущенная версия Minecraft и выбранный в Mod Menu канал обновлений. Отключение проверки в Mod Menu распространяется и на наш мод. При выборе обновления открывается страница релиза, самостоятельной установки файлов нет. Ошибки сети не мешают игре. Дополнительные библиотеки настроек не нужны; Mod Menu и Entity Culling остаются необязательными.

## API references

- [Mod Menu 21.0.0 developer documentation](https://github.com/TerraformersMC/ModMenu/tree/v21.0.0#developers)
- [ModMenuApi](https://github.com/TerraformersMC/ModMenu/blob/v21.0.0/src/main/java/com/terraformersmc/modmenu/api/ModMenuApi.java)
- [UpdateChecker](https://github.com/TerraformersMC/ModMenu/blob/v21.0.0/src/main/java/com/terraformersmc/modmenu/api/UpdateChecker.java)
- [UpdateInfo](https://github.com/TerraformersMC/ModMenu/blob/v21.0.0/src/main/java/com/terraformersmc/modmenu/api/UpdateInfo.java)
