package net.chococraft.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.chococraft.Chococraft;
import net.chococraft.entity.Chocobo;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class LayerCollar extends RenderLayer<Chocobo, EntityModel<Chocobo>> {
	private ResourceLocation COLLAR_CHOCOBO = new ResourceLocation(Chococraft.MODID, "textures/entities/chocobos/collar.png");
	private ResourceLocation COLLAR_CHICOBO = new ResourceLocation(Chococraft.MODID, "textures/entities/chicobos/collar.png");

	public LayerCollar(RenderLayerParent<Chocobo, EntityModel<Chocobo>> rendererIn) {
		super(rendererIn);
	}

	@Override
	public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Chocobo chocoboEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (chocoboEntity.isTame() && !chocoboEntity.isInvisible()) {
			renderColoredCutoutModel(this.getParentModel(), chocoboEntity.isBaby() ? COLLAR_CHICOBO : COLLAR_CHOCOBO, matrixStackIn, bufferIn, packedLightIn, chocoboEntity, 1.0F, 1.0F, 1.0F);
		}
	}
}
