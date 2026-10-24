package io.github.seraphina.infinity_item_editor_re.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public final class ComponentCompat {
    private ComponentCompat() {
    }

    public static String toJson(Component component) {
        return ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, component)
                .getOrThrow()
                .toString();
    }

    public static Component fromJson(String json) {
        return fromJson(JsonParser.parseString(json));
    }

    public static Component fromJson(JsonElement json) {
        return ComponentSerialization.CODEC
                .parse(JsonOps.INSTANCE, json)
                .result()
                .orElse(Component.empty());
    }

    public static Component fromJsonLenient(String json) {
        return fromJson(json);
    }
}
