package compuglobalhypermeganet.captchalogue.mixin_support;

import compuglobalhypermeganet.captchalogue.FetchModusGuiState;
import net.minecraft.container.Slot;
import net.minecraft.item.ItemStack;

public interface IContainerMixin {
	public FetchModusGuiState getFetchModusGuiState();
	
	// Callback from Slot.setStack
	public void captchalogue_onSlotStackChanging(Slot slot, ItemStack stack);
	
	// Called when player inventory is changed
	public void captchalogue_onPlayerInventoryStackChanging(int slot, ItemStack stack);
	
	//public int captchalogue_getNextChangedSlot();
	public boolean captchalogue_haveChanges();
}
