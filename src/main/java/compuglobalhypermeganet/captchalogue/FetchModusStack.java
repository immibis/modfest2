package compuglobalhypermeganet.captchalogue;

import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
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
		if(slot == 0)
			return true;
		// Allow insert into the first empty slot. This is redirected by overrideInventoryClick so it inserts onto the top of the stack.
		// We allow this because we need to have an empty slot to insert into, for some operations to work (like shift-click from a chest).
		return inv.main.get(slot).isEmpty() && (slot == 0 || !inv.main.get(slot == MODUS_SLOT+1 ? slot-2 : slot-1).isEmpty());
	}
	
	@Override
	public boolean overrideInventoryClick(Container cont, PlayerInventory inv, int slotIndex, SlotActionType actionType, int clickData) {
		
		// A QuickCraft of one slot must be treated as a pickup, because it's very easy to drag over one slot, and it still gets counted as a QuickCraft.
		// We can't easily prevent QuickCraft from starting, but if we only make one slot insertable, then that's all the user can use for QuickCrafting.
		
		switch (actionType) {
		case CLONE: // copy stack in creative mode - allowed on any stack
			return false; // no override

		case PICKUP_ALL: // double-click on a slot to collect a stack of that item from anywhere in the inventory
			return false; // no override. It shouldn't take from blocked slots.

		case PICKUP: // normal click
			if(slotIndex == 0)
				return false; // allow all forms of normal click in slot 0. This can extract, and swap items with the cursor. All are valid stack operations.
			// To insert items into a stack, you can't click on slot 0, because that will swap with the cursor! But you can click anywhere else.
			
			// Insert items at the end of the queue by clicking anywhere other than slot 0 (even on slots that already have items). Right-click to insert one item.
			ItemStack cursor = inv.getCursorStack();
			if(!cursor.isEmpty()) {
				if (clickData == 1 && cursor.getCount() >= 2) {
					// insert one item. TODO: de-duplicate this code with FetchModusQueue
					ItemStack one = cursor.copy();
					one.setCount(1);
					insert(inv, one);
					if(one.isEmpty())
						cursor.decrement(1);
				} else {
					insert(inv, cursor);
				}
			}
			return true;

		case QUICK_CRAFT:
			return false; // allow action ("quick craft" = dragging the mouse to select multiple slots; it can only put items into insertable slots, i.e. the first empty one)
			
		case SWAP:
			return true; // block action ("swap" = swap a hotbar slot 1-9 with the slot under the cursor)
			
		case QUICK_MOVE: // shift-click. Allowed on slot 0 to transfer the item into the other container.
			// On the inventory screen, this moves items from the hotbar into the player's non-hotbar inventory - which works fine. For a queue it cycles the queue, for a stack it does nothing, for others (memory) it might be useful.
			// It does tend to cause desyncs if you repeatedly click it. Not sure if that's a vanilla bug.
			// You wouldn't see that bug in vanilla, since shift-clicking clears the slot so you can't shift-click it again.
			if (slotIndex == 0)
				return false;
			return true;
			
		case THROW: // throw item. Should only be allowed in extractable slots already.
			return false;
		}
		return true; // block all unknown kinds of clicks
	}
	
	@Override
	public void afterInventoryClick(Container this_, PlayerInventory inv, int slotIndex, SlotActionType actionType, int clickData) {
		// Brute force! :)
		initialize(inv);
	}
	
	@Override
	public void afterPossibleInventoryChange(Container this_, PlayerInventory inv) {
		// Brute force! :)
		initialize(inv);
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
	
	@Override
	protected boolean blocksAccessToHotbarSlot_(int slot) {
		return slot != 0;
	}
}