package io.github.seraphina.infinity_item_editor_re.client.screen;

import io.github.seraphina.infinity_item_editor_re.util.ItemStackCompat;

import io.github.seraphina.infinity_item_editor_re.util.ComponentCompat;

import io.github.seraphina.infinity_item_editor_re.util.ItemStackNbt;
import io.github.seraphina.infinity_item_editor_re.util.LegacyPluginCompat;
import io.github.seraphina.infinity_item_editor_re.util.LegacyText;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import io.github.seraphina.infinity_item_editor_re.ModSource;
import io.github.seraphina.infinity_item_editor_re.client.CreativeTabRefresher;
import io.github.seraphina.infinity_item_editor_re.data.realms.RealmController;
import io.github.seraphina.infinity_item_editor_re.util.GiveHelper;
import io.github.seraphina.infinity_item_editor_re.util.PlayerInventorySlots;
import io.github.seraphina.infinity_item_editor_re.util.SaveVerifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.WrittenBookItem;
import io.github.seraphina.infinity_item_editor_re.util.PotionCompat;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import io.github.seraphina.infinity_item_editor_re.util.CompatRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
abstract class ItemEditorScreenActions extends ItemEditorScreenColorLore {
    protected ItemEditorScreenActions(ItemStack stack, int targetContainerSlot, ItemEditorScreen parentTradeScreen, int parentTradeIndex, int parentTradeSlot) {
        super(stack, targetContainerSlot, parentTradeScreen, parentTradeIndex, parentTradeSlot);
    }

    protected void switchPanel(Panel panel) {
        if (this.activePanel == panel) {
            return;
        }
        captureFieldValues();
        if (this.activePanel == Panel.ITEM) {
            applyMainFieldsToStack(false);
        }
        this.activePanel = panel;
        this.status = Component.empty();
        rebuildWidgets();
    }

    protected void openContainerItemEditor() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (!applyMainFieldsToStack(true) || !isContainerEditableItem(this.previewStack)) {
            return;
        }
        this.status = Component.empty();
        syncNbtEditorValuesFromStack();
        this.minecraft.setScreen(ContainerItemScreen.create((ItemEditorScreen) this, this.minecraft.player, this.previewStack));
    }

    protected void openBookItemEditor() {
        if (this.minecraft == null) {
            return;
        }
        if (!applyMainFieldsToStack(true) || !isBookEditableItem(this.previewStack)) {
            return;
        }
        this.status = Component.empty();
        syncNbtEditorValuesFromStack();
        this.minecraft.setScreen(new BookItemScreen((ItemEditorScreen) this, this.previewStack));
    }

    protected void openJsonEditor() {
        if (this.minecraft == null) {
            return;
        }
        if (!applyMainFieldsToStack(true)) {
            return;
        }
        this.status = Component.empty();
        syncNbtEditorValuesFromStack();
        this.minecraft.setScreen(new ItemJsonEditorScreen((ItemEditorScreen) this, this.previewStack));
    }

    protected void openCommandBlockEditor() {
        if (this.minecraft == null) {
            return;
        }
        if (!applyMainFieldsToStack(true) || !isCommandBlockEditableItem(this.previewStack)) {
            return;
        }
        this.status = Component.empty();
        syncNbtEditorValuesFromStack();
        this.minecraft.setScreen(new ItemCommandBlockEditorScreen((ItemEditorScreen) this, this.previewStack));
    }

    protected void openArmorTrimEditor() {
        if (this.minecraft == null) {
            return;
        }
        if (!applyMainFieldsToStack(true) || !isArmorTrimApplicable(this.previewStack)) {
            return;
        }
        this.status = Component.empty();
        syncNbtEditorValuesFromStack();
        this.minecraft.setScreen(new ArmorTrimEditorScreen((ItemEditorScreen) this, this.previewStack));
    }

    protected void openItemPicker() {
        if (this.minecraft == null) {
            return;
        }
        if (this.activePanel == Panel.ITEM) {
            applyMainFieldsToStack(false);
        }
        this.minecraft.setScreen(new ItemPickScreen((ItemEditorScreen) this, this::replacePickedStack, () -> this.previewStack));
    }

    private void replacePickedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        this.previewStack = stack.copy();
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
        syncItemPanelFieldsFromStack();
    }

    private void syncItemPanelFieldsFromStack() {
        if (this.itemIdBox != null) {
            this.itemIdBox.setValue(this.itemIdValue);
        }
        if (this.countBox != null) {
            this.countBox.setValue(this.countValue);
        }
        if (this.damageBox != null) {
            this.damageBox.setValue(this.damageValue);
        }
        if (this.nameBox != null) {
            this.nameBox.setValue(this.nameValue);
        }
    }

    void refreshAfterContainerEdit() {
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.containerSlotNbtValue = getContainerSelectedSlotNbt();
        this.nbtFeedback = "";
    }

    void refreshAfterBookEdit() {
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
    }

    void applyJsonEditedStack(ItemStack stack) {
        if (stack == null) {
            return;
        }
        this.previewStack = stack.copy();
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
        this.status = Component.translatable(messageKey("editor_json_applied"), this.previewStack.getHoverName());
    }

    void applyCommandBlockEditedStack(ItemStack stack) {
        if (stack == null) {
            return;
        }
        this.previewStack = stack.copy();
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
        this.status = Component.translatable(messageKey("editor_command_block_applied"));
    }

    void applyArmorTrimEditedStack(ItemStack stack) {
        if (stack == null) {
            return;
        }
        this.previewStack = stack.copy();
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
        this.status = Component.translatable(messageKey("editor_armor_trim_applied"));
    }

    protected void goBack() {
        if (this.activePanel == Panel.ITEM) {
            if (isTradeSlotEditor()) {
                applyTradeSlotEditorAndReturn();
                return;
            }
            onClose();
            return;
        }

        if (this.activePanel == Panel.TRADE) {
            this.activePanel = Panel.TRADES;
            ensureVillagerTradeOffers();
            rebuildWidgets();
            return;
        }

        this.activePanel = Panel.ITEM;
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        rebuildWidgets();
    }

    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    protected void applyToSelectedSlot() {
        if (!applyMainFieldsToStack(true) || this.previewStack.isEmpty() || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        restoreLostComponents();

        int selected = this.minecraft.player.getInventory().getSelectedSlot();
        int containerSlot = this.targetContainerSlot >= 0
                ? this.targetContainerSlot
                : PlayerInventorySlots.HOTBAR_CONTAINER_SLOT_START + selected;
        ItemStack inventoryStack = this.previewStack.copy();
        if (this.minecraft.isSingleplayer()) {
            if (!applySingleplayerInventoryStack(containerSlot, inventoryStack)) {
                this.status = Component.translatable(messageKey("editor_invalid_target_slot"));
                return;
            }

            ModSource.LOGGER.info("Infinity Item Editor save: {}", ItemStackNbt.save(inventoryStack));
            this.status = Component.translatable(messageKey("editor_applied"), inventoryStack.getHoverName());
            return;
        }

        if (this.minecraft.gameMode == null) {
            return;
        }

        if (Screen.hasShiftDown()) {
            String command = GiveHelper.getStringFromItemStack(inventoryStack);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            this.minecraft.player.connection.sendCommand(command);
            this.status = Component.translatable(messageKey("editor_give_sent"), inventoryStack.getHoverName());
            return;
        }
        if (this.minecraft.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.CREATIVE) {
            this.status = Component.translatable(messageKey("editor_server_not_creative"));
            this.minecraft.keyboardHandler.setClipboard(GiveHelper.getStringFromItemStack(inventoryStack));
            return;
        }
        if (!this.minecraft.player.getAbilities().instabuild) {
            this.status = Component.translatable(messageKey("editor_requires_creative"));
            return;
        }

        if (!PlayerInventorySlots.setStack(this.minecraft.player, containerSlot, inventoryStack)) {
            this.status = Component.translatable(messageKey("editor_invalid_target_slot"));
            return;
        }

        String sentNbt = ItemStackNbt.save(inventoryStack).toString();
        ModSource.LOGGER.info("Infinity Item Editor save: {}", sentNbt);
        int inventorySlot = containerSlot >= 36 && containerSlot <= 44
                ? containerSlot - 36
                : (containerSlot >= 9 && containerSlot <= 35 ? containerSlot : -1);
        SaveVerifier.expect(inventoryStack.copy(), sentNbt, inventorySlot);
        this.minecraft.gameMode.handleCreativeModeItemAdd(inventoryStack.copy(), containerSlot);
        this.status = Component.translatable(messageKey("editor_save_sent"), inventoryStack.getHoverName());
    }

    private static final List<DataComponentType<?>> PROTECTED_COMPONENTS = List.of(
            DataComponents.CUSTOM_DATA,
            DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS,
            DataComponents.ATTRIBUTE_MODIFIERS,
            DataComponents.TRIM,
            DataComponents.PROFILE,
            DataComponents.BLOCK_ENTITY_DATA,
            DataComponents.ENTITY_DATA,
            DataComponents.POTION_CONTENTS,
            DataComponents.FIREWORKS,
            DataComponents.BANNER_PATTERNS,
            DataComponents.BASE_COLOR,
            DataComponents.POT_DECORATIONS,
            DataComponents.CONTAINER,
            DataComponents.TOOLTIP_DISPLAY,
            DataComponents.UNBREAKABLE,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.BUNDLE_CONTENTS,
            DataComponents.MAP_ID,
            DataComponents.MAP_DECORATIONS,
            DataComponents.LOCK,
            DataComponents.CONTAINER_LOOT,
            DataComponents.RECIPES,
            DataComponents.INSTRUMENT
    );

    /**
     * The legacy NBT view only knows a subset of 1.21 components; a failed conversion used to drop
     * the rest when saving. Restore anything the original stack had that the edited stack lost,
     * except name/lore which the user edits directly.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void restoreLostComponents() {
        if (this.originalStack.isEmpty() || this.previewStack.isEmpty()) {
            return;
        }

        List<DataComponentType<?>> missing = new ArrayList<>();
        for (DataComponentType<?> type : PROTECTED_COMPONENTS) {
            if (this.originalStack.has(type) && !this.previewStack.has(type)) {
                missing.add(type);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        // A single missing component is usually an intentional edit (e.g. clearing enchantments);
        // two or more means a conversion dropped data, so restore everything that disappeared.
        boolean catastrophic = missing.size() >= 2;
        List<String> restored = new ArrayList<>();
        for (DataComponentType<?> type : missing) {
            if (catastrophic || type == DataComponents.CUSTOM_DATA) {
                setComponent(this.previewStack, type, this.originalStack);
                restored.add(type.toString());
            }
        }
        if (!restored.isEmpty()) {
            ModSource.LOGGER.warn("Infinity Item Editor: restored lost components before saving: {}", restored);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setComponent(ItemStack target, DataComponentType type, ItemStack source) {
        target.set(type, source.get(type));
    }

    private boolean applySingleplayerInventoryStack(int containerSlot, ItemStack stack) {
        IntegratedServer server = this.minecraft.getSingleplayerServer();
        if (server == null || !PlayerInventorySlots.setStack(this.minecraft.player, containerSlot, stack)) {
            return false;
        }

        UUID playerId = this.minecraft.player.getUUID();
        CompoundTag serverStackTag = ItemStackNbt.save(stack);
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
            ItemStack serverStack = ItemStackNbt.parse(server.registryAccess(), serverStackTag.copy());
            if (serverPlayer == null || !PlayerInventorySlots.setStack(serverPlayer, containerSlot, serverStack)) {
                return;
            }

            serverPlayer.containerMenu.broadcastChanges();
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.inventoryMenu.broadcastChanges();
            }
        });
        return true;
    }

    protected void dropEditedStack() {
        if (Screen.hasShiftDown()) {
            copyGiveCommand();
            return;
        }

        if (!applyMainFieldsToStack(true) || this.previewStack.isEmpty() || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }

        if (this.minecraft.player == null || !this.minecraft.player.getAbilities().instabuild) {
            this.status = Component.translatable(messageKey("editor_requires_creative"));
            return;
        }

        ItemStack dropped = this.previewStack.copy();
        this.minecraft.gameMode.handleCreativeModeItemDrop(dropped);
        this.status = Component.translatable(messageKey("editor_dropped"), dropped.getHoverName());
    }

    protected void copyGiveCommand() {
        if (!applyMainFieldsToStack(true) || this.previewStack.isEmpty() || this.minecraft == null) {
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(GiveHelper.getStringFromItemStack(this.previewStack));
        this.status = Component.translatable(messageKey("editor_copied"));
    }

    protected void resetStack() {
        if (this.activePanel == Panel.TRADES) {
            clearVillagerTrades();
            return;
        }

        if (this.activePanel == Panel.TRADE) {
            return;
        }

        if (this.activePanel == Panel.COMPONENTS) {
            this.previewStack = parseStackWithComponents(this.previewStack, new CompoundTag());
        } else if (this.activePanel == Panel.NBT) {
            ItemStackNbt.set(this.previewStack, new CompoundTag());
        } else {
            ItemStackNbt.set(this.previewStack, null);
        }
        readMainFieldsFromStack(this.previewStack);
        syncNbtEditorValuesFromStack();
        this.nbtFeedback = "";
        rebuildWidgets();
    }

    protected void saveRealm() {
        if (!applyMainFieldsToStack(true) || this.previewStack.isEmpty() || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        RealmController realmController = ModSource.getOrCreateRealmController(this.minecraft.gameDirectory);
        if (realmController != null) {
            if (realmController.addItemStack(this.minecraft.player, this.previewStack.copy())) {
                CreativeTabRefresher.refreshRealm(this.minecraft);
                this.status = Component.translatable(messageKey("editor_saved"), this.previewStack.getHoverName());
            }
        }
    }

    protected boolean applyMainFieldsToStack(boolean updateStatus) {
        try {
            captureFieldValues();
            tryApplyItemId(true);
            tryApplyCount(true);
            tryApplyDamage(true);
            applyNameToStack();
            applyLoreToStack();
            if (Boolean.getBoolean("infinity_item_editor_re.syncLegacyPluginData")) {
                LegacyPluginCompat.sync(this.previewStack, this.nameValue, this.loreValues);
            }
            if (this.activePanel == Panel.SIGN) {
                applySignToStack();
            }
            if (this.activePanel == Panel.BOOK) {
                applyBookMetadataToStack();
            }
            if (this.activePanel == Panel.HEAD) {
                applyHeadToStack();
            }
            if (this.activePanel == Panel.BANNER) {
                readBannerFieldsFromStack(this.previewStack);
            }
            if (this.activePanel == Panel.DECORATED_POT) {
                readDecoratedPotFieldsFromStack(this.previewStack);
            }
            syncNbtEditorValuesFromStack();
            return true;
        } catch (IllegalArgumentException exception) {
            if (updateStatus) {
                this.status = Component.literal(exception.getMessage());
            }
            return false;
        }
    }

    protected boolean tryApplyItemId(boolean throwOnError) {
        String idText = normalizeItemId(this.itemIdBox == null ? this.itemIdValue : this.itemIdBox.getValue());
        ResourceLocation id = ResourceLocation.tryParse(idText);
        Item item = id == null ? null : CompatRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR && !"minecraft:air".equals(idText)) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_item"), idText).getString());
            }
            return false;
        }

        this.itemIdValue = stripMinecraftNamespace(id);
        if (this.previewStack.is(item)) {
            return false;
        }

        boolean syncDefaultName = isNameFollowingDefault(this.previewStack);
        CompoundTag tag = ItemStackNbt.get(this.previewStack) == null ? null : ItemStackNbt.get(this.previewStack).copy();
        int count = this.previewStack.getCount() <= 0 ? 1 : this.previewStack.getCount();
        this.previewStack = new ItemStack(item, count);
        ItemStackNbt.set(this.previewStack, tag);
        if (syncDefaultName) {
            this.nameValue = getDefaultHoverName(this.previewStack);
            if (this.nameBox != null) {
                this.nameBox.setValue(this.nameValue);
            }
        }
        this.damageValue = Integer.toString(Math.min(getDamageMaxForField(this.previewStack), parsePositiveOrZero(this.damageValue)));
        tryApplyDamage(false);
        this.attributeSlot = getDefaultAttributeSlot(this.previewStack);
        this.colorHexValue = formatColorHex(getEditorColor());
        return true;
    }

    protected void tryApplyCount(boolean throwOnError) {
        String value = this.countBox == null ? this.countValue : this.countBox.getValue();
        if (value == null || value.isBlank()) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_count"), MAX_COUNT).getString());
            }
            return;
        }

        int count;
        try {
            count = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_count"), MAX_COUNT).getString());
            }
            return;
        }
        if (count < 1 || count > MAX_COUNT) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_count"), MAX_COUNT).getString());
            }
            return;
        }

        this.countValue = Integer.toString(count);
        this.previewStack.setCount(count);
    }

    protected void tryApplyDamage(boolean throwOnError) {
        String value = this.damageBox == null ? this.damageValue : this.damageBox.getValue();
        if (value == null || value.isBlank()) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_damage"), getDamageMaxForField(this.previewStack)).getString());
            }
            return;
        }

        int damage = Integer.parseInt(value);
        int maxDamage = getDamageMaxForField(this.previewStack);
        if (damage < 0 || damage > maxDamage) {
            if (throwOnError) {
                throw new IllegalArgumentException(Component.translatable(messageKey("editor_invalid_damage"), maxDamage).getString());
            }
            return;
        }

        this.damageValue = Integer.toString(damage);
        this.previewStack.setDamageValue(damage);
    }

    protected void applyNameToStack() {
        String value = this.nameBox == null ? this.nameValue : this.nameBox.getValue();
        this.nameValue = value;
        if (value == null || value.isBlank()) {
            ItemStackCompat.resetHoverName(this.previewStack);
            cleanupEmptyDisplayTag();
            return;
        }

        ItemStack withoutName = this.previewStack.copy();
        ItemStackCompat.resetHoverName(withoutName);
        if (!ItemStackCompat.hasCustomHoverName(this.previewStack)
                && LegacyText.stripCodes(value).equals(withoutName.getHoverName().getString())) {
            return;
        }

        ItemStackCompat.setHoverName(this.previewStack, LegacyText.parse(value));
    }

    protected void clearCustomName() {
        ItemStackCompat.resetHoverName(this.previewStack);
        this.nameValue = this.previewStack.getHoverName().getString();
        if (this.nameBox != null) {
            this.nameBox.setValue(this.nameValue);
        }
        cleanupEmptyDisplayTag();
    }

    protected void applyLoreToStack() {
        List<Component> lines = new ArrayList<>();
        for (String line : this.loreValues) {
            lines.add(line == null || line.isEmpty() ? Component.empty() : LegacyText.parse(line));
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).getString().isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        if (lines.isEmpty()) {
            this.previewStack.remove(DataComponents.LORE);
        } else {
            this.previewStack.set(DataComponents.LORE, new ItemLore(lines));
        }
    }

    protected void setLoreLine(int line, String value) {
        while (this.loreValues.size() <= line) {
            this.loreValues.add("");
        }
        this.loreValues.set(line, value);
    }

    protected void removeLoreLine(int line) {
        if (line >= 0 && line < this.loreValues.size()) {
            this.loreValues.remove(line);
            this.loreScroll = Mth.clamp(this.loreScroll, 0, Math.max(0, this.loreValues.size() - loreLineSpaces()));
            applyLoreToStack();
        }
    }

    protected void moveLoreLine(int line, int direction) {
        int other = line + direction;
        if (line < 0 || line >= this.loreValues.size() || other < 0 || other >= this.loreValues.size()) {
            return;
        }
        String value = this.loreValues.get(line);
        this.loreValues.set(line, this.loreValues.get(other));
        this.loreValues.set(other, value);
        applyLoreToStack();
        rebuildWidgets();
    }

    protected void copyLoreOnly() {
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(String.join("\n", this.loreValues));
        }
    }

    protected void copyFullTooltip() {
        if (this.minecraft != null && this.minecraft.player != null) {
            List<Component> tooltip = ItemStackCompat.getTooltipLines(this.previewStack, this.minecraft.player, this.minecraft.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            List<String> lines = new ArrayList<>();
            for (Component component : tooltip) {
                lines.add(component.getString());
            }
            this.minecraft.keyboardHandler.setClipboard(String.join("\n", lines));
        }
    }

    protected void pasteLore() {
        if (this.minecraft == null) {
            return;
        }
        String clipboard = this.minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }
        for (String line : clipboard.split("\\r?\\n")) {
            if (!line.startsWith(String.valueOf(ChatFormatting.PREFIX_CODE)) && line.length() > 1) {
                line = ChatFormatting.RESET.toString() + ChatFormatting.GRAY + line;
            }
            this.loreValues.add(line);
        }
        applyLoreToStack();
        rebuildWidgets();
    }
}
