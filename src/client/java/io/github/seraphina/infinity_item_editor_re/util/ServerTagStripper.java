package io.github.seraphina.infinity_item_editor_re.util;

import io.github.seraphina.infinity_item_editor_re.ModSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Experimental: removes server tracking keys so a modified item is treated as a new item. */
public final class ServerTagStripper {
    private ServerTagStripper() {
    }

    public static boolean strip(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        CompoundTag tag = data.copyTag();
        List<String> removed = new ArrayList<>();
        for (String key : new ArrayList<>(tag.keySet())) {
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.startsWith("wlprotocol") || lower.equals("display") || lower.contains("uuid")) {
                tag.remove(key);
                removed.add(key);
            }
        }
        if (removed.isEmpty()) {
            return false;
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        ModSource.LOGGER.info("Infinity Item Editor: stripped server tracking keys {}", removed);
        return true;
    }
}
