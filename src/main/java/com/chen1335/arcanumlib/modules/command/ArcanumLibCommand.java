package com.chen1335.arcanumlib.modules.command;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorApplier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import java.util.Optional;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * ArcanumLib 的指令根：{@code /arcanumlib}。
 *
 * <ul>
 *     <li>{@code /arcanumlib export lootTable} —— 导出战利品表（modified / original 两份）；</li>
 *     <li>{@code /arcanumlib loot_simulation <lootTable> [次数]} —— 反复生成同一张表并统计产出（移植自 LegendaryRelics）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public final class ArcanumLibCommand {

    /** 导出根目录名（相对游戏目录）。 */
    private static final String EXPORT_DIR = "arcanumlib/exportedLootTable";
    /** 模拟的默认次数与上限。 */
    private static final int DEFAULT_TIMES = 1000;
    private static final int MAX_TIMES = 100000;
    /** 看向探测距离（方块与实体共用）。 */
    private static final double LOOK_DISTANCE = 8.0D;
    /** 百分比列的固定宽度，100.00 加上百分号正好占 7 个西文字宽。 */
    private static final int WIDTH_PERCENT = 7;

    private ArcanumLibCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ArcanumLib.MODID)
                // 导出与模拟都涉及全量战利品表读写，按原版惯例要求权限等级 2
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .then(Commands.literal("lootTable")
                                .executes(context -> exportLootTable(context.getSource()))))
                .then(Commands.literal("loot_simulation")
                        .then(Commands.argument("lootTable", ResourceLocationArgument.id())
                                .suggests(ArcanumLibCommand::suggestLootTables)
                                .executes(context -> simulate(context.getSource(),
                                        ResourceLocationArgument.getId(context, "lootTable"), DEFAULT_TIMES, null))
                                .then(Commands.argument("times", IntegerArgumentType.integer(1, MAX_TIMES))
                                        .executes(context -> simulate(context.getSource(),
                                                ResourceLocationArgument.getId(context, "lootTable"),
                                                IntegerArgumentType.getInteger(context, "times"), null))
                                        .then(Commands.argument("lootContextParamSet", ResourceLocationArgument.id())
                                                .suggests(ArcanumLibCommand::suggestParamSets)
                                                .executes(context -> simulate(context.getSource(),
                                                        ResourceLocationArgument.getId(context, "lootTable"),
                                                        IntegerArgumentType.getInteger(context, "times"),
                                                        ResourceLocationArgument.getId(context, "lootContextParamSet"))))))));
    }

    // ------------------------------------------------------------------ 导出

    private static int exportLootTable(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Path root = FMLPaths.GAMEDIR.get().resolve(EXPORT_DIR);

        try {
            int modified = exportModified(server, root.resolve("modified"));
            int original = exportOriginal(server, root.resolve("original"));

            ArcanumLib.LOGGER.info("Exported {} modified and {} original loot tables to {}", modified, original, root);
            source.sendSuccess(() -> Component.translatable("arcanumlib.command.export.success",
                    String.valueOf(modified), String.valueOf(original), root.toString()), true);

            return modified + original;
        } catch (IOException e) {
            ArcanumLib.LOGGER.error("Failed to export loot tables to {}", root, e);
            source.sendFailure(Component.translatable("arcanumlib.command.export.failed", String.valueOf(e.getMessage())));

            return 0;
        }
    }

    /** 序列化内存里的（已改过的）表；结构是 {@code <命名空间>/<路径>.json}。 */
    private static int exportModified(MinecraftServer server, Path root) throws IOException {
        HolderLookup.RegistryLookup<LootTable> lookup = server.reloadableRegistries().get()
                .lookup(Registries.LOOT_TABLE).orElse(null);

        if (lookup == null) {
            ArcanumLib.LOGGER.error("Cannot resolve the loot table registry, skipping the modified export");

            return 0;
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // 只导出"被注入器改过"的那批表：按实例身份比对，口径与 LootInjectorApplier 的已应用记录一致
        Set<LootTable> applied = LootInjectorApplier.appliedTables();
        int count = 0;

        for (Holder.Reference<LootTable> holder : lookup.listElements().toList()) {
            if (!applied.contains(holder.value())) {
                continue;
            }

            ResourceLocation id = holder.key().location();
            JsonElement json = LootTable.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow();
            Path target = root.resolve(id.getNamespace()).resolve(id.getPath() + ".json");

            Files.createDirectories(target.getParent());
            Files.writeString(target, gson.toJson(json) + "\n", StandardCharsets.UTF_8);
            count++;
        }

        return count;
    }

    /** 直接把数据包里的原始文件复制出来，保持 {@code <命名空间>/loot_table/<路径>.json} 的原样结构。 */
    private static int exportOriginal(MinecraftServer server, Path root) throws IOException {
        Map<ResourceLocation, Resource> found = server.getResourceManager()
                .listResources("loot_table", id -> id.getPath().endsWith(".json"));
        int count = 0;

        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation id = entry.getKey();

            // listResources("loot_table", ...) 的返回值有的版本带起始目录、有的不带，这里统一补上
            String path = id.getPath().startsWith("loot_table/") ? id.getPath() : "loot_table/" + id.getPath();
            Path target = root.resolve(id.getNamespace()).resolve(path);

            Files.createDirectories(target.getParent());

            try (InputStream in = entry.getValue().open()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            count++;
        }

        return count;
    }

    // -------------------------------------------------------------- 战利品模拟

    /**
     * 照着原版 loot 指令用 suggestResource：它会同时给出带命名空间和不带的两种写法，
     * 否则输入 desert 时匹配不到 minecraft:chests/desert_pyramid，补全就出不来了。
     */
    private static CompletableFuture<Suggestions> suggestLootTables(CommandContext<CommandSourceStack> context,
                                                                   SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                context.getSource().getServer().reloadableRegistries().get()
                        .registryOrThrow(Registries.LOOT_TABLE).keySet(),
                builder);
    }

    /** 原版参数集 id（没有注册表可枚举，这里是 LootContextParamSets 里的全部 16 个）。 */
    /** 原版参数集 id（没有注册表可枚举，这里是 LootContextParamSets 里的全部 2 个）。 */
    /** 原版参数集 id（没有注册表可枚举，这里是 LootContextParamSets 里的全部 23 个）。 */
    private static final List<String> PARAM_SETS = List.of(
            "minecraft:empty", "minecraft:chest", "minecraft:command", "minecraft:selector",
            "minecraft:fishing", "minecraft:target", "minecraft:equipment", "minecraft:archaeology",
            "minecraft:gift", "minecraft:barter", "minecraft:vault", "minecraft:advancement_reward",
            "minecraft:advancement_entity", "minecraft:advancement_location", "minecraft:block_use", "minecraft:generic",
            "minecraft:block", "minecraft:shearing", "minecraft:enchanted_damage", "minecraft:enchanted_item",
            "minecraft:enchanted_location", "minecraft:enchanted_entity", "minecraft:hit_block");

    /** 参数集补全。 */
    private static CompletableFuture<Suggestions> suggestParamSets(CommandContext<CommandSourceStack> context,
                                                                   SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PARAM_SETS, builder);
    }
    /** 用手持物品当 TOOL，让依赖工具的战利品表（算工具附魔/时运那类）也能正常模拟。 */
    private static ItemStack toolOf(CommandSourceStack source) {
        Entity entity = source.getEntity();

        return entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
    }

    /** 玩家看向的方块；超出距离或没看方块时返回 null。 */
    private static BlockPos lookedBlock(Player player) {
        HitResult hit = player.pick(LOOK_DISTANCE, 1.0F, false);

        return hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
    }

    /** 玩家看向的实体：沿视线找最近的、可被选中的实体。 */
    private static Entity lookedEntity(Player player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(LOOK_DISTANCE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(LOOK_DISTANCE)).inflate(1.0D);
        Entity best = null;
        double bestDistance = LOOK_DISTANCE * LOOK_DISTANCE;

        for (Entity candidate : player.level().getEntities(player, box,
                entity -> entity.isPickable() && !entity.isSpectator())) {
            AABB hitBox = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Optional<Vec3> hit = hitBox.clip(eye, end);

            if (hit.isPresent()) {
                double distance = eye.distanceToSqr(hit.get());

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            } else if (hitBox.contains(eye)) {
                bestDistance = 0.0D;
                best = candidate;
            }
        }

        return best;
    }
    /**
     * 反复生成同一张战利品表，按物品类型统计后输出「物品 | 数量 | 比例 | 概率」。
     *
     * <p>用带种子的 {@code getRandomItems} 重载，每轮换新种子，避免复用同一份 LootParams 导致每轮结果相同。
     */
    /**
     * 反复生成同一张战利品表，按物品类型统计后输出「物品 | 数量 | 比例 | 概率」。
     *
     * <p>上下文按 {@code lootContextParamSet} 的类型填充（省略时用表自带的参数集）：
     * <ul>
     *     <li><b>方块类</b>（含 chest）：{@code ORIGIN} 用玩家看向的方块，{@code THIS_ENTITY} / {@code ATTACKING_ENTITY} 用玩家；</li>
     *     <li><b>实体类</b>（如 target）：{@code THIS_ENTITY} 用玩家看向的实体、{@code ORIGIN} 用它的位置，
     *         {@code DAMAGE_SOURCE} 用玩家攻击，攻击者三个参数都归玩家；</li>
     *     <li><b>通用类</b>（generic）：看向实体则按实体填，否则按方块填，都没有就用玩家脚下；</li>
     *     <li>其余（fishing / barter / gift 等）：{@code THIS_ENTITY} 就是玩家本人。</li>
     * </ul>
     * 每个参数都会先问 {@code paramSet.isAllowed} 再填，避免把表不接受的参数塞进去。
     * 每轮用新随机种子，避免复用同一份 LootParams 导致每轮结果相同。
     */
    private static int simulate(CommandSourceStack source, ResourceLocation tableId, int times,
                                ResourceLocation paramSetId) {
        ServerLevel level = source.getLevel();
        LootTable table = level.getServer().reloadableRegistries().get()
                .registryOrThrow(Registries.LOOT_TABLE).get(tableId);

        if (table == null) {
            source.sendFailure(Component.translatable("arcanumlib.command.simulate.not_found", tableId.toString()));

            return 0;
        }

        LootContextParamSet paramSet;

        if (paramSetId == null) {
            paramSet = table.getParamSet();
        } else {
            // 参数集没有注册表，只有 LootContextParamSets.CODEC，用 json 的字符串形式解析
            paramSet = LootContextParamSets.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(paramSetId.toString()))
                    .result().orElse(null);
        }

        if (paramSet == null) {
            source.sendFailure(Component.translatable("arcanumlib.command.simulate.param_set_not_found",
                    String.valueOf(paramSetId)));

            return 0;
        }

        boolean blockKind = paramSetId != null && paramSetId.getPath().equals("chest")
                || paramSet.isAllowed(LootContextParams.BLOCK_STATE)
                || paramSet.isAllowed(LootContextParams.BLOCK_ENTITY);
        // 实体掉落类：既允许 THIS_ENTITY 又允许 DAMAGE_SOURCE（target 与 generic），
        // 这样 fishing / barter 这类"自己要实体但不需要伤害来源"的表不会被误判
        boolean entityKind = paramSet.isAllowed(LootContextParams.THIS_ENTITY)
                && paramSet.isAllowed(LootContextParams.DAMAGE_SOURCE);

        Player player = source.getPlayer();
        LootParams.Builder builder = new LootParams.Builder(level);
        Vec3 origin = source.getPosition();
        String kind = "generic";

        if (player != null) {
            origin = player.position();
            BlockPos pos = lookedBlock(player);
            Entity looked = lookedEntity(player);
            // generic 同时具备两类参数：看向实体优先，其次方块，最后玩家脚下
            boolean useEntity = entityKind && looked != null && (!blockKind || pos == null || looked != null);
            boolean useBlock = blockKind && pos != null && !(entityKind && looked != null);

            if (useEntity) {
                origin = looked.position();
                builder.withParameter(LootContextParams.THIS_ENTITY, looked);
                kind = "target";
            } else if (useBlock) {
                origin = Vec3.atCenterOf(pos);

                if (paramSet.isAllowed(LootContextParams.BLOCK_STATE)) {
                    builder.withParameter(LootContextParams.BLOCK_STATE, level.getBlockState(pos));
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);

                if (blockEntity != null && paramSet.isAllowed(LootContextParams.BLOCK_ENTITY)) {
                    builder.withParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
                }

                kind = "block";
            }
        }

        if (paramSet.isAllowed(LootContextParams.ORIGIN)) {
            builder.withParameter(LootContextParams.ORIGIN, origin);
        }

        if (paramSet.isAllowed(LootContextParams.TOOL)) {
            builder.withParameter(LootContextParams.TOOL, toolOf(source));
        }

        if (player != null) {
            // 掉落类上下文：攻击者 / 直接攻击者 / 最后伤害玩家 / 伤害来源 都归到玩家
            if (paramSet.isAllowed(LootContextParams.ATTACKING_ENTITY)) {
                builder.withParameter(LootContextParams.ATTACKING_ENTITY, player);
            }

            if (paramSet.isAllowed(LootContextParams.DIRECT_ATTACKING_ENTITY)) {
                builder.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player);
            }

            if (paramSet.isAllowed(LootContextParams.LAST_DAMAGE_PLAYER)) {
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            }

            if (paramSet.isAllowed(LootContextParams.DAMAGE_SOURCE)) {
                builder.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player));
            }

            // 实体类已经指向"看向的实体"，其余情况 THIS_ENTITY 就是玩家本人
            if (paramSet.isAllowed(LootContextParams.THIS_ENTITY) && !"target".equals(kind)) {
                builder.withParameter(LootContextParams.THIS_ENTITY, player);
            }
        }

        LootParams params;

        try {
            params = builder.create(paramSet);
        } catch (Exception e) {
            source.sendFailure(Component.translatable("arcanumlib.command.simulate.need_context",
                    String.valueOf(e.getMessage())));

            return 0;
        }

        Map<Item, Integer> counts = new HashMap<>();
        long totalCount = 0L;

        for (int i = 0; i < times; i++) {
            for (ItemStack stack : table.getRandomItems(params, level.getRandom().nextLong())) {
                if (stack.isEmpty()) {
                    continue;
                }

                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                totalCount += stack.getCount();
            }
        }

        long totalItems = totalCount;
        // lambda 只能捕获 effectively final，转存一份
        String kindLabel = kind;
        source.sendSuccess(() -> Component.translatable("arcanumlib.command.simulate.header",
                String.valueOf(times), tableId.toString(), String.valueOf(totalItems), kindLabel), false);

        List<Map.Entry<Item, Integer>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed())
                .toList();
        int countWidth = String.valueOf(sorted.isEmpty() ? 0 : sorted.getFirst().getValue()).length();
        int nameWidth = sorted.stream()
                .mapToInt(entry -> width(entry.getKey().getDescription().getString()))
                .max().orElse(0) + 2;

        // 列名走本地化：文字由客户端翻译，补位空格按服务端解析出的文本算（两者宽度一般一致）
        MutableComponent itemLabel = Component.translatable("arcanumlib.command.simulate.column.item");
        MutableComponent countLabel = Component.translatable("arcanumlib.command.simulate.column.count");
        MutableComponent ratioLabel = Component.translatable("arcanumlib.command.simulate.column.ratio");
        MutableComponent probabilityLabel = Component.translatable("arcanumlib.command.simulate.column.probability");

        source.sendSuccess(() -> padRight(itemLabel, nameWidth)
                .append(padLeft(countLabel, countWidth))
                .append("  ").append(padLeft(ratioLabel, WIDTH_PERCENT))
                .append("  ").append(padLeft(probabilityLabel, WIDTH_PERCENT)), false);

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<Item, Integer> entry = sorted.get(i);
            double ratio = totalItems == 0 ? 0.0D : entry.getValue() * 100.0D / totalItems;
            double probability = entry.getValue() * 100.0D / times;
            String name = entry.getKey().getDescription().getString();
            int color = colorOf(i);

            // 颜色不占字形宽度，所以补位空格紧跟名字，对齐不受影响
            String rest = " ".repeat(Math.max(0, nameWidth - width(name)))
                    + padLeft(String.valueOf(entry.getValue()), countWidth)
                    + "  " + padLeft(String.format(Locale.ROOT, "%.2f%%", ratio), WIDTH_PERCENT)
                    + "  " + padLeft(String.format(Locale.ROOT, "%.2f%%", probability), WIDTH_PERCENT);

            Component line = Component.empty()
                    .append(Component.literal(name).withStyle(style -> style.withColor(color)))
                    .append(rest);
            source.sendSuccess(() -> line, false);
        }

        return counts.size();
    }
    private static int colorOf(int index) {
        float hue = (index * 0.618033988749895F) % 1.0F;

        return Mth.hsvToRgb(hue, 0.65F, 0.95F);
    }

    /** 左侧补空格（组件版）：宽度按服务端解析出的文本算。 */
    private static MutableComponent padLeft(Component text, int width) {
        return Component.literal(" ".repeat(Math.max(0, width - width(text.getString())))).append(text);
    }

    /** 右侧补空格（组件版）。 */
    private static MutableComponent padRight(Component text, int width) {
        return text.copy().append(" ".repeat(Math.max(0, width - width(text.getString()))));
    }
    /** 中文字形约为西文两倍宽，按显示宽度补空格才能真正竖着对齐。 */
    private static int width(String text) {
        int count = 0;

        for (int i = 0; i < text.length(); i++) {
            count += text.charAt(i) > 0x2E80 ? 2 : 1;
        }

        return count;
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(0, width - width(text)));
    }

    private static String padLeft(String text, int width) {
        return " ".repeat(Math.max(0, width - width(text))) + text;
    }
}
