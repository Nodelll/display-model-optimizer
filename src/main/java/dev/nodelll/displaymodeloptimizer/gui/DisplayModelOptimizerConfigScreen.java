package dev.nodelll.displaymodeloptimizer.gui;

import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import dev.nodelll.displaymodeloptimizer.compat.EntityCullingCompat;
import java.io.IOException;
import java.util.EnumMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DisplayModelOptimizerConfigScreen extends Screen {
    private final Screen parent;
    private final SettingsDraft draft;
    private final EnumMap<SettingsDraft.NumberOption, EditBox> fields = new EnumMap<>(SettingsDraft.NumberOption.class);
    private Button saveButton;
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
        contentWidth = Math.min(440, width - 32);
        left = (width - contentWidth) / 2;
        top = Math.max(28, (height - 204) / 2);
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.enabled.tooltip")))
                .create(left, top, contentWidth, 20, Component.translatable("display_model_optimizer.option.enabled"),
                        (button, value) -> draft.enabled(value))).setValue(draft.enabled());
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.cache_models.tooltip")))
                .create(left, top + 22, contentWidth, 20, Component.translatable("display_model_optimizer.option.cache_models"),
                        (button, value) -> draft.cacheModels(value))).setValue(draft.cacheModels());
        addRenderableWidget(CycleButton.onOffBuilder(true)
                .withTooltip(value -> Tooltip.create(Component.translatable("display_model_optimizer.option.occlusion.tooltip")))
                .create(left, top + 44, contentWidth, 20, Component.translatable("display_model_optimizer.option.occlusion"),
                        (button, value) -> draft.occlusion(value))).setValue(draft.occlusion());
        int row = 3;
        for (SettingsDraft.NumberOption option : SettingsDraft.NumberOption.values()) {
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
            row++;
        }
        int buttonWidth = (contentWidth - 12) / 3;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("display_model_optimizer.config.save"), button -> save())
                .bounds(left, top + 177, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + buttonWidth + 6, top + 177, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("display_model_optimizer.config.reset"), button -> {
                    draft.reset();
                    saveError = false;
                    rebuildWidgets();
                }).bounds(left + 2 * (buttonWidth + 6), top + 177, buttonWidth, 20).build());
        updateValidation();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        var text = graphics.textRenderer();
        text.accept(TextAlignment.CENTER, width / 2, top - 23, title);
        int row = 3;
        for (SettingsDraft.NumberOption option : SettingsDraft.NumberOption.values()) {
            text.accept(TextAlignment.LEFT, left, top + row * 22 + 5, Component.translatable(option.key));
            row++;
        }
        Component status = Component.translatable("display_model_optimizer.config.applies");
        if (draft.occlusion() && !EntityCullingCompat.available()) status = Component.translatable("display_model_optimizer.config.ec_missing");
        if (!draft.isValid()) status = Component.translatable("display_model_optimizer.config.invalid").withStyle(ChatFormatting.RED);
        if (OptimizerSettings.isDisabledForSession()) status = Component.translatable("display_model_optimizer.config.session_disabled").withStyle(ChatFormatting.RED);
        if (saveError) status = Component.translatable("display_model_optimizer.config.save_failed").withStyle(ChatFormatting.RED);
        text.accept(TextAlignment.CENTER, width / 2, top + 155, status);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
