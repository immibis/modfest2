package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusNull extends FetchModus {
	@Override
	public boolean canInsertToSlot(InventoryWrapper inv, int slot) {
		return false;
	}
	@Override
	public boolean canTakeFromSlot(InventoryWrapper inv, int slot) {
		return false;
	}
	@Override
	public void initialize(InventoryWrapper inventory) {
		PlayerEntity player = inventory.getPlayer();
		for(int k = 0; k < inventory.getNumSlots(); k++) {
			ItemStack stack = inventory.getInvStack(k);
			inventory.setInvStack(k, ItemStack.EMPTY);
			
			if (!player.world.isClient()) {
				// Second parameter makes items drop in a much bigger range; third parameter sets the thrower?
				player.dropItem(stack, false, true);
			}
		}
	}
	@Override
	public boolean setStackInSlot(PlayerInventory inventory, int slot, ItemStack stack) {
		if(inventory.player.world.isClient() || stack.isEmpty())
			return false; // note that we often get here before the fetch modus item loads in on the client
		else {
			inventory.player.dropItem(stack, true, false); // can't hold any items!
			return true;
		}
	}
	
	@Override
	public boolean hasCustomInsert() {
		return true;
	}
	@Override
	public void insert(InventoryWrapper inv, ItemStack stack) {
		// no-op
	}
	@Override
	public boolean forceRightClickOneItem() {
		return false; // doesn't matter since no items can be stored
	}
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if(slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		return BG_GROUP_INVISIBLE;
	}
}