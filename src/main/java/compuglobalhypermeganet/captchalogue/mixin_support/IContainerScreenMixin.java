package compuglobalhypermeganet.captchalogue.mixin_support;

import java.util.List;

import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;

public interface IContainerScreenMixin {
	public int captchalogue_getGuiX();
	public int captchalogue_getGuiY();
	public Slot captchalogue_getSlotAt(double x, double y);
	public void captchalogue_onMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType);
	public void captchalogue_fiddleWithItemRenderTooltip(List<String> tooltip);
	public void captchalogue_invalidateLayout();
}
