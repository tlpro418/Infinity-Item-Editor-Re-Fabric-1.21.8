package io.github.seraphina.infinity_item_editor_re.util;

import io.github.seraphina.infinity_item_editor_re.ModSource;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes components that cannot be encoded into the creative-mode slot packet.
 * Only components that fail on their own are dropped, so name/lore survive.
 */
public final class NetworkSafeStacks {
    private NetworkSafeStacks() {
    }

    public static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty() || encodable(stack)) {
            return stack;
        }

        ItemStack copy = stack.copy();
        List<DataComponentType<?>> broken = new ArrayList<>();
        for (DataComponentType<?> type : new ArrayList<>(copy.getComponents().keySet())) {
            ItemStack single = new ItemStack(copy.getItem(), 1);
            setRaw(single, type, copy.get(type));
            if (!encodable(single)) {
                broken.add(type);
            }
        }

        if (!broken.isEmpty()) {
            for (DataComponentType<?> type : broken) {
                if (type == DataComponents.CUSTOM_DATA) {
                    preserveLegacyDisplay(copy);
                }
                copy.remove(type);
            }
            if (encodable(copy)) {
                ModSource.LOGGER.warn("Infinity Item Editor: dropped components that cannot be sent to the server: {}", names(broken));
                return copy;
            }
        }

        // Fallback: drop components until the stack encodes, but never return empty unless unavoidable.
        for (DataComponentType<?> type : new ArrayList<>(copy.getComponents().keySet())) {
            copy.remove(type);
            if (encodable(copy)) {
                ModSource.LOGGER.warn("Infinity Item Editor: dropped components until encodable: {}", type);
                return copy;
            }
        }
        ModSource.LOGGER.warn("Infinity Item Editor: item cannot be encoded even without components; dropping it");
        return ItemStack.EMPTY;
    }

    private static void preserveLegacyDisplay(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains("display")) {
            return;
        }
        CompoundTag display = tag.getCompoundOrEmpty("display");
        if (display.contains("Name") && !stack.has(DataComponents.CUSTOM_NAME)) {
            String name = display.getStringOr("Name", "");
            if (!name.isBlank()) {
                stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name));
            }
        }
        if (display.contains("Lore") && !stack.has(DataComponents.LORE)) {
            ListTag lore = display.getListOrEmpty("Lore");
            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < lore.size(); i++) {
                lines.add(LegacyText.parse(lore.getStringOr(i, "")));
            }
            if (!lines.isEmpty()) {
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
        }
    }

    private static String names(List<DataComponentType<?>> types) {
        List<String> names = new ArrayList<>(types.size());
        for (DataComponentType<?> type : types) {
            names.add(type.toString());
        }
        return names.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setRaw(ItemStack stack, DataComponentType type, Object value) {
        stack.set(type, value);
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
