package io.github.seraphina.infinity_item_editor_re.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws tooltips immediately.
 * <p>
 * 1.21.6+ defers {@code setTooltipForNextFrame} into a private field of the {@link GuiGraphics} instance.
 * The editor renders through a derived {@link GuiGraphics} (for UI scaling), whose deferred tooltip would
 * never be flushed by the screen, so tooltips are drawn eagerly like the 1.21.4 code did.
 */
public final class Tooltips {
    private Tooltips() {
    }

    public static void show(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> lines = Screen.getTooltipFromItem(minecraft, stack);
        show(graphics, font, lines, stack.getTooltipImage(), x, y);
    }

    public static void show(GuiGraphics graphics, Font font, Component component, int x, int y) {
        show(graphics, font, List.of(component), Optional.empty(), x, y);
    }

    public static void showComponents(GuiGraphics graphics, Font font, List<Component> lines, int x, int y) {
        show(graphics, font, lines, Optional.empty(), x, y);
    }

    private static void show(GuiGraphics graphics, Font font, List<Component> lines,
                             Optional<TooltipComponent> image, int x, int y) {
        if (lines.isEmpty()) {
            return;
        }

        List<ClientTooltipComponent> components = new ArrayList<>(lines.size() + 1);
        image.ifPresent(tooltip -> components.add(ClientTooltipComponent.create(tooltip)));
        for (Component line : lines) {
            components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        graphics.renderTooltip(font, components, x, y, DefaultTooltipPositioner.INSTANCE, null);
    }
}
