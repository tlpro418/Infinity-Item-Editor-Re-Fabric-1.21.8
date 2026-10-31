# 移植说明：Fabric 1.21.4 → Fabric 1.21.8

本工作区以 `fabric-1.21.4` 分支为基底（该分支包含 Fabric 的入口点、Fabric API 创造标签页、
`fabric.mod.json` 与 split source set 布局），逐项适配到 Minecraft 1.21.8。
Forge 1.20.1 的 `master` 仅用于对照功能行为。

## 版本

| 项 | 旧 | 新 |
| --- | --- | --- |
| Minecraft | 1.21.4 | 1.21.8 |
| Fabric Loader | 0.19.3 | 0.20.1 |
| Fabric API | 0.119.4+1.21.4 | 0.136.1+1.21.8 |
| Fabric Loom | 1.10.5 | 1.13.6 |
| Gradle | 8.14 (services.gradle.org) | 8.14.3（腾讯镜像） |
| Java | 21 | 21 |

工程中 `gradle.properties` 的 `org.gradle.java.home` 指向 `C:/Program Files/Java/jdk-21.0.11`，
因为本机默认 `java` 为 26，而 Gradle 8.14 不支持该版本。

## 主要 API 迁移

### 1. NBT 读取：`Optional` 化

1.21.5 起 `CompoundTag` / `ListTag` 的读取方法返回 `Optional`，并移除了 `getX(String, int)`、
`contains(String, int)`、`getAllKeys()`、`getAsString()`、`getAsNumber()` 等：

- `getString(k)` → `getStringOr(k, "")`；`getInt` → `getIntOr`；`getByte` → `getByteOr` …
- `getCompound(k)` → `getCompoundOrEmpty(k)`；`getList(k, type)` → `getListOrEmpty(k)`
- `contains(k, type)` → 新增 `util/NbtCompat.contains(tag, key, type)` 保留类型判定
- `getAllKeys()` → `keySet()`；`StringTag.getAsString()` → `value()`；`NumericTag.getAsNumber()` → `box()`
- `TagParser.parseTag` → `TagParser.parseCompoundFully`
- UUID 读写使用 `NbtCompat.hasUUID / getUUID / putUUID`（基于 int 数组）

### 2. `CompoundTag` / `ListTag` 变为 `final`

`ItemStackNbt` 原先通过继承这两个类实现「NBT 改动实时写回物品」。1.21.8 中类为 `final`，
因此改为：

- `ListeningMap`（`HashMap` 子类）与 `ListeningList`（`ArrayList` 子类）拦截写入；
- 反射调用包私有构造 `CompoundTag(Map)`、`ListTag(List)`，让 NBT 对象直接以监听容器作为存储；
- 任何写入都会触发 `applyLegacyTag(stack, root)`，实现与旧版一致的自动同步。

该方案已在无头探针中验证：根键写入、嵌套 `display` 写入、以及回写物品均生效。

### 3. 隐藏标签（HideFlags）

1.21.5 移除了各组件上的 `show_in_tooltip` 与 `minecraft:hide_additional_tooltip`。
现统一写入 `minecraft:tooltip_display` 的 `hidden_components`（组件 ID 列表），读取时反向映射：

- 1 → `minecraft:enchantments`
- 2 → `minecraft:attribute_modifiers`
- 4 → `minecraft:unbreakable`
- 8 → `minecraft:can_break`
- 16 → `minecraft:can_place_on`
- 64 → `minecraft:dyed_color`
- 128 → `minecraft:trim`
- 32 → `minecraft:potion_contents`（旧 `hide_additional_tooltip` 的近似映射）

### 4. 物品序列化

`ItemStack.saveOptional` / `ItemStack.parseOptional` 已移除，改用
`ItemStack.OPTIONAL_CODEC` + `provider.createSerializationContext(NbtOps.INSTANCE)`。

### 5. GUI 渲染

- `GuiGraphics` 构造改为 `(Minecraft, GuiRenderState)`，`pose()` 返回 `Matrix3x2fStack`：
  `pushPose/popPose` → `pushMatrix/popMatrix`，`translate(x,y,z)` → `translate(x,y)`，
  `scale(x,y,1)` → `scale(x,y)`，`mulPose(Axis.ZN.rotationDegrees(d))` → `rotate(radians)`。
- `renderTooltip` → `setTooltipForNextFrame`；`renderComponentTooltip` → `setComponentTooltipForNextFrame`。
- `drawString` 返回 `void`（原先返回结束坐标），光标位置改用 `font.width` 计算。
- `blit(RenderType::guiTextured, …)` → `blit(RenderPipelines.GUI_TEXTURED, …)`。
- `GuiGraphics.flush()` 移除。

### 6. 实体与物品栏

- `ArmorItem` 移除 → `stack.get(DataComponents.EQUIPPABLE) != null && equippable.slot().isArmor()`
- `Inventory.items/armor/offhand/selected` 私有化 → `getItem/setItem/setSelectedSlot`，
  装备槽改用 `LivingEntity#setItemSlot` / `getItemBySlot`
- `Player#getHandSlots/getArmorSlots` 移除 → 遍历 `Inventory` 与 `EquipmentSlot`
- `new Inventory(player)` → `new Inventory(player, new EntityEquipment())`
- `DyedItemColor(int, boolean)` → `DyedItemColor(int)`
- `AttributeModifier#save()` 移除 → `AttributeModifier.CODEC.encodeStart(NbtOps.INSTANCE, …)`

### 7. 文本与方块实体

- `Component.Serializer` 移除 → `ComponentSerialization.CODEC` + `JsonOps`（`util/ComponentCompat`）
- `ClickEvent` / `HoverEvent` 改为 record：`new ClickEvent.RunCommand(cmd)`、
  `clickEvent instanceof ClickEvent.RunCommand rc`、`HoverEvent.ShowItem#item()`
- `BlockEntity#saveCustomOnly/removeComponentsFromTag` 与 `BlockItem#setBlockEntityData`
  改为接收 `TagValueOutput`
- `ContainerHelper#loadAllItems/saveAllItems` 改为 `ValueInput` / `ValueOutput`
  （使用 `TagValueInput.create` / `TagValueOutput.createWithContext`）

## 1.6.7 修复记录（回滚 WLProtocol 同步）

- `LegacyPluginCompat.sync` 改为由系统属性 `infinity_item_editor_re.syncLegacyPluginData` 控制，默认 **false**。
  现场反馈：该同步会让部分服务端把物品判定为被篡改并整体重置。
- `SaveVerifier` 增加可点击的诊断消息（`ClickEvent.CopyToClipboard`），一键复制 `sent=` / `received=`。

## 1.6.5 修复记录（WLProtocol 服务器字段）

- 新增 `util/LegacyPluginCompat`：当物品 `custom_data` 中存在 `WLProtocol*` 键时，把编辑后的名称/Lore 写回
  `display.Name` / `display.Lore` 以及所有以 `Name` / `Lore` 结尾的键；根据原值是否为 JSON 决定写纯文本还是 JSON，保留 `color`、协议标记等其他字段。
- `ItemEditorScreenActions` 在保存前调用该同步；随后仍由 `SaveVerifier` 校验服务端回显。

## 1.6.4 修复记录（保存诊断）

- 新增 `util/SaveVerifier`：保存时记录发送的 NBT，由 `ClientEvents.onClientTick` 在 60 tick 内比对槽位，
  不依赖编辑器是否仍然打开。
- 不一致时输出 `sent=` / `received=` 到日志，聊天栏提示，并复制 `/give` 作为兜底。
- 用于区分“输入没进去 / 客户端没写对 / 服务端改写”三种情况，特别是非 ASCII（中文）文本的问题定位。

## 1.6.3 修复记录（服务端兼容）

- `LegacyText.parse` 不再生成 `{text:"",extra:[...]}`，而是以第一段文本作为根组件（`{text:"...",color:...}`），多段时才用 `extra`。这更接近原版 `/give` 生成的组件，降低 ViaVersion/插件转换时丢字段的概率。
- 保存后把发送的 NBT 记录下来，在后续 40 tick 内比对服务端同步回来的槽位内容；不一致时提示玩家并在日志输出 `the server rewrote the saved item`，用于区分「客户端没写对」与「服务端改写」。
- 端到端验证：本地 Fabric 1.21.8 服务端 + 客户端自动进服，保存后服务端回显的组件与发送值一致（名称、两行 Lore、颜色全部保留）。

## 1.6.2 修复记录（保存损坏物品）

- `applyLoreToStack` 直接 `set/remove(DataComponents.LORE)`；`applyNameToStack` 早已直接写 `CUSTOM_NAME`。
  旧实现通过 `ItemStackNbt.getOrCreate` 修改 `display`，同步回写时会**重写 `custom_data`**，把服务器插件存放的数据抹掉，插件随后把物品重置成普通物品。
- `cleanupEmptyTag` / `cleanupEmptyDisplayTag` 置空，避免 `ItemStackNbt.set(stack, null)` 把名称、Lore、附魔等已映射组件一起清掉。
- 保存前 `restoreLostComponents()`：若原物品的受保护组件（`custom_data`、附魔、属性、纹饰、方块实体数据等）在编辑后消失，自动补回；单个组件消失视为用户有意清除，两个及以上视为转换丢失。
- 保存时输出最终 NBT 到日志，便于判断是客户端丢数据还是服务器二次改写。

## 1.6.1 修复记录（1.21.8 实际差异）

首版 1.6.0 能编译、能加载，但实测发现三个问题，均已在 1.6.1 修复：

1. **组件 NBT 结构与 1.21.4 不同**（导致整份 `components` 解析失败，回退为 `custom_data`，物品不显示名称/Lore/颜色）
   - `minecraft:dyed_color`：`4095`（整数），不是 `{rgb:4095}`
   - `minecraft:custom_model_data`：`{floats:[5.0f]}`（可带 flags/strings/colors），不是整数
   - `minecraft:can_break` / `can_place_on`：`AdventureModePredicate`，单个条件编码为 `{blocks:"id"}`，多个为 `[{blocks:...},...]`，不是 `{predicates:[...]}`
   - `minecraft:attribute_modifiers`：直接是修饰符数组，不是 `{modifiers:[...]}`
   - `minecraft:enchantments` / `stored_enchantments`：直接是 `{"id":等级}` map，不是 `{levels:{...}}`
   - 兜底策略也改了：整包解析失败时逐个组件尝试解析，能用的组件照常写入，其余留在 `custom_data`
2. **`§` 颜色码不再生效**：1.21.5+ 把字符串组件当作纯文本，`§6` 只是两个字面字符。新增 `util/LegacyText`：
   - 写入时把 `§` 码（颜色/粗体/斜体/下划线/删除线/乱码）解析为 `Style`，编码成真正的组件
   - 读取时把「简单组件」反向还原成 `§` 码，编辑器输入框继续按旧习惯显示与编辑
   - 带 click/hover/insertion/自定义字体的复杂组件不还原，避免丢失信息
3. **编辑器 tooltip 预览不显示**：1.21.6+ 的 `setTooltipForNextFrame` 把待绘制 tooltip 存进 `GuiGraphics` 的私有字段 `deferredTooltip`，只有该实例调用 `renderDeferredTooltip()` 才会画；编辑器缩放时使用派生的 `EditorGuiGraphics`，父级 `Screen.renderWithTooltip` 不会 flush 它。
   - 新增 `util/Tooltips`，用 `GuiGraphics#renderTooltip` 立即绘制，恢复 1.21.4 的即时 tooltip 行为

## 验证情况

- `gradlew build` 通过，生成 remapped jar。
- Mixin 目标已对照 1.21.8 类逐一确认存在：
  `AbstractContainerScreen#hoveredSlot / keyPressed / mouseClicked`、
  `Connection#channel`、`CreativeModeInventoryScreen#selectedTab /
  refreshCurrentTabContents(Collection) / selectTab`。
- 无头探针验证了 `ItemStackNbt` 的写入同步、嵌套标签写入、隐藏标签映射、现代 `components` 解析。
- 在真实开发客户端（`gradlew runClient`，Minecraft 1.21.8 + Fabric Loader 0.20.1）中启动成功：
  模组正常加载、Mixin 全部应用、标题界面正常显示。
- 通过临时的面板轮播冒烟测试依次打开并渲染了全部 22 个 `Panel`
  （ITEM / NBT / COMPONENTS / NBT_ADVANCED / HIDE_FLAGS / ENCHANTMENTS / POTION /
  ATTRIBUTES / COLOR / SIGN / HEAD / ARMOR_STAND / FIREWORK / CONTAINER / BANNER /
  DECORATED_POT / SPAWN_EGG / TRADES / TRADE / BOOK / LORE / LORE_PAINTER），
  循环 300+ 次切换，无异常、无崩溃；物品图标、文本、按钮与列表均正常绘制。
- 冒烟测试代码未包含在最终 jar 中（最终 jar 已确认不含任何 `*Smoke*` / `*Probe*` 类）。
- 仍需真机点验的部分：实际写入物品栏/服务器交互、创造模式标签页在真实存档中的行为、
  各专用面板的交互细节（本次只验证了渲染与状态切换，未覆盖所有按钮回调）。

