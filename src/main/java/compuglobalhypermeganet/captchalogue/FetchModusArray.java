package compuglobalhypermeganet.captchalogue;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusArray extends FetchModus {
	@Override
	public boolean canInsertToSlot(PlayerInventory inv, int slot) {
		return true;
	}
	@Override
	public boolean canTakeFromSlot(PlayerInventory inv, int slot) {
		return true;
	}
	@Override
	public void initialize(PlayerInventory inventory) {
	}
	@Override
	public boolean hasCustomInsert() {
		return false;
	}
	@Override
	public void insert(PlayerInventory inv, ItemStack stack) {
		throw new AssertionError("unreachable");
	}
	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
}