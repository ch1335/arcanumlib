package com.chen1335.arcanumlib.compat.ali;

import com.chen1335.arcanumlib.ArcanumLib;
import com.yanny.aci.api.IWidget;
import com.yanny.aci.api.Rect;
import com.yanny.aci.api.RelativeRect;
import com.yanny.aci.api.WidgetDirection;
import com.yanny.aci.tooltip.CoreTooltipUtils;
import com.yanny.aci.tooltip.TooltipLine;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.IWidgetUtils;
import com.yanny.ali.compatibility.common.GenericUtils;
import com.yanny.ali.plugin.client.TooltipUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 把一条 summon_mob 条目画成【实体本身】（不是刷怪蛋、也不是刷怪笼）。
 *
 * <p>渲染直接交给 ALI 自己的 {@link GenericUtils#renderEntity}：它负责屏幕偏移、裁剪框与缩放，
 * 是 REI / JEI / EMI 三个界面共用的那套画法，自己手写坐标很容易画到屏幕外。
 */
public class SummonMobWidget implements IWidget {
    private final RelativeRect bounds;
    private final SummonMobNode node;
    private Entity shown;
    private boolean resolved;

    public SummonMobWidget(IWidgetUtils utils, IDataNode entry, RelativeRect rect, int ignoredMaxWidth) {
        this.node = (SummonMobNode) entry;
        this.bounds = rect;
        this.bounds.setDimensions(18, 18);
    }

    @Override
    @NotNull
    public RelativeRect getRect() {
        return this.bounds;
    }

    @Override
    @NotNull
    public WidgetDirection getDirection() {
        return WidgetDirection.HORIZONTAL;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Entity entity = this.entity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        GenericUtils.renderEntity(entity,
                new Rect(this.bounds.getX(), this.bounds.getY(), this.bounds.getWidth(), this.bounds.getHeight()),
                this.bounds.getWidth(), guiGraphics, mouseX, mouseY);
    }

    @Override
    @NotNull
    public List<TooltipLine> getTooltipLines(int mouseX, int mouseY) {
        return CoreTooltipUtils.toLines(this.node.getTooltip(), 0,
                Minecraft.getInstance().options.advancedItemTooltips,
                TooltipUtils.getStyle(), TooltipUtils.getNumberOptions());
    }

    /** 展示用实体只造一次；造不出来时只提醒一次，免得每帧刷屏。 */
    private Entity entity() {
        if (!this.resolved) {
            this.resolved = true;

            if (this.node.getMob() != null && Minecraft.getInstance().level != null) {
                this.shown = this.node.getMob().create(Minecraft.getInstance().level);
            }

            if (this.shown == null) {
                ArcanumLib.LOGGER.warn("Failed to create a display entity for summon_mob entry: {}",
                        this.node.getMob());
            }
        }

        return this.shown;
    }
}