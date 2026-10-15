package dev.nodelll.displaymodeloptimizer.gui;

import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import dev.nodelll.displaymodeloptimizer.ProjectLinks;
import dev.nodelll.displaymodeloptimizer.compat.EntityCullingCompat;
import java.io.IOException;
import java.net.URI;
import java.util.EnumMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DisplayModelOptimizerConfigScreen extends Screen {
    private enum Page {
        GENERAL("general"), ADVANCED("advanced"), ABOUT("about");
        final String key;
        Page(String key) { this.key = "display_model_optimizer.config.page." + key; }
    }

    private final Screen parent;
    private final SettingsDraft draft;
    private final EnumMap<SettingsDraft.NumberOption, EditBox> fields = new EnumMap<>(SettingsDraft.NumberOption.class);
    private Button saveButton;
    private Page page = Page.GENERAL;
    private int left, contentWidth, top;
    private boolean saveError;

    public DisplayModelOptimizerConfigScreen(Screen parent) {
        super(Component.translatable("display_model_optimizer.config.title"));
        this.parent = parent;
        draft = new SettingsDraft(OptimizerSettings.current());
    }

    @Override
    protected void init() {
        fields.clear();
        saveButton = null;
        contentWidth = Math.min(440, width - 32);
        left = (width - contentWidth) / 2;
        top = 68;
        int tabWidth = (contentWidth - 12) / 3;
        for (Page tab : Page.values()) {
            Button button = addRenderableWidget(Button.builder(Component.translatable(tab.key), pressed -> {
                page = tab;
                rebuildWidgets();
            }).bounds(left + tab.ordinal() * (tabWidth + 6), 34, tabWidth, 20).build());
            button.active = page != tab;
        }
        if (page == Page.GENERAL) addGeneralOptions();
        if (page == Page.ADVANCED) {
            addNumber(SettingsDraft.NumberOption.MODEL_LIMIT, 0);
            addRenderableWidget(CycleButton.onOffBuilder(true)
                    .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.cache_geometry.tooltip")))
                    .create(left, top + 22, contentWidth, 20, Component.translatable("display_model_optimizer.option.cache_geometry"),
                            (button, value) -> draft.cacheGeometry(value))).setValue(draft.cacheGeometry());
        }
        if (page == Page.ABOUT) addProjectLinks();
        int buttonWidth = (contentWidth - 12) / 3;
        int footer = height - 28;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("display_model_optimizer.config.save"), button -> save())
                .bounds(left, footer, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + buttonWidth + 6, footer, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("display_model_optimizer.config.reset"), button -> {
                    draft.reset();
                    saveError = false;
                    rebuildWidgets();
                }).bounds(left + 2 * (buttonWidth + 6), footer, buttonWidth, 20).build());
        updateValidation();
    }

    private void addGeneralOptions() {
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.enabled.tooltip")))
                .create(left, top, contentWidth, 20, Component.translatable("display_model_optimizer.option.enabled"),
                        (button, value) -> draft.enabled(value))).setValue(draft.enabled());
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.cache_models.tooltip")))
                .create(left, top + 22, contentWidth, 20, Component.translatable("display_model_optimizer.option.cache_models"),
                        (button, value) -> draft.cacheModels(value))).setValue(draft.cacheModels());
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.offscreen.tooltip")))
                .create(left, top + 44, contentWidth, 20, Component.translatable("display_model_optimizer.option.offscreen"),
                        (button, value) -> draft.offscreen(value))).setValue(draft.offscreen());
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.occlusion.tooltip")))
                .create(left, top + 66, contentWidth, 20, Component.translatable("display_model_optimizer.option.occlusion"),
                        (button, value) -> draft.occlusion(value))).setValue(draft.occlusion());
        addNumber(SettingsDraft.NumberOption.DISTANCE, 4);
    }

    private void addNumber(SettingsDraft.NumberOption option, int row) {
        EditBox box = new EditBox(font, left + contentWidth - 76, top + row * 22 + 1, 76, 18,
                Component.translatable(option.key));
        box.setMaxLength(7);
        box.setValue(draft.text(option));
        box.setTooltip(Tooltip.create(Component.translatable(option.key + ".tooltip")));
        fields.put(option, box);
        box.setResponder(value -> {
            draft.text(option, value);
            saveError = false;
            updateValidation();
        });
        addRenderableWidget(box);
    }

    private void addProjectLinks() {
        int buttonWidth = (contentWidth - 6) / 2;
        addLink("display_model_optimizer.link.project", ProjectLinks.HOME, left, top + 66, buttonWidth);
        addLink("display_model_optimizer.link.issues", ProjectLinks.ISSUES, left + buttonWidth + 6, top + 66, buttonWidth);
        addLink("display_model_optimizer.link.releases", ProjectLinks.RELEASES, left, top + 88, buttonWidth);
        addLink("display_model_optimizer.link.guide", ProjectLinks.GUIDE, left + buttonWidth + 6, top + 88, buttonWidth);
    }

    private void addLink(String key, String url, int x, int y, int buttonWidth) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(key),
                ConfirmLinkScreen.confirmLink(this, URI.create(url))).bounds(x, y, buttonWidth, 20).build());
        button.setTooltip(Tooltip.create(Component.literal(url)));
    }

    private void updateValidation() {
        fields.forEach((option, box) -> box.setTextColor(draft.valid(option) ? 0xffffffff : 0xffff5555));
        if (saveButton != null) saveButton.active = draft.isValid();
    }

    private void save() {
        if (!draft.isValid()) return;
        try {
            OptimizerSettings.save(draft.values());
            onClose();
        } catch (IOException e) {
            saveError = true;
            System.getLogger("Display Model Optimizer").log(System.Logger.Level.WARNING,
                    "[Display Model Optimizer] Cannot save settings", e);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Minecraft's screen wrapper extracts the background before this method.
        // Extracting it again requests a second blur in the same frame.
        graphics.drawCenteredString(font, title, width / 2, 14, 0xffffffff);
        if (page == Page.GENERAL) {
            graphics.drawString(font, Component.translatable(SettingsDraft.NumberOption.DISTANCE.key), left, top + 93, 0xffffffff);
        } else if (page == Page.ADVANCED) {
            int row = 0;
            for (SettingsDraft.NumberOption option : SettingsDraft.NumberOption.values()) {
                if (option == SettingsDraft.NumberOption.MODEL_LIMIT)
                    graphics.drawString(font, Component.translatable(option.key), left, top + row++ * 22 + 5, 0xffffffff);
            }
        } else {
            graphics.drawCenteredString(font, Component.translatable("display_model_optimizer.about.version", version(ProjectLinks.MOD_ID)), width / 2, top, 0xffffffff);
            graphics.drawCenteredString(font, Component.translatable("display_model_optimizer.about.author", "Nodelll"), width / 2, top + 14, 0xffffffff);
            graphics.drawCenteredString(font,
                    Component.translatable(EntityCullingCompat.available() ? "display_model_optimizer.about.ec_present" : "display_model_optimizer.about.ec_absent",
                            version("entityculling")).withStyle(EntityCullingCompat.available() ? ChatFormatting.GREEN : ChatFormatting.GRAY), width / 2, top + 28, 0xffffffff);
            graphics.drawCenteredString(font, Component.translatable("display_model_optimizer.about.modmenu", version("modmenu")), width / 2, top + 42, 0xffffffff);
        }
        graphics.drawCenteredString(font, status(), width / 2, height - 48, 0xffffffff);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component status() {
        Component status = Component.translatable("display_model_optimizer.config.applies");
        if (draft.occlusion() && !EntityCullingCompat.available()) status = Component.translatable("display_model_optimizer.config.ec_missing");
        if (!draft.isValid()) status = Component.translatable("display_model_optimizer.config.invalid").withStyle(ChatFormatting.RED);
        if (OptimizerSettings.isDisabledForSession()) status = Component.translatable("display_model_optimizer.config.session_disabled").withStyle(ChatFormatting.RED);
        if (saveError) status = Component.translatable("display_model_optimizer.config.save_failed").withStyle(ChatFormatting.RED);
        return status;
    }

    private static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("—");
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(title).append(". ").append(Component.translatable(page.key)).append(". ").append(status());
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
