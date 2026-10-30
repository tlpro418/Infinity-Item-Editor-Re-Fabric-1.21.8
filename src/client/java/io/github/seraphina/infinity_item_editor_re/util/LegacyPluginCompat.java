package io.github.seraphina.infinity_item_editor_re.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Some servers/plugins keep item display data inside {@code custom_data} (the reported item had
 * keys such as {@code WLProtocol_20_3To1_20_5}, {@code WLProtocol_12_2To1_13Name}). Editing only the
 * 1.21 components leaves those fields stale, so the server restores the old name/lore. This mirrors
 * the edited text back into those fields, preserving their original value format (plain vs JSON).
 */
public final class LegacyPluginCompat {
    private LegacyPluginCompat() {
    }

    public static boolean sync(ItemStack stack, String nameValue, List<String> loreValues) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        if (tag.keySet().stream().noneMatch(key -> key.startsWith("WLProtocol"))) {
            return false;
        }

        String name = LegacyText.stripCodes(nameValue == null ? "" : nameValue).trim();
        ListTag plainLore = new ListTag();
        for (String line : loreValues) {
            if (line != null && !line.isEmpty()) {
                plainLore.add(StringTag.valueOf(line));
            }
        }

        CompoundTag display = tag.contains("display")
                ? tag.getCompoundOrEmpty("display").copy()
                : new CompoundTag();
        if (name.isEmpty()) {
            display.remove("Name");
        } else if (isJsonString(display.get("Name"))) {
            display.putString("Name", ComponentCompat.toJson(LegacyText.parse(nameValue)));
        } else {
            display.putString("Name", name);
        }

        boolean jsonLore = isJsonList(display.get("Lore"));
        if (plainLore.isEmpty()) {
            display.remove("Lore");
        } else {
            display.put("Lore", buildLore(loreValues, jsonLore));
        }
        tag.put("display", display);

        for (String key : new ArrayList<>(tag.keySet())) {
            if (key.endsWith("Name") && tag.get(key) instanceof StringTag nameTag) {
                tag.putString(key, isJsonString(nameTag)
                        ? ComponentCompat.toJson(LegacyText.parse(nameValue))
                        : name);
            } else if (key.endsWith("Lore") && tag.get(key) instanceof ListTag loreTag) {
                tag.put(key, buildLore(loreValues, isJsonList(loreTag)));
            }
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    private static ListTag buildLore(List<String> lines, boolean json) {
        ListTag list = new ListTag();
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            list.add(StringTag.valueOf(json ? ComponentCompat.toJson(LegacyText.parse(line)) : line));
        }
        return list;
    }

    private static boolean isJsonString(net.minecraft.nbt.Tag tag) {
        return tag instanceof StringTag stringTag
                && (stringTag.value().startsWith("{") || stringTag.value().startsWith("\""));
    }

    private static boolean isJsonList(net.minecraft.nbt.Tag tag) {
        return tag instanceof ListTag listTag && !listTag.isEmpty()
                && isJsonString(listTag.get(0));
    }
}
