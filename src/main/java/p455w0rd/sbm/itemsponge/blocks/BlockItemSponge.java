package p455w0rd.sbm.itemsponge.blocks;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import p455w0rd.sbm.itemsponge.blocks.tiles.TileItemSponge;
import p455w0rd.sbm.itemsponge.init.ModGlobals;
import p455w0rd.sbm.itemsponge.init.ModItems;
import p455w0rd.sbm.itemsponge.inventories.ItemSpongeItemHandler;

/**
 * @author p455w0rd
 *
 */
public class BlockItemSponge extends Block implements ITileEntityProvider {

	static final ResourceLocation ITEM_SPONGE_REGISTRYNAME = new ResourceLocation(ModGlobals.MODID, "item_sponge");
	static final PropertyBool HASITEM = PropertyBool.create("contains_item");

	public BlockItemSponge() {
		super(Material.SPONGE);
		setRegistryName(ITEM_SPONGE_REGISTRYNAME);
		setUnlocalizedName(ITEM_SPONGE_REGISTRYNAME.toString());
		setHardness(0.6F);
		setSoundType(SoundType.PLANT);
		setDefaultState(blockState.getBaseState().withProperty(HASITEM, false));
		setCreativeTab(CreativeTabs.MISC);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
			TileEntity te = world.getTileEntity(pos);
			if (te != null && te instanceof TileItemSponge) {
				TileItemSponge tile = (TileItemSponge) te;
				if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
					ItemSpongeItemHandler itemHandler = (ItemSpongeItemHandler) tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
					ItemStack slotStack = itemHandler.getStackInSlot(0);
					if (!slotStack.isEmpty()) {
						int amount = 1;
						if (player.isSneaking()) {
							amount = Math.min(slotStack.getMaxStackSize(), slotStack.getCount());
						}
						ItemStack pulledStack = itemHandler.extractItem(0, amount, false);
						if (!pulledStack.isEmpty()) {
							player.addItemStackToInventory(pulledStack);
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		if (!state.getValue(HASITEM)) {
			world.setBlockState(pos, getDefaultState().withProperty(HASITEM, true));
		}
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(HASITEM, meta == 0 ? false : true);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return getMetaFromState(state);
	}

	/**
	 * Convert the BlockState into the correct metadata value
	 */
	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(HASITEM) ? 1 : 0;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] {
				HASITEM
		});
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (!player.isCreative()) {
			TileEntity te = world.getTileEntity(pos);
			ItemStack drop = new ItemStack(ModItems.ITEM_SPONGE_ITEMBLOCK);
			if (te != null && te instanceof TileItemSponge) {
				TileItemSponge tile = (TileItemSponge) world.getTileEntity(pos);
				if (tile != null && tile.hasAbsorbedStack()) {
					NBTTagCompound nbt = tile.writeToNBT(new NBTTagCompound());
					drop.setTagCompound(nbt);
					drop.setItemDamage(1);
				}
				spawnAsEntity(world, pos, drop);
				world.updateComparatorOutputLevel(pos, state.getBlock());
			}
		}
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileEntity te = world.getTileEntity(pos);
		ItemStack stack = new ItemStack(ModItems.ITEM_SPONGE_ITEMBLOCK);
		if (te != null && te instanceof TileItemSponge) {
			TileItemSponge tile = (TileItemSponge) world.getTileEntity(pos);
			if (tile != null && tile.hasAbsorbedStack()) {
				NBTTagCompound nbt = tile.writeToNBT(new NBTTagCompound());
				stack.setTagCompound(nbt);
			}
		}
		return stack;
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileItemSponge();
	}

	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public boolean isOpaqueCube(IBlockState p_isOpaqueCube_1_) {
		return false;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
	}

	// This is needed because we spawn our EntityItem manually in Block#breakBlock
	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Items.AIR;
	}

}
