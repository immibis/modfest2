package compuglobalhypermeganet.captchalogue;

import net.minecraft.item.ItemStack;

public class FetchModusArray extends FetchModusType {
	@Override
	public boolean canInsertToSlot(InventoryWrapper inv, int slot) {
		return true;
	}
	@Override
	public boolean canTakeFromSlot(InventoryWrapper inv, int slot) {
		return true;
	}
	@Override
	public boolean hasCustomInsert() {
		return false;
	}
	@Override
	public void insert(InventoryWrapper inv, ItemStack stack) {
		throw new AssertionError("unreachable");
	}
	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
}