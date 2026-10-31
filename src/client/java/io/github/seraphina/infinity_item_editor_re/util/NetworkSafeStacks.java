package io.github.seraphina.infinity_item_editor_re.util;

import io.github.seraphina.infinity_item_editor_re.ModSource;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Removes components that cannot be encoded into the creative-mode slot packet (prevents EncoderException). */
public final class NetworkSafeStacks {
    private NetworkSafeStacks() {
    }

    public static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty() || encodable(stack)) {
            return stack;
        }

        ItemStack copy = stack.copy();
        List<String> removed = new ArrayList<>();
        for (DataComponentType<?> type : new ArrayList<>(copy.getComponents().keySet())) {
            copy.remove(type);
            removed.add(type.toString());
            if (encodable(copy)) {
                ModSource.LOGGER.warn("Infinity Item Editor: dropped components that cannot be sent to the server: {}", removed);
                return copy;
            }
            if (copy.getComponents().isEmpty()) {
                break;
            }
        }

        ModSource.LOGGER.warn("Infinity Item Editor: item still cannot be encoded after dropping {}", removed);
        return ItemStack.EMPTY;
    }

    private static boolean encodable(ItemStack stack) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), ItemStackNbt.registryAccess());
        try {
            ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.encode(buffer,
                    new ServerboundSetCreativeModeSlotPacket(36, stack));
            return true;
        } catch (Throwable throwable) {
            return false;
        } finally {
            try {
                buffer.release();
            } catch (Throwable ignored) {
            }
        }
    }
}
