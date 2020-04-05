package compuglobalhypermeganet.captchalogue;

public abstract class Drawer {

	public abstract void beginRenderingSolidQuads();
	public abstract void appendSolidQuad(float x1, float y1, float x2, float y2, float r, float g, float b, float a);
	public abstract void appendVertex(float x, float y, float r, float g, float b, float a);
	public abstract void endRenderingSolidQuads();
	public abstract void restrictRendering(int x1, int y1, int x2, int y2);
	public abstract void unrestrictRendering();
	public abstract void enableBlend();
	public abstract void disableBlend();
	
	public final void appendSolidQuad(double x1, double y1, double x2, double y2, float r, float g, float b, float a) {
		appendSolidQuad((float)x1, (float)y1, (float)x2, (float)y2, r, g, b, a);
	}
	
	// Fills the background and draws a 1-pixel border inside the specified area
	public final void drawBorder(double x1, double y1, double x2, double y2) {
		appendSolidQuad(x1, y1, x2, y2, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
		appendSolidQuad(x1, y1, x2-1, y1+1, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, 1.0f);
		appendSolidQuad(x1, y1, x1+1, y2-1, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, 1.0f);
		appendSolidQuad(x2-1, y1+1, x2, y2, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, 1.0f);
		appendSolidQuad(x1+1, y2-1, x2, y2, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, 1.0f);
	}
	
	// Same but with opposite colours. This doesn't draw the BL and TR corners.
	public final void drawInverseBorder(double x1, double y1, double x2, double y2) {
		appendSolidQuad(x1+1, y1+1, x2-1, y2-1, COL_INVISIBLE, COL_INVISIBLE, COL_INVISIBLE, 1.0f);
		appendSolidQuad(x1, y1, x2-1, y1+1, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, 1.0f);
		appendSolidQuad(x1, y1, x1+1, y2-1, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, 1.0f);
		appendSolidQuad(x2-1, y1+1, x2, y2, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, 1.0f);
		appendSolidQuad(x1+1, y2-1, x2, y2, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, 1.0f);
	}

	public static final float COL_INVISIBLE = 198.0f/255.0f; // standard colour for backgrounds of vanilla GUIs
	public static final float COL_SLOT = 139.0f/255.0f; // standard colour for backgrounds of clickable slots
	public static final float COL_RIGHT_BOTTOM = 255.0f/255.0f; // standard colour for right and bottom side border.
	public static final float COL_LEFT_TOP = 55.0f/255.0f; // standard colour for top and left side border.
}
