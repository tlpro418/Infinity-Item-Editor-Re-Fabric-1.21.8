package io.github.seraphina.infinity_item_editor_re.util;

import io.github.seraphina.infinity_item_editor_re.ModSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Verifies that the server kept the item that was written to a slot. */
public final class SaveVerifier {
    private static String expectedNbt;
    private static ItemStack sentStack;
    private static int slot = -1;
    private static int ticks;

    private SaveVerifier() {
    }

    public static void expect(ItemStack stack, String nbt, int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= 41) {
            return;
        }
        sentStack = stack.copy();
        expectedNbt = nbt;
        slot = inventorySlot;
        ticks = 0;
    }

    public static void tick(Minecraft minecraft) {
        if (slot < 0 || expectedNbt == null) {
            return;
        }
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        ticks++;
        String current;
        try {
            current = ItemStackNbt.save(minecraft.player.getInventory().getItem(slot)).toString();
        } catch (RuntimeException exception) {
            current = "<unreadable>";
        }
        if (current.equals(expectedNbt)) {
            clear();
            return;
        }
        if (ticks < 60) {
            return;
        }

        ModSource.LOGGER.warn("Infinity Item Editor: the server rewrote the saved item. sent={} received={}",
                expectedNbt, current);
        minecraft.player.displayClientMessage(
                Component.translatable("message." + ModSource.MODID + ".editor_server_rewrote"), false);
        String diagnostic = "sent=" + expectedNbt + " | received=" + current;
        minecraft.player.displayClientMessage(
                Component.translatable("message." + ModSource.MODID + ".editor_diagnostic_link")
                        .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent.CopyToClipboard(diagnostic))
                                .withColor(net.minecraft.ChatFormatting.AQUA)),
                false);
        if (sentStack != null && !sentStack.isEmpty()) {
            minecraft.keyboardHandler.setClipboard(GiveHelper.getStringFromItemStack(sentStack));
            minecraft.player.displayClientMessage(
                    Component.translatable("message." + ModSource.MODID + ".editor_give_copied"), false);
        }
        clear();
    }

    private static void clear() {
        expectedNbt = null;
        sentStack = null;
        slot = -1;
        ticks = 0;
    }
}
