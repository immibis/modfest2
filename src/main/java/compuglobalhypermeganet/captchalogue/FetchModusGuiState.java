package compuglobalhypermeganet.captchalogue;

import java.awt.geom.Rectangle2D;

import compuglobalhypermeganet.captchalogue.mixin_support.IContainerScreenMixin;
import net.minecraft.container.Slot;

public abstract class FetchModusGuiState {

	public Rectangle2D.Double area;

	// Mouse events: return true to handle. Only called on client.
	public void mouseMoved(double x, double y) {}
	public boolean mouseClicked(double x, double y, int button) {return false;}
	public boolean mouseReleased(double x, double y, int button) {return false;}
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {return false;}
	public boolean mouseScrolled(double x, double y, double scrollAmount) {return false;}
	public boolean isMouseOver(double x, double y) {return false;}
	
	public void onBeforeDraw(IContainerScreenMixin contScreen) {}
	public void drawAdditionalBackground(Drawer d) {}
	public void beforeDrawSlot(Slot slot, Drawer impl) {}
	public void afterDrawSlot(Slot slot, Drawer impl) {}
	
	// return 0 to force false, 1 to force true, else don't override
	public int overridesIsPointOverSlot(Slot slot, double x, double y) {return -1;}
	
	public static final FetchModusGuiState NULL_GUI_STATE = new FetchModusGuiState() {
		
	};
}
