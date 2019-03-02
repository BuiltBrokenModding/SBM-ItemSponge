package com.builtbroken.itemsponge.sponge.item;

import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.client.render.ItemLayerWrapper;
import com.builtbroken.itemsponge.sponge.ItemSpongeItemHandler;
import com.builtbroken.itemsponge.sponge.ItemSpongeUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author p455w0rd
 */
@SuppressWarnings("deprecation")
public class ItemBlockItemSponge extends ItemBlock
{
    private static final Map<ItemStack, ItemSpongeItemHandler> ITEMHANDLER_CACHE = new HashMap<>();

    @SideOnly(Side.CLIENT)
    ItemLayerWrapper wrappedModel0, wrappedModel1;

    public ItemBlockItemSponge(Block block)
    {
        super(block);
        setRegistryName(block.getRegistryName());
        setHasSubtypes(true);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
    {
        return new ICapabilityProvider()
        {

            @Override
            public boolean hasCapability(Capability<?> capability, EnumFacing facing)
            {
                return ItemSpongeUtils.hasAbsorbedItem(stack) && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, EnumFacing facing)
            {
                if (ItemSpongeUtils.hasAbsorbedItem(stack) && hasCapability(capability, facing))
                {
                    return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getItemHandler(stack));
                }
                return null;
            }

        };
    }

    private ItemSpongeItemHandler getItemHandler(ItemStack sponge)
    {
        if (!ITEMHANDLER_CACHE.containsKey(sponge))
        {
            ItemSpongeItemHandler itemHandler = new ItemSpongeItemHandler();
            itemHandler.deserializeNBT(ItemSpongeUtils.getSerializedItemHandlerFromSponge(sponge));
            ITEMHANDLER_CACHE.put(sponge, itemHandler);
        }
        return ITEMHANDLER_CACHE.get(sponge);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (isInCreativeTab(tab))
        {
            items.add(new ItemStack(ItemSpongeMod.item, 1, 0));
        }
    }

    @Override
    public int getDamage(ItemStack stack)
    {
        return ItemSpongeUtils.hasAbsorbedItem(stack) ? 1 : 0;
    }

    @Override
    public int getMetadata(ItemStack stack)
    {
        return getDamage(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
    {
        if (ItemSpongeUtils.hasAbsorbedItem(stack))
        {
            tooltip.add(I18n.translateToLocal("item.itemsponge:tooltip.absorbing") + " " + TextFormatting.BLUE + "" + TextFormatting.ITALIC + ItemSpongeUtils.getAbsorbedItem(stack).getDisplayName());
            int count = ItemSpongeUtils.getAbsorbedItemCount(stack);
            String countTooltip = I18n.translateToLocal("tile.itemsponge:nameplate.count") + ": " + (count < 1 ? I18n.translateToLocal("tile.itemsponge:nameplate.empty") : count);
            if (ItemSpongeUtils.isFull(stack))
            {
                countTooltip += TextFormatting.BOLD + "" + TextFormatting.RED + " (" + I18n.translateToLocal("item.itemsponge:tooltip.full") + ")";
            }
            tooltip.add(countTooltip);
            if (count > 0)
            {
                tooltip.add(I18n.translateToLocal("item.itemPsonge:tooltip.howtouncraft1"));
                tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("item.itemPsonge:tooltip.howtouncraft2"));
            }
        }
        else
        {
            tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("item.itemsponge:tooltip.howtocraft"));
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
        // We don't allow placing of an Item Sponge block that has no item set
        if (!ItemSpongeUtils.hasAbsorbedItem(stack) || !world.setBlockState(pos, newState, 11))
        {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == block)
        {
            block.onBlockPlacedBy(world, pos, state, player, stack);
            ItemSpongeUtils.setTileNBT(world, player, pos, stack);
            if (player instanceof EntityPlayerMP)
            {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
        }
        return true;
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, World world)
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    public ModelResourceLocation getModelResource(boolean containsItem)
    {
        return new ModelResourceLocation(getRegistryName(), "contains_item=" + containsItem);
    }

    @SideOnly(Side.CLIENT)
    public ItemLayerWrapper getWrappedModel(boolean hasItem)
    {
        return hasItem ? wrappedModel1 : wrappedModel0;
    }

    @SideOnly(Side.CLIENT)
    public void setWrappedModel(ItemLayerWrapper wrappedModel, boolean hasItem)
    {
        if (!hasItem)
        {
            wrappedModel0 = wrappedModel;
            return;
        }
        wrappedModel1 = wrappedModel;
    }

}
