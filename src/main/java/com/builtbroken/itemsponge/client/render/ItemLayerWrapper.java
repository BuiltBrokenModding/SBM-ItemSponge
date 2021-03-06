package com.builtbroken.itemsponge.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import java.util.List;

/**
 * @author p455w0rd
 */
public class ItemLayerWrapper implements IBakedModel
{

    private IBakedModel internal;

    public ItemLayerWrapper(IBakedModel internal)
    {
        this.internal = internal;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand)
    {
        return internal.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return internal.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return internal.isGui3d();
    }

    public IBakedModel getInternal()
    {
        return internal;
    }

    public void setInternal(IBakedModel model)
    {
        internal = model;
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture()
    {
        return internal.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides()
    {
        return ItemOverrideList.NONE;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type)
    {
        ItemSpongeItemRenderer.transformType = type;
        return Pair.of(this, internal.handlePerspective(type).getRight());
    }

}
