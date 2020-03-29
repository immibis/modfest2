package compuglobalhypermeganet.captchalogue;

import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusStack extends FetchModus {
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
		if (stack.isEmpty()) {
			inv.main.set(slot, stack);
			compactItemsToLowerIndices(inv, slot);
			return true;
		}
		
		if (!inv.main.get(slot).isEmpty())
			return false; // either decrement of an accessible stack, or invalid access we couldn't block...
		
		// TODO: why does this duplicate items?
		insert(inv, stack.copy());
		return true;
	}
	
	@Override
	public boolean hasCustomInsert() {
		return true;
	}
	
	@Override
	public void insert(PlayerInventory inv, ItemStack stack) {
		ItemStack curStack0 = inv.main.get(0);
		if(!curStack0.isEmpty() && Container.canStacksCombine(curStack0, stack)) {
			int ntransfer = Math.min(stack.getCount(), curStack0.getMaxCount() - curStack0.getCount());
			if(ntransfer > 0) {
				curStack0.increment(ntransfer);
				stack.decrement(ntransfer);
				if(stack.getCount() == 0)
					return;
			}
		}
		for(int k = 0; k < inv.main.size(); k++) {
			if (inv.main.get(k).isEmpty()) {
				
				// Shift items up
				for (int i = inv.main.size() - 1; i >= 0; i--) {
					if(i == MODUS_SLOT)
						continue;
					int from = (i-1 == MODUS_SLOT ? i-2 : i-1);
					if (from >= 0)
						inv.main.set(i, inv.main.get(from));
				}
				// insert new item
				inv.main.set(MODUS_SLOT == 0 ? 1 : 0, stack.copy());
				
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