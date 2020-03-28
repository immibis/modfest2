package compuglobalhypermeganet.captchalogue;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusNull extends FetchModus {
	@Override
	public boolean canInsertToSlot(PlayerInventory inv, int slot) {
		return false;
	}
	@Override
	public boolean canTakeFromSlot(PlayerInventory inv, int slot) {
		return false;
	}
	@Override
	public void initialize(PlayerInventory inventory) {
		for(int k = 0; k < inventory.main.size(); k++) {
			if(k != MODUS_SLOT) {
				if (!inventory.player.world.isClient()) {
					// TODO: was true, false. Second parameter makes items drop in a much bigger range; third parameter sets the thrower?
					inventory.player.dropItem(inventory.main.get(k), false, true);
				}
				inventory.main.set(k, ItemStack.EMPTY);
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
	public void insert(PlayerInventory inv, ItemStack stack) {
		// no-op
	}
	@Override
	public boolean forceRightClickOneItem() {
		return false; // doesn't matter since no items can be stored
	}
}