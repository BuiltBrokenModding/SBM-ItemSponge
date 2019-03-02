package com.builtbroken.itemsponge.sponge;

import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.ModConfig.Options;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * @author p455w0rd
 */
public class RecipeItemSponge extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{

    private static final String RECIPE_GROUP = ItemSpongeMod.MODID + ":itemsponge_recipe";
    private static final RecipeItemSponge INSTANCE = new RecipeItemSponge();
    private ItemStack resultStack = ItemStack.EMPTY;
    private static final NonNullList<Ingredient> ingredients = NonNullList.<Ingredient>withSize(9, Ingredient.EMPTY);

    public RecipeItemSponge()
    {
        setRegistryName(RECIPE_GROUP);
    }

    public static RecipeItemSponge getInstance()
    {
        return INSTANCE;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return resultStack;
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        return ingredients;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
    {
        return NonNullList.withSize(9, ItemStack.EMPTY);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn)
    {
        int ingredientCount = 0;
        ItemStack sponge = ItemStack.EMPTY;
        ItemStack absorbedItem = ItemStack.EMPTY;
        for (int i = 0; i < inv.getHeight(); ++i)
        {
            for (int j = 0; j < inv.getWidth(); ++j)
            {
                ItemStack stack = inv.getStackInRowAndColumn(j, i);
                if (!stack.isEmpty())
                {
                    ++ingredientCount;
                    if (ingredientCount > 2)
                    {
                        return false;
                    }
                    if (sponge.isEmpty() && stack.getItem() == ItemSpongeMod.item)
                    {
                        sponge = stack.copy();
                        if (sponge.getCount() > 1)
                        {
                            sponge.shrink(1);
                        }
                    }
                    else
                    {
                        if (stack.getItem() != ItemSpongeMod.item && !Options.getItemBlackList().contains(stack.getItem()))
                        {
                            absorbedItem = stack.copy();
                            if (absorbedItem.getCount() > 1)
                            {
                                absorbedItem.shrink(1);
                            }
                        }
                    }
                }
            }
        }
        if (sponge.isEmpty())
        {
            return false;
        }
        if (absorbedItem.isEmpty() && ingredientCount == 1 && ItemSpongeUtils.hasAbsorbedItem(sponge))
        {
            resultStack = ItemSpongeUtils.setAbsorbedItem(sponge, ItemStack.EMPTY).copy();
            return true;
        }
        if (!absorbedItem.isEmpty() && sponge.getItemDamage() == 0 && !ItemSpongeUtils.isItemBlacklisted(absorbedItem))
        {
            if (absorbedItem.isItemStackDamageable() && absorbedItem.isItemDamaged())
            {
                absorbedItem.setItemDamage(absorbedItem.getMaxDamage());
            }
            resultStack = new ItemStack(ItemSpongeUtils.getSerializedSpongeStack(sponge, absorbedItem, 0)).copy();
            resultStack.setCount(1);
            resultStack.setItemDamage(1);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv)
    {
        return resultStack;
    }

    @Override
    public boolean canFit(int width, int height)
    {
        return width * height >= 2;
    }

}
