package io.github.seraphina.infinity_item_editor_re.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/**
 * Converts legacy section-sign formatting codes to real component styles and back.
 * Minecraft 1.21.5+ no longer applies color codes stored in a plain component string.
 */
public final class LegacyText {
    private static final char SECTION = '\u00a7';

    private LegacyText() {
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent root = null;
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == SECTION && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(text.charAt(i + 1));
                if (format != null) {
                    root = append(root, buffer, style);
                    style = style.applyLegacyFormat(format);
                    i++;
                    continue;
                }
            }
            buffer.append(current);
        }
        root = append(root, buffer, style);
        return root == null ? Component.empty() : root;
    }

    /** Returns the legacy-code string, or {@code null} when the component is too complex to round-trip. */
    public static String toLegacy(Component component) {
        if (component == null) {
            return null;
        }

        StringBuilder out = new StringBuilder();
        boolean[] complex = {false};
        component.visit((style, text) -> {
            if (complex[0]) {
                return Optional.empty();
            }
            if (style.getClickEvent() != null || style.getHoverEvent() != null
                    || style.getInsertion() != null || (style.getFont() != null && !Style.DEFAULT_FONT.equals(style.getFont()))) {
                complex[0] = true;
                return Optional.empty();
            }
            if (!appendStyleCodes(out, style)) {
                complex[0] = true;
                return Optional.empty();
            }
            out.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return complex[0] ? null : out.toString();
    }

    public static String toLegacyOrPlain(Component component) {
        String legacy = toLegacy(component);
        return legacy == null ? component.getString() : legacy;
    }

    /** Unwraps a quoted JSON string such as {@code "\"\u00a76text\""} produced by older serializers. */
    public static String unwrapJsonString(String text) {
        if (text == null || text.length() < 2 || !text.startsWith("\"") || !text.endsWith("\"")) {
            return text;
        }
        try {
            return com.google.gson.JsonParser.parseString(text).getAsString();
        } catch (RuntimeException exception) {
            return text;
        }
    }

    public static String stripCodes(String text) {
        if (text == null || text.indexOf(SECTION) < 0) {
            return text == null ? "" : text;
        }

        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == SECTION && i + 1 < text.length() && ChatFormatting.getByCode(text.charAt(i + 1)) != null) {
                i++;
                continue;
            }
            out.append(current);
        }
        return out.toString();
    }

    private static MutableComponent append(MutableComponent root, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) {
            return root;
        }
        MutableComponent part = Component.literal(buffer.toString()).withStyle(style);
        buffer.setLength(0);
        return root == null ? part : root.append(part);
    }

    private static boolean appendStyleCodes(StringBuilder out, Style style) {
        if (style.getColor() != null) {
            ChatFormatting color = legacyColor(style.getColor());
            if (color == null) {
                return false;
            }
            out.append(SECTION).append(color.getChar());
        }
        if (style.isBold()) {
            out.append(SECTION).append(ChatFormatting.BOLD.getChar());
        }
        if (style.isItalic()) {
            out.append(SECTION).append(ChatFormatting.ITALIC.getChar());
        }
        if (style.isUnderlined()) {
            out.append(SECTION).append(ChatFormatting.UNDERLINE.getChar());
        }
        if (style.isStrikethrough()) {
            out.append(SECTION).append(ChatFormatting.STRIKETHROUGH.getChar());
        }
        if (style.isObfuscated()) {
            out.append(SECTION).append(ChatFormatting.OBFUSCATED.getChar());
        }
        return true;
    }

    private static ChatFormatting legacyColor(TextColor color) {
        for (ChatFormatting format : ChatFormatting.values()) {
            if (format.isColor() && TextColor.fromLegacyFormat(format).equals(color)) {
                return format;
            }
        }
        return null;
    }
}


