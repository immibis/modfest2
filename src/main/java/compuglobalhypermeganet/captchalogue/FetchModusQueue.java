package compuglobalhypermeganet.captchalogue;

import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusQueue extends FetchModus {
	@Override
	public void initialize(PlayerInventory inventory) {
		compactItemsToLowerIndices(inventory, 0);
	}
	
	@Override
	public boolean canTakeFromSlot(PlayerInventory inv, int slot) {
		return slot == 0;
	}
	@Override
	public boolean canInsertToSlot(PlayerInventory inv, int slot) {
		return inv.main.get(slot).isEmpty();
	}
	
	@Override
	public boolean setStackInSlot(PlayerInventory inv, int slot, ItemStack stack) {
		// TODO: don't process moduses when receiving inventory contents from the server.
		if (stack.isEmpty()) {
			// Remove item; move existing items down.
			inv.main.set(slot, ItemStack.EMPTY);
			compactItemsToLowerIndices(inv, 0);
			return true;
		}
		
		if (!inv.main.get(slot).isEmpty())
			return false; // either decrement of an accessible stack, or invalid access we couldn't block...
		
		for (int k = 0; k < inv.main.size(); k++) {
			if (k != MODUS_SLOT && inv.main.get(k).isEmpty()) {
				inv.setInvStack(k, stack);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean hasCustomInsert() {
		return true;
	}
	
	private int getLastFilledSlot(PlayerInventory inv) {
		int lastFilled = 0; // return 0 if inventory is totally empty
		for(int k = 0; k < inv.main.size(); k++) {
			if (k != MODUS_SLOT && !inv.main.get(k).isEmpty())
				lastFilled = k;
		}
		return lastFilled;
	}
	
	@Override
	public void insert(PlayerInventory inv, ItemStack stack) {
		ItemStack curStackLast = inv.main.get(getLastFilledSlot(inv));
		if(!curStackLast.isEmpty() && Container.canStacksCombine(curStackLast, stack)) {
			int ntransfer = Math.min(stack.getCount(), curStackLast.getMaxCount() - curStackLast.getCount());
			curStackLast.increment(ntransfer);
			stack.decrement(ntransfer);
			if(stack.getCount() == 0)
				return;
		}
		for(int k = 0; k < inv.main.size(); k++) {
			if (inv.main.get(k).isEmpty()) {
				inv.setInvStack(k, stack.copy());
				stack.setCount(0);
				return;
			}
		}
	}
	@Override
	public boolean forceRightClickOneItem() {
		return true;
	}
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if(slot == MODUS_SLOT)
			return BG_GROUP_MODUS;
		
		// don't merge the extractable slot with anything else
		if(slot == 0)
			return 0;
		
		return 1; // merged rest of hotbar and main inventory
	}
}