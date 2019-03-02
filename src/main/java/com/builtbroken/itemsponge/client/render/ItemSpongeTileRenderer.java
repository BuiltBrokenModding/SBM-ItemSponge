package com.builtbroken.itemsponge.client.render;

import com.builtbroken.itemsponge.ModConfig.Options;
import com.builtbroken.itemsponge.sponge.block.TileEntityItemSponge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * @author p455w0rd
 */
@SuppressWarnings("deprecation")
public class ItemSpongeTileRenderer extends TileEntitySpecialRenderer<TileEntityItemSponge>
{

    @Override
    public void render(TileEntityItemSponge te, double xPos, double yPos, double zPos, float partialTicks, int destroyStage, float alpha)
    {
        if (!te.hasAbsorbedStack())
        {
            return;
        }
        ItemStack teInventoryStack = te.getAbsorbedStackWithCount();
        if (te.getWorld() != null && te.getWorld().isAirBlock(te.getPos().up()))
        {
            float dist = 32.0F;
            EntityPlayerSP entity = Minecraft.getMinecraft().player;
            Vec3d start = entity.getPositionEyes(partialTicks);
            Vec3d vec31 = entity.getLook(partialTicks);
            Vec3d end = start.add(vec31.x * dist, vec31.y * dist, vec31.z * dist);
            RayTraceResult ray = te.getWorld().rayTraceBlocks(start, end, false, true, false);
            if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.getBlockPos().equals(te.getPos()))
            {
                String info = teInventoryStack.isEmpty() ? I18n.translateToLocal("tile.itemsponge:nameplate.empty") : I18n.translateToLocal("tile.itemsponge:nameplate.count") + ": " + te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0).getCount();
                if (te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0).getCount() == Options.maxItemsPerSponge)
                {
                    info += TextFormatting.BOLD + "" + TextFormatting.RED + " (" + I18n.translateToLocal("item.itemsponge:tooltip.full") + ")";
                }
                setLightmapDisabled(true);
                EntityRenderer.drawNameplate(getFontRenderer(), info, (float) xPos + 0.5F, (float) yPos + 1.5F, (float) zPos + 0.5F, 0, rendererDispatcher.entityYaw, rendererDispatcher.entityPitch, false, false);
                setLightmapDisabled(false);
            }
        }

        ItemStack tmpStack = te.getAbsorbedStack();
        GlStateManager.pushMatrix();
        GlStateManager.translate(xPos, yPos, zPos);
        for (EnumFacing side : EnumFacing.VALUES)
        {
            BlockPos sidePos = te.getPos().offset(side);
            if (te.getWorld() != null && te.getWorld().getBlockState(sidePos).isSideSolid(getWorld(), sidePos, side.getOpposite()))
            {
                continue;
            }
            StackRenderUtil.renderStackOnSide(tmpStack, side);
        }
        GlStateManager.popMatrix();
    }

}
