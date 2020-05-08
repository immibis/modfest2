package com.immibis.captchalogue_sylladex.client;

import org.lwjgl.opengl.GL11;

import com.immibis.captchalogue_sylladex.Drawer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public class DrawerImpl extends Drawer {
	
	private BufferBuilder bb;
	
	@Override
	public void beginRenderingSolidQuads() {
		RenderSystem.disableTexture();
		bb = Tessellator.getInstance().getBuffer();
		bb.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
	}
	
	Matrix4f matrix = Matrix4f.translate(0, 0, 0);
	float offsetX = 0, offsetY = 0;
	// matrix used for drawing, offset used for glScissor
	public void setMatrix(Matrix4f matrix, float offsetX, float offsetY) {
		this.matrix = matrix;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	@Override
	public void appendSolidQuad(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
		bb.vertex(matrix, x1, y1, 0).color(r, g, b, a).next();
		bb.vertex(matrix, x1, y2, 0).color(r, g, b, a).next();
		bb.vertex(matrix, x2, y2, 0).color(r, g, b, a).next();
		bb.vertex(matrix, x2, y1, 0).color(r, g, b, a).next();
	}
	
	@Override
	public void appendVertex(float x, float y, float r, float g, float b, float a) {
		bb.vertex(matrix, x, y, 0).color(r, g, b, a).next();
	}

	@Override
	public void endRenderingSolidQuads() {
		bb.end();
		BufferRenderer.draw(bb);
		RenderSystem.enableTexture();
	}

	@Override
	public void restrictRendering(int x1, int y1, int x2, int y2) {
		
		MinecraftClient mc = MinecraftClient.getInstance();
		double scale = mc.getWindow().getScaleFactor();
		
		/*Vector4f v = new Vector4f(x1, y1, 0, 1);
		v.transform(matrix);
		x1 = (int)(v.getX() * scale);
		y1 = (int)(v.getY() * scale);
		
		v = new Vector4f(x2, y2, 0, 1);
		v.transform(matrix);
		x2 = (int)(v.getX() * scale);
		y2 = (int)(v.getY() * scale);*/
		
		x1 += offsetX;
		x2 += offsetX;
		y1 += offsetY;
		y2 += offsetY;
		
		x1 *= scale;
		x2 *= scale;
		y1 *= scale;
		y2 *= scale;
		
		GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		int height = mc.getWindow().getHeight();
		GL11.glScissor(x1, height - y2, x2 - x1, y2 - y1);
	}
	@Override
	public void unrestrictRendering() {
		GL11.glPopAttrib();
	}
	
	@Override
	public void enableBlend() {
		GlStateManager.enableBlend();
	}
	
	@Override
	public void disableBlend() {
		GlStateManager.disableBlend();
	}
}
