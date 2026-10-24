package io.github.seraphina.infinity_item_editor_re.util;
import io.github.seraphina.infinity_item_editor_re.util.NbtCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ItemStackNbt {
    private static final Set<String> FULL_STACK_KEYS = Set.of("id", "item", "Item", "count", "Count", "components", "tag", "nbt");
    private static final Map<String, String> BANNER_HASH_TO_PATTERN = Map.ofEntries(
            Map.entry("bl", "square_bottom_left"),
            Map.entry("br", "square_bottom_right"),
            Map.entry("tl", "square_top_left"),
            Map.entry("tr", "square_top_right"),
            Map.entry("bs", "stripe_bottom"),
            Map.entry("ts", "stripe_top"),
            Map.entry("ls", "stripe_left"),
            Map.entry("rs", "stripe_right"),
            Map.entry("cs", "stripe_center"),
            Map.entry("ms", "stripe_middle"),
            Map.entry("drs", "stripe_downright"),
            Map.entry("dls", "stripe_downleft"),
            Map.entry("ss", "small_stripes"),
            Map.entry("cr", "cross"),
            Map.entry("sc", "straight_cross"),
            Map.entry("bt", "triangle_bottom"),
            Map.entry("tt", "triangle_top"),
            Map.entry("bts", "triangles_bottom"),
            Map.entry("tts", "triangles_top"),
            Map.entry("ld", "diagonal_left"),
            Map.entry("rd", "diagonal_up_right"),
            Map.entry("lud", "diagonal_up_left"),
            Map.entry("rud", "diagonal_right"),
            Map.entry("mc", "circle"),
            Map.entry("mr", "rhombus"),
            Map.entry("vh", "half_vertical"),
            Map.entry("hh", "half_horizontal"),
            Map.entry("vhr", "half_vertical_right"),
            Map.entry("hhb", "half_horizontal_bottom"),
            Map.entry("bo", "border"),
            Map.entry("cbo", "curly_border"),
            Map.entry("gra", "gradient"),
            Map.entry("gru", "gradient_up"),
            Map.entry("bri", "bricks"),
            Map.entry("glb", "globe"),
            Map.entry("cre", "creeper"),
            Map.entry("sku", "skull"),
            Map.entry("flo", "flower"),
            Map.entry("moj", "mojang"),
            Map.entry("pig", "piglin")
    );
    private static final Map<String, String> BANNER_PATTERN_TO_HASH = invert(BANNER_HASH_TO_PATTERN);
    private static final Map<String, String> BLOCK_ENTITY_BY_ITEM = Map.ofEntries(
            Map.entry("barrel", "barrel"),
            Map.entry("beacon", "beacon"),
            Map.entry("beehive", "beehive"),
            Map.entry("bee_nest", "beehive"),
            Map.entry("blast_furnace", "blast_furnace"),
            Map.entry("brewing_stand", "brewing_stand"),
            Map.entry("campfire", "campfire"),
            Map.entry("soul_campfire", "campfire"),
            Map.entry("chest", "chest"),
            Map.entry("trapped_chest", "chest"),
            Map.entry("command_block", "command_block"),
            Map.entry("chain_command_block", "command_block"),
            Map.entry("repeating_command_block", "command_block"),
            Map.entry("comparator", "comparator"),
            Map.entry("conduit", "conduit"),
            Map.entry("crafter", "crafter"),
            Map.entry("decorated_pot", "decorated_pot"),
            Map.entry("dispenser", "dispenser"),
            Map.entry("dropper", "dropper"),
            Map.entry("furnace", "furnace"),
            Map.entry("hopper", "hopper"),
            Map.entry("jigsaw", "jigsaw"),
            Map.entry("jukebox", "jukebox"),
            Map.entry("lectern", "lectern"),
            Map.entry("smoker", "smoker"),
            Map.entry("spawner", "mob_spawner"),
            Map.entry("structure_block", "structure_block"),
            Map.entry("suspicious_gravel", "brushable_block"),
            Map.entry("suspicious_sand", "brushable_block"),
            Map.entry("trial_spawner", "trial_spawner"),
            Map.entry("vault", "vault")
    );

    private ItemStackNbt() {
    }

    public static CompoundTag get(ItemStack stack) {
        CompoundTag tag = buildLegacyTag(stack);
        return tag == null || tag.isEmpty() ? null : syncedRoot(stack, tag);
    }

    public static CompoundTag getOrCreate(ItemStack stack) {
        CompoundTag tag = buildLegacyTag(stack);
        return syncedRoot(stack, tag == null ? new CompoundTag() : tag);
    }

    public static CompoundTag getElement(ItemStack stack, String key) {
        CompoundTag tag = get(stack);
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        return tag.getCompoundOrEmpty(key);
    }

    public static CompoundTag getOrCreateElement(ItemStack stack, String key) {
        CompoundTag tag = getOrCreate(stack);
        if (!tag.contains(key)) {
            tag.put(key, new CompoundTag());
        }
        return tag.getCompoundOrEmpty(key);
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        applyLegacyTag(stack, tag == null ? new CompoundTag() : tag);
    }

    public static CompoundTag save(ItemStack stack) {
        Tag saved = ItemStack.OPTIONAL_CODEC
                .encodeStart(provider().createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
        return saved instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }

    private static ItemStack parseOptionalStack(CompoundTag tag) {
        return parse(provider(), tag);
    }

    public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag) {
        var result = ItemStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag);
        if (result.error().isPresent() && Boolean.getBoolean("infinity_item_editor_re.debug")) {
            System.err.println("[infinity_item_editor_re] stack parse failed: " + result.error().get().message() + " input=" + tag);
        }
        return result.result().orElse(ItemStack.EMPTY);
    }

    public static ItemStack parse(CompoundTag tag) {
        ItemStack parsed = parseOptionalStack(normalizeStackTag(tag));
        if (!parsed.isEmpty() || !isLegacyStackTag(tag)) {
            return parsed;
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getStringOr("id", ""));
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        int count = tag.contains("Count") ? tag.getByteOr("Count", (byte) 0) : 1;
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        if (tag.contains("tag")) {
            set(stack, tag.getCompoundOrEmpty("tag").copy());
        }
        return stack;
    }

    public static ItemStack parseEditorNbt(ItemStack current, CompoundTag tag) {
        if (isFullStackTag(tag)) {
            ItemStack parsed = parse(tag);
            if (!parsed.isEmpty() || "minecraft:air".equals(tag.getStringOr("id", ""))) {
                return parsed;
            }
        }

        ItemStack edited = current.copy();
        set(edited, tag);
        return edited;
    }

    public static HolderLookup.Provider provider() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                return minecraft.level.registryAccess();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static RegistryAccess registryAccess() {
        HolderLookup.Provider provider = provider();
        return provider instanceof RegistryAccess registryAccess ? registryAccess : RegistryAccess.EMPTY;
    }

    private static CompoundTag syncedRoot(ItemStack stack, CompoundTag tag) {
        final CompoundTag[] holder = new CompoundTag[1];
        holder[0] = syncedCompound(tag, () -> applyLegacyTag(stack, holder[0]));
        return holder[0];
    }

    private static CompoundTag buildLegacyTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        CompoundTag saved = save(stack);
        CompoundTag components = saved.getCompoundOrEmpty("components");
        CompoundTag tag = new CompoundTag();
        int hideFlags = 0;

        if (components.contains("minecraft:custom_data")) {
            tag.merge(components.getCompoundOrEmpty("minecraft:custom_data").copy());
        }

        copyComponent(components, "minecraft:damage", tag, "Damage");
        copyComponent(components, "minecraft:repair_cost", tag, "RepairCost");
        copyComponent(components, "minecraft:custom_model_data", tag, "CustomModelData");
        copyComponent(components, "minecraft:block_state", tag, "BlockStateTag");
        copyComponent(components, "minecraft:entity_data", tag, "EntityTag");
        copyComponent(components, "minecraft:trim", tag, "Trim");

        hideFlags = readComponentDisplay(components, tag, hideFlags);
        hideFlags = readComponentEnchantments(components, tag, hideFlags);
        hideFlags = readComponentUnbreakable(components, tag, hideFlags);
        hideFlags = readComponentAttributes(components, tag, hideFlags);
        hideFlags = readComponentAdventure(components, tag, hideFlags);
        readComponentPotion(components, tag);
        readComponentBooks(components, tag);
        readComponentFireworks(components, tag);
        readComponentProfile(components, tag);
        readComponentBlockEntity(stack, components, tag);

        if (isTooltipComponentHidden(components, "minecraft:trim")) {
            hideFlags |= 128;
        }
        if (isTooltipComponentHidden(components, "minecraft:potion_contents")) {
            hideFlags |= 32;
        }
        if (hideFlags != 0) {
            tag.putInt("HideFlags", hideFlags);
        }

        return tag;
    }

    private static void applyLegacyTag(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (tag == null || tag.isEmpty()) {
            applyLegacyComponentsFrom(stack, new ItemStack(stack.getItem(), stack.getCount()));
            return;
        }

        CompoundTag stackTag = stackTagFromLegacy(stack, tag);
        ItemStack parsed = parseOptionalStack(stackTag);
        if (parsed.isEmpty() && !stack.is(Items.AIR)) {
            applyParseableComponents(stack, stackTag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
            return;
        }

        applyLegacyComponentsFrom(stack, parsed);
    }

    private static void applyParseableComponents(ItemStack stack, CompoundTag stackTag) {
        Tag componentsTag = stackTag.get("components");
        if (!(componentsTag instanceof CompoundTag components) || components.isEmpty()) {
            return;
        }

        CompoundTag good = new CompoundTag();
        for (String key : components.keySet()) {
            CompoundTag single = new CompoundTag();
            single.putString("id", stackTag.getStringOr("id", "minecraft:air"));
            single.putInt("count", Math.max(1, stackTag.getIntOr("count", 1)));
            CompoundTag singleComponents = new CompoundTag();
            singleComponents.put(key, components.get(key));
            single.put("components", singleComponents);
            if (!parseOptionalStack(single).isEmpty()) {
                good.put(key, components.get(key));
            }
        }
        if (good.isEmpty()) {
            return;
        }

        CompoundTag goodStack = new CompoundTag();
        goodStack.putString("id", stackTag.getStringOr("id", "minecraft:air"));
        goodStack.putInt("count", Math.max(1, stackTag.getIntOr("count", 1)));
        goodStack.put("components", good);
        ItemStack parsed = parseOptionalStack(goodStack);
        if (!parsed.isEmpty()) {
            applyLegacyComponentsFrom(stack, parsed);
        }
    }

    private static void applyLegacyComponentsFrom(ItemStack stack, ItemStack source) {
        copyDataComponent(source, stack, DataComponents.CUSTOM_DATA);
        copyDataComponent(source, stack, DataComponents.DAMAGE);
        copyDataComponent(source, stack, DataComponents.REPAIR_COST);
        copyDataComponent(source, stack, DataComponents.CUSTOM_MODEL_DATA);
        copyDataComponent(source, stack, DataComponents.ENTITY_DATA);
        copyDataComponent(source, stack, DataComponents.BLOCK_ENTITY_DATA);
        copyDataComponent(source, stack, DataComponents.UNBREAKABLE);
        copyDataComponent(source, stack, DataComponents.ENCHANTMENTS);
        copyDataComponent(source, stack, DataComponents.STORED_ENCHANTMENTS);
        copyDataComponent(source, stack, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        copyDataComponent(source, stack, DataComponents.CUSTOM_NAME);
        copyDataComponent(source, stack, DataComponents.ITEM_NAME);
        copyDataComponent(source, stack, DataComponents.LORE);
        copyDataComponent(source, stack, DataComponents.DYED_COLOR);
        copyDataComponent(source, stack, DataComponents.MAP_COLOR);
        copyDataComponent(source, stack, DataComponents.CAN_BREAK);
        copyDataComponent(source, stack, DataComponents.CAN_PLACE_ON);
        copyDataComponent(source, stack, DataComponents.ATTRIBUTE_MODIFIERS);
        copyDataComponent(source, stack, DataComponents.TRIM);
        copyDataComponent(source, stack, DataComponents.TOOLTIP_DISPLAY);
        copyDataComponent(source, stack, DataComponents.CHARGED_PROJECTILES);
        copyDataComponent(source, stack, DataComponents.BUNDLE_CONTENTS);
        copyDataComponent(source, stack, DataComponents.MAP_ID);
        copyDataComponent(source, stack, DataComponents.MAP_DECORATIONS);
        copyDataComponent(source, stack, DataComponents.POTION_CONTENTS);
        copyDataComponent(source, stack, DataComponents.WRITABLE_BOOK_CONTENT);
        copyDataComponent(source, stack, DataComponents.WRITTEN_BOOK_CONTENT);
        copyDataComponent(source, stack, DataComponents.SUSPICIOUS_STEW_EFFECTS);
        copyDataComponent(source, stack, DataComponents.DEBUG_STICK_STATE);
        copyDataComponent(source, stack, DataComponents.BUCKET_ENTITY_DATA);
        copyDataComponent(source, stack, DataComponents.INSTRUMENT);
        copyDataComponent(source, stack, DataComponents.RECIPES);
        copyDataComponent(source, stack, DataComponents.LODESTONE_TRACKER);
        copyDataComponent(source, stack, DataComponents.FIREWORKS);
        copyDataComponent(source, stack, DataComponents.FIREWORK_EXPLOSION);
        copyDataComponent(source, stack, DataComponents.PROFILE);
        copyDataComponent(source, stack, DataComponents.NOTE_BLOCK_SOUND);
        copyDataComponent(source, stack, DataComponents.BANNER_PATTERNS);
        copyDataComponent(source, stack, DataComponents.BASE_COLOR);
        copyDataComponent(source, stack, DataComponents.CONTAINER);
        copyDataComponent(source, stack, DataComponents.BEES);
        copyDataComponent(source, stack, DataComponents.LOCK);
        copyDataComponent(source, stack, DataComponents.CONTAINER_LOOT);
        copyDataComponent(source, stack, DataComponents.POT_DECORATIONS);
    }

    private static <T> void copyDataComponent(ItemStack source, ItemStack target, DataComponentType<T> component) {
        target.set(component, source.get(component));
    }

    private static CompoundTag stackTagFromLegacy(ItemStack stack, CompoundTag legacyTag) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stackTagFromLegacy(itemId == null ? "minecraft:air" : itemId.toString(), Math.max(1, stack.getCount()), legacyTag);
    }

    private static CompoundTag stackTagFromLegacy(String itemId, int count, CompoundTag legacyTag) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", Math.max(1, count));

        CompoundTag customData = legacyTag.copy();
        CompoundTag components = new CompoundTag();
        int hideFlags = removeInt(customData, "HideFlags");

        moveComponent(customData, "Damage", components, "minecraft:damage");
        moveComponent(customData, "RepairCost", components, "minecraft:repair_cost");
        writeCustomModelData(customData, components);
        moveComponent(customData, "BlockStateTag", components, "minecraft:block_state");
        writeEntityDataComponent(itemId, customData, components);
        writeTrimComponent(customData, components, hideFlags);

        writeDisplayComponents(customData, components, hideFlags);
        writeEnchantmentsComponent(customData, components, "Enchantments", "minecraft:enchantments", (hideFlags & 1) != 0);
        writeEnchantmentsComponent(customData, components, "StoredEnchantments", "minecraft:stored_enchantments", (hideFlags & 32) != 0);
        writeUnbreakableComponent(customData, components, hideFlags);
        writeAttributeComponents(customData, components, hideFlags);
        writeAdventureComponents(customData, components, hideFlags);
        writePotionComponents(customData, components);
        writeBookComponents(customData, components);
        writeFireworkComponents(customData, components);
        writeProfileComponent(customData, components);
        writeBlockEntityComponents(itemId, customData, components);

        if ((hideFlags & 32) != 0) {
            hideTooltipComponent(components, "minecraft:potion_contents");
        }
        if (!customData.isEmpty()) {
            components.put("minecraft:custom_data", customData);
        }
        if (!components.isEmpty()) {
            stackTag.put("components", components);
        }
        return stackTag;
    }

    private static CompoundTag normalizeStackTag(CompoundTag tag) {
        if (!isFullStackTag(tag)) {
            return tag;
        }

        CompoundTag normalized = tag.copy();
        if (!normalized.contains("id")) {
            if (normalized.contains("item")) {
                normalized.putString("id", normalized.getStringOr("item", ""));
            } else if (normalized.contains("Item")) {
                normalized.putString("id", normalized.getStringOr("Item", ""));
            }
        }
        if (!normalized.contains("count") && normalized.contains("Count")) {
            normalized.putInt("count", normalized.getByteOr("Count", (byte) 0) & 255);
        }
        if (!normalized.contains("count")) {
            normalized.putInt("count", 1);
        }
        if (!normalized.contains("components")) {
            CompoundTag legacy = normalized.contains("tag")
                    ? normalized.getCompoundOrEmpty("tag")
                    : normalized.contains("nbt") ? normalized.getCompoundOrEmpty("nbt") : new CompoundTag();
            normalized = stackTagFromLegacy(normalized.getStringOr("id", ""), normalized.getIntOr("count", 0), legacy);
        }
        return normalized;
    }

    private static boolean isFullStackTag(CompoundTag tag) {
        for (String key : tag.keySet()) {
            if (FULL_STACK_KEYS.contains(key)) {
                return tag.contains("id") || tag.contains("item") || tag.contains("Item");
            }
        }
        return false;
    }

    private static boolean isLegacyStackTag(CompoundTag tag) {
        return tag.contains("id") && (tag.contains("Count") || tag.contains("tag"));
    }


    private static void hideTooltipComponent(CompoundTag components, String hiddenComponent) {
        CompoundTag tooltipDisplay = components.contains("minecraft:tooltip_display")
                ? components.getCompoundOrEmpty("minecraft:tooltip_display").copy()
                : new CompoundTag();
        ListTag hiddenComponents = tooltipDisplay.contains("hidden_components")
                ? tooltipDisplay.getListOrEmpty("hidden_components").copy()
                : new ListTag();
        if (!listContainsString(hiddenComponents, hiddenComponent)) {
            hiddenComponents.add(StringTag.valueOf(hiddenComponent));
        }
        tooltipDisplay.put("hidden_components", hiddenComponents);
        components.put("minecraft:tooltip_display", tooltipDisplay);
    }

    private static boolean isTooltipComponentHidden(CompoundTag components, String componentKey) {
        if (!components.contains("minecraft:tooltip_display")) {
            return false;
        }
        CompoundTag tooltipDisplay = components.getCompoundOrEmpty("minecraft:tooltip_display");
        return tooltipDisplay.getBooleanOr("hide_tooltip", false)
                || listContainsString(tooltipDisplay.getListOrEmpty("hidden_components"), componentKey);
    }

    private static boolean listContainsString(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getStringOr(i, ""))) {
                return true;
            }
        }
        return false;
    }

    private static void writeDisplayComponents(CompoundTag customData, CompoundTag components, int hideFlags) {
        if (!customData.contains("display")) {
            return;
        }

        CompoundTag display = customData.getCompoundOrEmpty("display").copy();
        writeTextComponent(display, "Name", components, "minecraft:custom_name");
        writeLoreComponent(display, components);
        if (display.contains("LocName")) {
            CompoundTag itemName = new CompoundTag();
            itemName.putString("translate", display.getStringOr("LocName", ""));
            components.put("minecraft:item_name", itemName);
            display.remove("LocName");
        }
        if (display.contains("color")) {
            components.putInt("minecraft:dyed_color", display.getIntOr("color", 0));
            if ((hideFlags & 64) != 0) {
                hideTooltipComponent(components, "minecraft:dyed_color");
            }
            display.remove("color");
        } else if ((hideFlags & 64) != 0) {
            components.putInt("minecraft:dyed_color", 10511680);
            hideTooltipComponent(components, "minecraft:dyed_color");
        }
        moveComponent(display, "MapColor", components, "minecraft:map_color");

        if (display.isEmpty()) {
            customData.remove("display");
        } else {
            customData.put("display", display);
        }
    }

    private static void writeCustomModelData(CompoundTag customData, CompoundTag components) {
        Tag value = customData.get("CustomModelData");
        if (value == null) {
            return;
        }
        if (value instanceof NumericTag numericTag) {
            CompoundTag modern = new CompoundTag();
            ListTag floats = new ListTag();
            floats.add(FloatTag.valueOf(numericTag.floatValue()));
            modern.put("floats", floats);
            components.put("minecraft:custom_model_data", modern);
        } else if (value instanceof CompoundTag compoundTag) {
            components.put("minecraft:custom_model_data", compoundTag.copy());
        }
        customData.remove("CustomModelData");
    }

    private static void writeTextComponent(CompoundTag display, String key, CompoundTag components, String componentKey) {
        Tag value = display.get(key);
        if (value == null) {
            return;
        }
        Tag component = legacyTextToComponentTag(value);
        if (component != null) {
            components.put(componentKey, component);
        }
        display.remove(key);
    }

    private static void writeLoreComponent(CompoundTag display, CompoundTag components) {
        Tag value = display.get("Lore");
        if (value == null) {
            return;
        }

        ListTag lines = new ListTag();
        if (value instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Tag component = legacyTextToComponentTag(list.get(i));
                if (component != null) {
                    lines.add(component);
                }
            }
        }
        if (!lines.isEmpty()) {
            components.put("minecraft:lore", lines);
        }
        display.remove("Lore");
    }

    private static Tag legacyTextToComponentTag(Tag value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringTag stringTag)) {
            return value.copy();
        }

        String raw = LegacyText.unwrapJsonString(stringTag.value());
        Component component = null;
        if (raw.startsWith("{") || raw.startsWith("[")) {
            try {
                component = ComponentCompat.fromJson(raw);
            } catch (RuntimeException ignored) {
                component = null;
            }
        }
        if (component == null) {
            component = LegacyText.parse(raw);
        }
        return ComponentSerialization.CODEC
                .encodeStart(provider().createSerializationContext(NbtOps.INSTANCE), component)
                .result()
                .orElseGet(() -> StringTag.valueOf(raw));
    }

    private static void copyTextComponent(CompoundTag components, String componentKey, CompoundTag display, String key) {
        Tag value = components.get(componentKey);
        if (value == null) {
            return;
        }
        Component component = parseComponentTag(value);
        String legacy = component == null ? null : LegacyText.toLegacy(component);
        if (legacy != null) {
            display.putString(key, legacy);
        } else {
            display.put(key, value.copy());
        }
    }

    private static void copyLoreComponent(CompoundTag components, CompoundTag display) {
        Tag value = components.get("minecraft:lore");
        if (!(value instanceof ListTag list)) {
            return;
        }
        ListTag lines = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            Tag entry = list.get(i);
            Component component = parseComponentTag(entry);
            String legacy = component == null ? null : LegacyText.toLegacy(component);
            lines.add(legacy != null ? StringTag.valueOf(legacy) : entry.copy());
        }
        display.put("Lore", lines);
    }

    private static Component parseComponentTag(Tag tag) {
        try {
            return ComponentSerialization.CODEC
                    .parse(provider().createSerializationContext(NbtOps.INSTANCE), tag)
                    .result()
                    .orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void writeEnchantmentsComponent(CompoundTag customData, CompoundTag components, String oldKey, String componentKey, boolean hidden) {
        if (!customData.contains(oldKey) && !hidden) {
            return;
        }

        ListTag oldEnchantments = customData.getListOrEmpty(oldKey);
        CompoundTag levels = new CompoundTag();
        for (int i = 0; i < oldEnchantments.size(); i++) {
            CompoundTag enchantment = oldEnchantments.getCompoundOrEmpty(i);
            if (enchantment.contains("id")) {
                levels.putInt(enchantment.getStringOr("id", ""), Math.max(0, enchantment.getIntOr("lvl", 0)));
            }
        }

        if (!levels.isEmpty()) {
            components.put(componentKey, levels);
        }
        if (hidden) {
            hideTooltipComponent(components, componentKey);
            if (levels.isEmpty()) {
                components.putBoolean("minecraft:enchantment_glint_override", true);
            }
        }
        customData.remove(oldKey);
    }

    private static void writeUnbreakableComponent(CompoundTag customData, CompoundTag components, int hideFlags) {
        if (!customData.getBooleanOr("Unbreakable", false)) {
            customData.remove("Unbreakable");
            return;
        }

        CompoundTag unbreakable = new CompoundTag();
        if ((hideFlags & 4) != 0) {
            hideTooltipComponent(components, "minecraft:unbreakable");
        }
        components.put("minecraft:unbreakable", unbreakable);
        customData.remove("Unbreakable");
    }

    private static void writeAttributeComponents(CompoundTag customData, CompoundTag components, int hideFlags) {
        if (!customData.contains("AttributeModifiers")) {
            return;
        }

        ListTag oldModifiers = customData.getListOrEmpty("AttributeModifiers");
        ListTag modifiers = new ListTag();
        for (int i = 0; i < oldModifiers.size(); i++) {
            CompoundTag oldModifier = oldModifiers.getCompoundOrEmpty(i);
            CompoundTag modifier = new CompoundTag();
            String type = oldModifier.contains("AttributeName")
                    ? oldModifier.getStringOr("AttributeName", "")
                    : oldModifier.getStringOr("type", "");
            if (type.isBlank()) {
                continue;
            }
            modifier.putString("type", type);
            modifier.putString("id", oldModifier.contains("id")
                    ? oldModifier.getStringOr("id", "")
                    : type + "/" + UUID.randomUUID());
            modifier.putDouble("amount", oldModifier.contains("Amount") ? oldModifier.getDoubleOr("Amount", 0.0D) : oldModifier.getDoubleOr("amount", 0.0D));
            modifier.putString("operation", operationName(oldModifier.contains("Operation") ? oldModifier.getIntOr("Operation", 0) : 0));
            if (oldModifier.contains("operation")) {
                modifier.putString("operation", oldModifier.getStringOr("operation", ""));
            }
            if (oldModifier.contains("Slot")) {
                modifier.putString("slot", oldModifier.getStringOr("Slot", ""));
            } else if (oldModifier.contains("slot")) {
                modifier.putString("slot", oldModifier.getStringOr("slot", ""));
            }
            modifiers.add(modifier);
        }
        if (!modifiers.isEmpty()) {
            components.put("minecraft:attribute_modifiers", modifiers);
        }
        if ((hideFlags & 2) != 0) {
            hideTooltipComponent(components, "minecraft:attribute_modifiers");
        }
        customData.remove("AttributeModifiers");
    }

    private static void writeAdventureComponents(CompoundTag customData, CompoundTag components, int hideFlags) {
        writeStringPredicateComponent(customData, components, "CanDestroy", "minecraft:can_break", (hideFlags & 8) != 0);
        writeStringPredicateComponent(customData, components, "CanPlaceOn", "minecraft:can_place_on", (hideFlags & 16) != 0);
    }

    private static void writeStringPredicateComponent(CompoundTag customData, CompoundTag components, String oldKey, String componentKey, boolean hidden) {
        if (!customData.contains(oldKey)) {
            return;
        }

        ListTag oldList = customData.getListOrEmpty(oldKey);
        ListTag predicates = new ListTag();
        for (int i = 0; i < oldList.size(); i++) {
            CompoundTag predicate = new CompoundTag();
            predicate.putString("blocks", oldList.getStringOr(i, ""));
            predicates.add(predicate);
        }
        if (predicates.size() == 1) {
            components.put(componentKey, predicates.getCompoundOrEmpty(0));
        } else if (!predicates.isEmpty()) {
            components.put(componentKey, predicates);
        }
        if (hidden) {
            hideTooltipComponent(components, componentKey);
        }
        customData.remove(oldKey);
    }

    private static void writePotionComponents(CompoundTag customData, CompoundTag components) {
        CompoundTag potion = new CompoundTag();
        if (customData.contains("Potion") && !"minecraft:empty".equals(customData.getStringOr("Potion", ""))) {
            potion.putString("potion", customData.getStringOr("Potion", ""));
        }
        moveInto(customData, potion, "CustomPotionColor", "custom_color");
        if (customData.contains("CustomPotionEffects")) {
            potion.put("custom_effects", convertPotionEffects(customData.getListOrEmpty("CustomPotionEffects")));
            customData.remove("CustomPotionEffects");
        } else {
            moveInto(customData, potion, "custom_potion_effects", "custom_effects");
        }
        customData.remove("Potion");
        if (!potion.isEmpty()) {
            components.put("minecraft:potion_contents", potion);
        }
    }

    private static void writeBookComponents(CompoundTag customData, CompoundTag components) {
        if (customData.contains("pages") && !customData.contains("title")) {
            CompoundTag book = new CompoundTag();
            book.put("pages", filterableStringList(customData.getListOrEmpty("pages"), null));
            components.put("minecraft:writable_book_content", book);
            customData.remove("pages");
            return;
        }

        if (customData.contains("title") || customData.contains("author") || customData.contains("pages")) {
            CompoundTag book = new CompoundTag();
            book.put("title", filterableText(customData.getStringOr("title", ""),
                    customData.contains("filtered_title") ? customData.getStringOr("filtered_title", "") : null));
            book.putString("author", customData.getStringOr("author", ""));
            if (customData.contains("generation")) {
                book.putInt("generation", clamp(customData.getIntOr("generation", 0), 0, 3));
            }
            if (customData.contains("resolved")) {
                book.putBoolean("resolved", customData.getBooleanOr("resolved", false));
            }
            if (customData.contains("pages")) {
                book.put("pages", filterableStringList(customData.getListOrEmpty("pages"),
                        customData.contains("filtered_pages") ? customData.getCompoundOrEmpty("filtered_pages") : null));
            }
            components.put("minecraft:written_book_content", book);
            customData.remove("title");
            customData.remove("filtered_title");
            customData.remove("author");
            customData.remove("generation");
            customData.remove("resolved");
            customData.remove("pages");
            customData.remove("filtered_pages");
        }
    }

    private static void writeFireworkComponents(CompoundTag customData, CompoundTag components) {
        if (customData.contains("Explosion")) {
            components.put("minecraft:firework_explosion", fireworkExplosionToComponent(customData.getCompoundOrEmpty("Explosion")));
            customData.remove("Explosion");
        }
        if (customData.contains("Fireworks")) {
            CompoundTag oldFireworks = customData.getCompoundOrEmpty("Fireworks");
            CompoundTag fireworks = new CompoundTag();
            fireworks.putInt("flight_duration", oldFireworks.getByteOr("Flight", (byte) 0) & 255);
            if (oldFireworks.contains("Explosions")) {
                ListTag explosions = new ListTag();
                ListTag oldExplosions = oldFireworks.getListOrEmpty("Explosions");
                for (int i = 0; i < oldExplosions.size(); i++) {
                    explosions.add(fireworkExplosionToComponent(oldExplosions.getCompoundOrEmpty(i)));
                }
                fireworks.put("explosions", explosions);
            }
            components.put("minecraft:fireworks", fireworks);
            customData.remove("Fireworks");
        }
    }

    private static void writeProfileComponent(CompoundTag customData, CompoundTag components) {
        if (!customData.contains("SkullOwner")) {
            return;
        }

        Tag skullOwner = customData.get("SkullOwner");
        if (skullOwner instanceof StringTag stringTag) {
            components.putString("minecraft:profile", stringTag.value());
        } else if (skullOwner instanceof CompoundTag skullOwnerTag) {
            CompoundTag profile = new CompoundTag();
            if (skullOwnerTag.contains("Name")) {
                profile.putString("name", skullOwnerTag.getStringOr("Name", ""));
            }
            if (NbtCompat.hasUUID(skullOwnerTag, "Id")) {
                NbtCompat.putUUID(profile, "id", NbtCompat.getUUID(skullOwnerTag, "Id"));
            }
            if (skullOwnerTag.contains("Properties")) {
                profile.put("properties", profilePropertiesToComponent(skullOwnerTag.getCompoundOrEmpty("Properties")));
            }
            components.put("minecraft:profile", profile);
        }
        customData.remove("SkullOwner");
    }

    private static void writeEntityDataComponent(String itemId, CompoundTag customData, CompoundTag components) {
        if (!customData.contains("EntityTag")) {
            return;
        }

        CompoundTag entity = customData.getCompoundOrEmpty("EntityTag").copy();
        ensureId(entity, inferEntityId(itemId));
        components.put("minecraft:entity_data", entity);
        customData.remove("EntityTag");
    }

    private static void writeTrimComponent(CompoundTag customData, CompoundTag components, int hideFlags) {
        Tag value = customData.get("Trim");
        if (value == null) {
            return;
        }

        Tag trim = value.copy();
        if ((hideFlags & 128) != 0) {
            hideTooltipComponent(components, "minecraft:trim");
        }
        components.put("minecraft:trim", trim);
        customData.remove("Trim");
    }

    private static void writeBlockEntityComponents(String itemId, CompoundTag customData, CompoundTag components) {
        if (!customData.contains("BlockEntityTag")) {
            return;
        }

        CompoundTag blockEntity = customData.getCompoundOrEmpty("BlockEntityTag").copy();
        ensureId(blockEntity, inferBlockEntityId(itemId));
        if (blockEntity.contains("Items")) {
            components.put("minecraft:container", containerItemsToComponent(blockEntity.getListOrEmpty("Items")));
            blockEntity.remove("Items");
        }
        if (blockEntity.contains("Patterns")) {
            components.put("minecraft:banner_patterns", bannerPatternsToComponent(blockEntity.getListOrEmpty("Patterns")));
            blockEntity.remove("Patterns");
        }
        if (blockEntity.contains("Base")) {
            components.putString("minecraft:base_color", dyeNameById(blockEntity.getIntOr("Base", 0)));
            blockEntity.remove("Base");
        }
        if ("minecraft:decorated_pot".equals(itemId) && blockEntity.contains("sherds")) {
            components.put("minecraft:pot_decorations", potDecorationsToComponent(blockEntity.getListOrEmpty("sherds")));
            blockEntity.remove("sherds");
        }
        moveInto(blockEntity, components, "Lock", "minecraft:lock");
        if (blockEntity.contains("LootTable")) {
            CompoundTag loot = new CompoundTag();
            loot.putString("loot_table", blockEntity.getStringOr("LootTable", ""));
            if (blockEntity.contains("LootTableSeed")) {
                loot.putLong("seed", blockEntity.getLongOr("LootTableSeed", 0L));
            }
            components.put("minecraft:container_loot", loot);
            blockEntity.remove("LootTable");
            blockEntity.remove("LootTableSeed");
        }
        if (!blockEntity.isEmpty() && !isOnlyId(blockEntity)) {
            components.put("minecraft:block_entity_data", blockEntity);
        }
        customData.remove("BlockEntityTag");
    }

    private static int readComponentDisplay(CompoundTag components, CompoundTag tag, int hideFlags) {
        CompoundTag display = tag.contains("display") ? tag.getCompoundOrEmpty("display").copy() : new CompoundTag();
        copyTextComponent(components, "minecraft:custom_name", display, "Name");
        copyTextComponent(components, "minecraft:item_name", display, "Name");
        copyLoreComponent(components, display);
        if (components.contains("minecraft:dyed_color")) {
            Tag dyed = components.get("minecraft:dyed_color");
            if (dyed instanceof NumericTag numericTag) {
                display.putInt("color", numericTag.intValue());
            } else if (dyed instanceof CompoundTag dyedTag) {
                display.putInt("color", dyedTag.getIntOr("rgb", 0));
            }
            if (isTooltipComponentHidden(components, "minecraft:dyed_color")) {
                hideFlags |= 64;
            }
        }
        copyComponent(components, "minecraft:map_color", display, "MapColor");
        if (display.isEmpty()) {
            tag.remove("display");
        } else {
            tag.put("display", display);
        }
        return hideFlags;
    }

    private static int readComponentEnchantments(CompoundTag components, CompoundTag tag, int hideFlags) {
        hideFlags = readEnchantmentsComponent(components, tag, "minecraft:enchantments", "Enchantments", hideFlags, 1);
        hideFlags = readEnchantmentsComponent(components, tag, "minecraft:stored_enchantments", "StoredEnchantments", hideFlags, 32);
        return hideFlags;
    }

    private static int readEnchantmentsComponent(CompoundTag components, CompoundTag tag, String componentKey, String oldKey, int hideFlags, int hideMask) {
        if (!components.contains(componentKey)) {
            return hideFlags;
        }

        CompoundTag component = components.get(componentKey) instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
        CompoundTag levels = component.contains("levels") ? component.getCompoundOrEmpty("levels") : component;
        ListTag enchantments = new ListTag();
        for (String id : levels.keySet()) {
            CompoundTag enchantment = new CompoundTag();
            enchantment.putString("id", id);
            enchantment.putInt("lvl", levels.getIntOr(id, 0));
            enchantments.add(enchantment);
        }
        if (!enchantments.isEmpty()) {
            tag.put(oldKey, enchantments);
        }
        if (isTooltipComponentHidden(components, componentKey)) {
            hideFlags |= hideMask;
        }
        return hideFlags;
    }

    private static int readComponentUnbreakable(CompoundTag components, CompoundTag tag, int hideFlags) {
        if (components.contains("minecraft:unbreakable")) {
            tag.putBoolean("Unbreakable", true);
            if (isTooltipComponentHidden(components, "minecraft:unbreakable")) {
                hideFlags |= 4;
            }
        }
        return hideFlags;
    }

    private static int readComponentAttributes(CompoundTag components, CompoundTag tag, int hideFlags) {
        if (!components.contains("minecraft:attribute_modifiers")) {
            return hideFlags;
        }

        Tag attributeComponent = components.get("minecraft:attribute_modifiers");
        ListTag modifiers;
        if (attributeComponent instanceof ListTag listTag) {
            modifiers = listTag;
        } else if (attributeComponent instanceof CompoundTag compoundTag) {
            modifiers = compoundTag.getListOrEmpty("modifiers");
        } else {
            modifiers = new ListTag();
        }
        ListTag oldModifiers = new ListTag();
        for (int i = 0; i < modifiers.size(); i++) {
            CompoundTag modifier = modifiers.getCompoundOrEmpty(i);
            CompoundTag oldModifier = new CompoundTag();
            oldModifier.putString("AttributeName", modifier.getStringOr("type", ""));
            oldModifier.putString("id", modifier.getStringOr("id", ""));
            oldModifier.putDouble("Amount", modifier.getDoubleOr("amount", 0.0D));
            oldModifier.putString("operation", modifier.getStringOr("operation", ""));
            oldModifier.putInt("Operation", operationId(modifier.getStringOr("operation", "")));
            if (modifier.contains("slot")) {
                oldModifier.putString("Slot", modifier.getStringOr("slot", ""));
            }
            oldModifiers.add(oldModifier);
        }
        if (!oldModifiers.isEmpty()) {
            tag.put("AttributeModifiers", oldModifiers);
        }
        if (isTooltipComponentHidden(components, "minecraft:attribute_modifiers")) {
            hideFlags |= 2;
        }
        return hideFlags;
    }

    private static int readComponentAdventure(CompoundTag components, CompoundTag tag, int hideFlags) {
        hideFlags = readStringPredicateComponent(components, tag, "minecraft:can_break", "CanDestroy", hideFlags, 8);
        return readStringPredicateComponent(components, tag, "minecraft:can_place_on", "CanPlaceOn", hideFlags, 16);
    }

    private static int readStringPredicateComponent(CompoundTag components, CompoundTag tag, String componentKey, String oldKey, int hideFlags, int hideMask) {
        if (!components.contains(componentKey)) {
            return hideFlags;
        }

        Tag adventureComponent = components.get(componentKey);
        ListTag predicates = new ListTag();
        if (adventureComponent instanceof CompoundTag compoundTag) {
            if (compoundTag.contains("predicates")) {
                predicates = compoundTag.getListOrEmpty("predicates");
            } else if (compoundTag.contains("blocks")) {
                predicates.add(compoundTag.copy());
            }
        } else if (adventureComponent instanceof ListTag listTag) {
            predicates = listTag;
        }
        ListTag oldList = new ListTag();
        for (int i = 0; i < predicates.size(); i++) {
            CompoundTag predicate = predicates.getCompoundOrEmpty(i);
            if (predicate.contains("blocks")) {
                oldList.add(StringTag.valueOf(predicate.getStringOr("blocks", "")));
            }
        }
        if (!oldList.isEmpty()) {
            tag.put(oldKey, oldList);
        }
        if (isTooltipComponentHidden(components, componentKey)) {
            hideFlags |= hideMask;
        }
        return hideFlags;
    }

    private static void readComponentPotion(CompoundTag components, CompoundTag tag) {
        if (!components.contains("minecraft:potion_contents")) {
            return;
        }

        CompoundTag potion = components.getCompoundOrEmpty("minecraft:potion_contents");
        copyTag(potion, "potion", tag, "Potion");
        copyTag(potion, "custom_color", tag, "CustomPotionColor");
        copyTag(potion, "custom_effects", tag, "CustomPotionEffects");
    }

    private static void readComponentBooks(CompoundTag components, CompoundTag tag) {
        if (components.contains("minecraft:writable_book_content")) {
            CompoundTag book = components.getCompoundOrEmpty("minecraft:writable_book_content");
            tag.put("pages", readFilterableList(book.getListOrEmpty("pages")));
        }
        if (components.contains("minecraft:written_book_content")) {
            CompoundTag book = components.getCompoundOrEmpty("minecraft:written_book_content");
            tag.putString("title", readFilterableString(book.get("title")));
            tag.putString("author", book.getStringOr("author", ""));
            if (book.contains("generation")) {
                tag.putInt("generation", book.getIntOr("generation", 0));
            }
            if (book.contains("resolved")) {
                tag.putBoolean("resolved", book.getBooleanOr("resolved", false));
            }
            tag.put("pages", readFilterableList(book.getListOrEmpty("pages")));
        }
    }

    private static void readComponentFireworks(CompoundTag components, CompoundTag tag) {
        if (components.contains("minecraft:firework_explosion")) {
            tag.put("Explosion", fireworkExplosionFromComponent(components.getCompoundOrEmpty("minecraft:firework_explosion")));
        }
        if (components.contains("minecraft:fireworks")) {
            CompoundTag component = components.getCompoundOrEmpty("minecraft:fireworks");
            CompoundTag fireworks = new CompoundTag();
            fireworks.putByte("Flight", (byte) component.getIntOr("flight_duration", 0));
            ListTag oldExplosions = new ListTag();
            ListTag explosions = component.getListOrEmpty("explosions");
            for (int i = 0; i < explosions.size(); i++) {
                oldExplosions.add(fireworkExplosionFromComponent(explosions.getCompoundOrEmpty(i)));
            }
            if (!oldExplosions.isEmpty()) {
                fireworks.put("Explosions", oldExplosions);
            }
            tag.put("Fireworks", fireworks);
        }
    }

    private static void readComponentProfile(CompoundTag components, CompoundTag tag) {
        if (!components.contains("minecraft:profile")) {
            return;
        }

        Tag profile = components.get("minecraft:profile");
        if (profile instanceof StringTag stringTag) {
            tag.putString("SkullOwner", stringTag.value());
        } else if (profile instanceof CompoundTag profileTag) {
            CompoundTag skullOwner = new CompoundTag();
            copyTag(profileTag, "name", skullOwner, "Name");
            if (NbtCompat.hasUUID(profileTag, "id")) {
                NbtCompat.putUUID(skullOwner, "Id", NbtCompat.getUUID(profileTag, "id"));
            }
            if (profileTag.contains("properties")) {
                skullOwner.put("Properties", profilePropertiesFromComponent(profileTag.getListOrEmpty("properties")));
            }
            tag.put("SkullOwner", skullOwner);
        }
    }

    private static void readComponentBlockEntity(ItemStack stack, CompoundTag components, CompoundTag tag) {
        CompoundTag blockEntity = tag.contains("BlockEntityTag") ? tag.getCompoundOrEmpty("BlockEntityTag").copy() : new CompoundTag();
        if (components.contains("minecraft:block_entity_data")) {
            blockEntity.merge(components.getCompoundOrEmpty("minecraft:block_entity_data").copy());
        }
        if (components.contains("minecraft:container")) {
            blockEntity.put("Items", containerItemsFromComponent(components.getListOrEmpty("minecraft:container")));
        }
        if (components.contains("minecraft:banner_patterns")) {
            blockEntity.put("Patterns", bannerPatternsFromComponent(components.getListOrEmpty("minecraft:banner_patterns")));
        }
        if (components.contains("minecraft:base_color")) {
            blockEntity.putInt("Base", dyeIdByName(components.getStringOr("minecraft:base_color", "")));
        }
        if (stack.is(Items.DECORATED_POT) && components.contains("minecraft:pot_decorations")) {
            blockEntity.put("sherds", potDecorationsFromComponent(components.getListOrEmpty("minecraft:pot_decorations")));
        }
        copyTag(components, "minecraft:lock", blockEntity, "Lock");
        if (components.contains("minecraft:container_loot")) {
            CompoundTag loot = components.getCompoundOrEmpty("minecraft:container_loot");
            copyTag(loot, "loot_table", blockEntity, "LootTable");
            copyTag(loot, "seed", blockEntity, "LootTableSeed");
        }
        if (!blockEntity.isEmpty()) {
            tag.put("BlockEntityTag", blockEntity);
        }
    }

    private static ListTag containerItemsToComponent(ListTag oldItems) {
        ListTag items = new ListTag();
        for (int i = 0; i < oldItems.size(); i++) {
            CompoundTag oldItem = oldItems.getCompoundOrEmpty(i);
            CompoundTag item = new CompoundTag();
            item.putInt("slot", oldItem.getByteOr("Slot", (byte) 0) & 255);
            CompoundTag stackTag = oldItem.copy();
            stackTag.remove("Slot");
            item.put("item", normalizeStackTag(stackTag));
            items.add(item);
        }
        return items;
    }

    private static ListTag containerItemsFromComponent(ListTag componentItems) {
        ListTag items = new ListTag();
        for (int i = 0; i < componentItems.size(); i++) {
            CompoundTag componentItem = componentItems.getCompoundOrEmpty(i);
            if (!componentItem.contains("item")) {
                continue;
            }
            CompoundTag item = componentItem.getCompoundOrEmpty("item").copy();
            item.putByte("Slot", (byte) componentItem.getIntOr("slot", 0));
            items.add(item);
        }
        return items;
    }

    private static ListTag bannerPatternsToComponent(ListTag oldPatterns) {
        ListTag patterns = new ListTag();
        for (int i = 0; i < oldPatterns.size(); i++) {
            CompoundTag oldPattern = oldPatterns.getCompoundOrEmpty(i);
            CompoundTag pattern = new CompoundTag();
            pattern.putString("pattern", bannerPatternId(oldPattern.getStringOr("Pattern", "")));
            pattern.putString("color", dyeNameById(oldPattern.getIntOr("Color", 0)));
            patterns.add(pattern);
        }
        return patterns;
    }

    private static ListTag bannerPatternsFromComponent(ListTag componentPatterns) {
        ListTag patterns = new ListTag();
        for (int i = 0; i < componentPatterns.size(); i++) {
            CompoundTag componentPattern = componentPatterns.getCompoundOrEmpty(i);
            CompoundTag pattern = new CompoundTag();
            pattern.putString("Pattern", bannerPatternHash(componentPattern.getStringOr("pattern", "")));
            pattern.putInt("Color", dyeIdByName(componentPattern.getStringOr("color", "")));
            patterns.add(pattern);
        }
        return patterns;
    }

    private static ListTag potDecorationsToComponent(ListTag oldSherds) {
        ListTag sherds = new ListTag();
        for (int i = 0; i < 4; i++) {
            String itemId = i < oldSherds.size() ? oldSherds.getStringOr(i, "") : "minecraft:brick";
            sherds.add(StringTag.valueOf(normalizeItemId(itemId)));
        }
        return sherds;
    }

    private static ListTag potDecorationsFromComponent(ListTag componentSherds) {
        ListTag sherds = new ListTag();
        for (int i = 0; i < Math.min(4, componentSherds.size()); i++) {
            sherds.add(StringTag.valueOf(normalizeItemId(componentSherds.getStringOr(i, ""))));
        }
        return sherds;
    }

    private static String normalizeItemId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return "minecraft:brick";
        }
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private static CompoundTag fireworkExplosionToComponent(CompoundTag oldExplosion) {
        CompoundTag explosion = new CompoundTag();
        explosion.putString("shape", switch (oldExplosion.getByteOr("Type", (byte) 0)) {
            case 1 -> "large_ball";
            case 2 -> "star";
            case 3 -> "creeper";
            case 4 -> "burst";
            default -> "small_ball";
        });
        if (oldExplosion.contains("Colors")) {
            explosion.put("colors", intArrayToList(NbtCompat.getIntArray(oldExplosion, "Colors")));
        }
        if (oldExplosion.contains("FadeColors")) {
            explosion.put("fade_colors", intArrayToList(NbtCompat.getIntArray(oldExplosion, "FadeColors")));
        }
        if (oldExplosion.contains("Trail")) {
            explosion.putBoolean("has_trail", oldExplosion.getBooleanOr("Trail", false));
        }
        if (oldExplosion.contains("Flicker")) {
            explosion.putBoolean("has_twinkle", oldExplosion.getBooleanOr("Flicker", false));
        }
        return explosion;
    }

    private static CompoundTag fireworkExplosionFromComponent(CompoundTag explosion) {
        CompoundTag oldExplosion = new CompoundTag();
        oldExplosion.putByte("Type", (byte) switch (explosion.getStringOr("shape", "")) {
            case "large_ball" -> 1;
            case "star" -> 2;
            case "creeper" -> 3;
            case "burst" -> 4;
            default -> 0;
        });
        if (explosion.contains("colors")) {
            oldExplosion.putIntArray("Colors", listToIntArray(explosion.getListOrEmpty("colors")));
        }
        if (explosion.contains("fade_colors")) {
            oldExplosion.putIntArray("FadeColors", listToIntArray(explosion.getListOrEmpty("fade_colors")));
        }
        if (explosion.contains("has_trail")) {
            oldExplosion.putBoolean("Trail", explosion.getBooleanOr("has_trail", false));
        }
        if (explosion.contains("has_twinkle")) {
            oldExplosion.putBoolean("Flicker", explosion.getBooleanOr("has_twinkle", false));
        }
        return oldExplosion;
    }

    private static ListTag convertPotionEffects(ListTag oldEffects) {
        ListTag effects = new ListTag();
        for (int i = 0; i < oldEffects.size(); i++) {
            CompoundTag oldEffect = oldEffects.getCompoundOrEmpty(i);
            CompoundTag effect = oldEffect.copy();
            String id = potionEffectId(oldEffect);
            if (!id.isBlank()) {
                effect.putString("id", id);
            }
            if (oldEffect.contains("Amplifier")) {
                effect.putInt("amplifier", oldEffect.getByteOr("Amplifier", (byte) 0) & 255);
            }
            if (oldEffect.contains("Duration")) {
                effect.putInt("duration", oldEffect.getIntOr("Duration", 0));
            }
            if (oldEffect.contains("Ambient")) {
                effect.putBoolean("ambient", oldEffect.getBooleanOr("Ambient", false));
            }
            if (oldEffect.contains("ShowParticles")) {
                effect.putBoolean("show_particles", oldEffect.getBooleanOr("ShowParticles", false));
            }
            if (oldEffect.contains("ShowIcon")) {
                effect.putBoolean("show_icon", oldEffect.getBooleanOr("ShowIcon", false));
            }
            effect.remove("Id");
            effect.remove("Amplifier");
            effect.remove("Duration");
            effect.remove("Ambient");
            effect.remove("ShowParticles");
            effect.remove("ShowIcon");
            effects.add(effect);
        }
        return effects;
    }

    private static String potionEffectId(CompoundTag effect) {
        if (effect.contains("Id")) {
            return effect.getStringOr("Id", "");
        }
        if (effect.contains("Id")) {
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.byId(effect.getIntOr("Id", 0));
            ResourceLocation id = mobEffect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
            return id == null ? "" : id.toString();
        }
        return effect.contains("id") ? effect.getStringOr("id", "") : "";
    }

    private static CompoundTag filterableText(String raw, String filtered) {
        CompoundTag tag = new CompoundTag();
        tag.putString("raw", raw == null ? "" : raw);
        if (filtered != null) {
            tag.putString("filtered", filtered);
        }
        return tag;
    }

    private static ListTag filterableStringList(ListTag strings, CompoundTag filteredPages) {
        ListTag pages = new ListTag();
        for (int i = 0; i < strings.size(); i++) {
            String filtered = filteredPages != null && filteredPages.contains(Integer.toString(i))
                    ? filteredPages.getStringOr(Integer.toString(i), "")
                    : null;
            pages.add(filterableText(strings.getStringOr(i, ""), filtered));
        }
        return pages;
    }

    private static ListTag readFilterableList(ListTag pages) {
        ListTag strings = new ListTag();
        for (int i = 0; i < pages.size(); i++) {
            strings.add(StringTag.valueOf(readFilterableString(pages.get(i))));
        }
        return strings;
    }

    private static String readFilterableString(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return stringTag.value();
        }
        if (tag instanceof CompoundTag compoundTag) {
            if (compoundTag.contains("raw")) {
                return compoundTag.getStringOr("raw", "");
            }
            if (compoundTag.contains("filtered")) {
                return compoundTag.getStringOr("filtered", "");
            }
        }
        return "";
    }

    private static ListTag profilePropertiesToComponent(CompoundTag properties) {
        ListTag converted = new ListTag();
        for (String name : properties.keySet()) {
            ListTag values = properties.getListOrEmpty(name);
            for (int i = 0; i < values.size(); i++) {
                CompoundTag value = values.getCompoundOrEmpty(i);
                CompoundTag property = new CompoundTag();
                property.putString("name", name);
                property.putString("value", value.getStringOr("Value", ""));
                if (value.contains("Signature")) {
                    property.putString("signature", value.getStringOr("Signature", ""));
                }
                converted.add(property);
            }
        }
        return converted;
    }

    private static CompoundTag profilePropertiesFromComponent(ListTag properties) {
        CompoundTag converted = new CompoundTag();
        for (int i = 0; i < properties.size(); i++) {
            CompoundTag property = properties.getCompoundOrEmpty(i);
            String name = property.getStringOr("name", "");
            ListTag values = converted.contains(name) ? converted.getListOrEmpty(name) : new ListTag();
            CompoundTag value = new CompoundTag();
            value.putString("Value", property.getStringOr("value", ""));
            if (property.contains("signature")) {
                value.putString("Signature", property.getStringOr("signature", ""));
            }
            values.add(value);
            converted.put(name, values);
        }
        return converted;
    }

    private static ListTag intArrayToList(int[] values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private static int[] listToIntArray(ListTag list) {
        int[] values = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            values[i] = list.getIntOr(i, 0);
        }
        return values;
    }

    private static void copyComponent(CompoundTag components, String componentKey, CompoundTag tag, String oldKey) {
        copyTag(components, componentKey, tag, oldKey);
    }

    private static void copyTag(CompoundTag source, String sourceKey, CompoundTag target, String targetKey) {
        Tag value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value.copy());
        }
    }

    private static void moveComponent(CompoundTag source, String oldKey, CompoundTag components, String componentKey) {
        Tag value = source.get(oldKey);
        if (value != null) {
            components.put(componentKey, value.copy());
            source.remove(oldKey);
        }
    }

    private static void moveInto(CompoundTag source, CompoundTag target, String sourceKey, String targetKey) {
        Tag value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value.copy());
            source.remove(sourceKey);
        }
    }

    private static int removeInt(CompoundTag tag, String key) {
        int value = tag.getIntOr(key, 0);
        tag.remove(key);
        return value;
    }

    private static String operationName(int id) {
        return switch (id) {
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
            default -> "add_value";
        };
    }

    private static int operationId(String name) {
        return switch (name) {
            case "add_multiplied_base" -> 1;
            case "add_multiplied_total" -> 2;
            default -> 0;
        };
    }

    private static void ensureId(CompoundTag tag, String inferredId) {
        if (!tag.contains("id") && inferredId != null && !inferredId.isBlank()) {
            tag.putString("id", inferredId);
        }
    }

    private static boolean isOnlyId(CompoundTag tag) {
        return tag.size() == 1 && tag.contains("id");
    }

    private static String inferEntityId(String itemId) {
        String namespace = namespace(itemId);
        String path = path(itemId);
        if ("armor_stand".equals(path)) {
            return "minecraft:armor_stand";
        }
        if ("item_frame".equals(path)) {
            return "minecraft:item_frame";
        }
        if ("glow_item_frame".equals(path)) {
            return "minecraft:glow_item_frame";
        }
        if ("command_block_minecart".equals(path)) {
            return "minecraft:command_block_minecart";
        }
        if (path.endsWith("_spawn_egg")) {
            return namespace + ":" + path.substring(0, path.length() - "_spawn_egg".length());
        }
        return null;
    }

    private static String inferBlockEntityId(String itemId) {
        String path = path(itemId);
        if (path.endsWith("_shulker_box")) {
            return "minecraft:shulker_box";
        }
        if (path.endsWith("_banner")) {
            return "minecraft:banner";
        }
        if (path.endsWith("_bed")) {
            return "minecraft:bed";
        }
        if (path.endsWith("_hanging_sign")) {
            return "minecraft:hanging_sign";
        }
        if (path.endsWith("_sign")) {
            return "minecraft:sign";
        }
        if (path.endsWith("_head") || path.endsWith("_skull")) {
            return "minecraft:skull";
        }

        String id = BLOCK_ENTITY_BY_ITEM.get(path);
        return id == null ? null : "minecraft:" + id;
    }

    private static String namespace(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(0, separator) : "minecraft";
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static String bannerPatternId(String hash) {
        String id = hash.contains(":") ? hash.substring(hash.indexOf(':') + 1) : hash;
        return "minecraft:" + BANNER_HASH_TO_PATTERN.getOrDefault(id, id);
    }

    private static String bannerPatternHash(String pattern) {
        String id = pattern.contains(":") ? pattern.substring(pattern.indexOf(':') + 1) : pattern;
        return BANNER_PATTERN_TO_HASH.getOrDefault(id, id);
    }

    private static String dyeNameById(int id) {
        return DyeColor.byId(id).getName();
    }

    private static int dyeIdByName(String name) {
        for (DyeColor color : DyeColor.values()) {
            if (color.getName().equals(name)) {
                return color.getId();
            }
        }
        return DyeColor.WHITE.getId();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Map<String, String> invert(Map<String, String> input) {
        Map<String, String> inverted = new HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            inverted.put(entry.getValue(), entry.getKey());
        }
        return inverted;
    }

    private static final Constructor<CompoundTag> COMPOUND_TAG_CONSTRUCTOR = compoundTagConstructor();
    private static final Constructor<ListTag> LIST_TAG_CONSTRUCTOR = listTagConstructor();

    private static CompoundTag syncedCompound(CompoundTag source, Runnable onChanged) {
        ListeningMap map = new ListeningMap(onChanged);
        map.muted = true;
        for (String key : source.keySet()) {
            Tag value = source.get(key);
            if (value != null) {
                map.put(key, syncedTag(value, onChanged));
            }
        }
        map.muted = false;
        try {
            return COMPOUND_TAG_CONSTRUCTOR.newInstance(map);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to create synced CompoundTag", exception);
        }
    }

    private static ListTag syncedList(ListTag source, Runnable onChanged) {
        ListeningList list = new ListeningList(onChanged);
        list.muted = true;
        for (int i = 0; i < source.size(); i++) {
            list.add(syncedTag(source.get(i), onChanged));
        }
        list.muted = false;
        try {
            return LIST_TAG_CONSTRUCTOR.newInstance(list);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to create synced ListTag", exception);
        }
    }

    private static Tag syncedTag(Tag tag, Runnable onChanged) {
        if (tag instanceof CompoundTag compoundTag) {
            return syncedCompound(compoundTag, onChanged);
        }
        if (tag instanceof ListTag listTag) {
            return syncedList(listTag, onChanged);
        }
        return tag.copy();
    }

    private static Constructor<CompoundTag> compoundTagConstructor() {
        try {
            Constructor<CompoundTag> constructor = CompoundTag.class.getDeclaredConstructor(Map.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("CompoundTag map constructor is unavailable", exception);
        }
    }

    private static Constructor<ListTag> listTagConstructor() {
        try {
            Constructor<ListTag> constructor = ListTag.class.getDeclaredConstructor(List.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("ListTag list constructor is unavailable", exception);
        }
    }

    private static final class ListeningMap extends HashMap<String, Tag> {
        private final Runnable onChanged;
        private boolean muted;

        private ListeningMap(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public Tag put(String key, Tag value) {
            Tag old = super.put(key, syncedTag(value, this.onChanged));
            changed();
            return old;
        }

        @Override
        public Tag remove(Object key) {
            Tag old = super.remove(key);
            changed();
            return old;
        }

        @Override
        public void clear() {
            super.clear();
            changed();
        }

        private void changed() {
            if (!this.muted) {
                this.onChanged.run();
            }
        }
    }

    private static final class ListeningList extends ArrayList<Tag> {
        private final Runnable onChanged;
        private boolean muted;

        private ListeningList(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public Tag set(int index, Tag element) {
            Tag old = super.set(index, syncedTag(element, this.onChanged));
            changed();
            return old;
        }

        @Override
        public void add(int index, Tag element) {
            super.add(index, syncedTag(element, this.onChanged));
            changed();
        }

        @Override
        public boolean add(Tag tag) {
            boolean added = super.add(syncedTag(tag, this.onChanged));
            if (added) {
                changed();
            }
            return added;
        }

        @Override
        public Tag remove(int index) {
            Tag old = super.remove(index);
            changed();
            return old;
        }

        @Override
        public boolean remove(Object value) {
            boolean removed = super.remove(value);
            if (removed) {
                changed();
            }
            return removed;
        }

        @Override
        public void clear() {
            if (isEmpty()) {
                return;
            }
            super.clear();
            changed();
        }

        private void changed() {
            if (!this.muted) {
                this.onChanged.run();
            }
        }
    }
}
