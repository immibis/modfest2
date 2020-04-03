package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusQueuestack extends FetchModusType {
	// Sorts items in a linear order like the queue or stack, but both the first and last inserted items are usable.
	// It sorts in queue order (new items are appended at the end, not the start).
	// The last inserted item always goes in slot 7, so that it's accessible on the hotbar.
	// Note that you can only insert items at one end (the slot 7 end), like in a queue.
	// You can insert items by clicking on the f
	
	// Acts like a queue, but the last inserted item is also usable.
	// AND: the slot order has been changed so that the last inserted item always goes in slot 7 so it's accessible on the hotbar.
	// AND: you can insert 
	// TODO: if we could decouple hotbar logic from inventory rendering, then we wouldn't need to move the last queue slot to the hotbar.
	
	@Override
	public void initialize(InventoryWrapper inv) {
		compactItemsToLowerIndices(inv, 0);
		
		// Find the end of the queue. (If inventory is empty, leave it at slot 0)
		int highestUsedSlot = inv.getNumSlots() - 1;
		for(; highestUsedSlot >= 1; highestUsedSlot--) {
			if (!inv.getInvStack(highestUsedSlot).isEmpty())
				break;
		}
		
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		
		// If it's less than LAST_ITEM_SLOT then just move it up one. LAST_ITEM_SLOT must be unused in this case
		// However, if there's only one item, it goes preferentially in slot 0.
		if (highestUsedSlot < LAST_ITEM_SLOT && highestUsedSlot != 0) {
			inv.setInvStack(LAST_ITEM_SLOT, inv.getInvStack(highestUsedSlot));
			inv.setInvStack(highestUsedSlot, ItemStack.EMPTY);
		}
		// If it's greater than LAST_ITEM_SLOT then we have to rotate all the items at LAST_ITEM_SLOT and above up by one, to make room.
		else if (highestUsedSlot > LAST_ITEM_SLOT) {
			// Rotate up all items from LAST_ITEM_SLOT to the end of the queue, so that the last item goes in LAST_ITEM_SLOT.
			ItemStack lastItemSlotStack = inv.getInvStack(highestUsedSlot);
			for(int k = highestUsedSlot; k > LAST_ITEM_SLOT; k--)
				inv.setInvStack(k, inv.getInvStack(k-1));
			inv.setInvStack(LAST_ITEM_SLOT, lastItemSlotStack);
		}
		// (If it was equal to LAST_ITEM_SLOT, then we're lucky and we don't need to change anything)
	}
	
	@Override
	public void deinitialize(InventoryWrapper inv) {
		// Form an orderly queue again, none of this LAST_ITEM_SLOT weirdness, so that other moduses can pick up the items in the right order.
		
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		
		// Move all items down, except for the item in LAST_ITEM_SLOT, which is put aside for now.
		ItemStack lastItemStack = ItemStack.EMPTY;
		int to = 0;
		for(int from = 0; from < inv.getNumSlots(); from++) {
			ItemStack fromStack = inv.getInvStack(from);
			if (!fromStack.isEmpty()) {
				if(from == LAST_ITEM_SLOT)
					lastItemStack = fromStack;
				else {
					if(from != to)
						inv.setInvStack(to, inv.getInvStack(from));
					to++;
				}
			}
		}
		if (!lastItemStack.isEmpty()) {
			if(to >= inv.getNumSlots())
				throw new AssertionError(); // can't happen
			inv.setInvStack(to, lastItemStack);
			to++;
		}
		for(; to < inv.getNumSlots(); to++) {
			inv.setInvStack(to, ItemStack.EMPTY);
		}
	}
	
	@Override
	public boolean canTakeFromSlot(InventoryWrapper inv, int slot) {
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		return slot == 0 || slot == LAST_ITEM_SLOT;
	}
	@Override
	public boolean canInsertToSlot(InventoryWrapper inv, int slot) {
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		// only allow one empty slot to be insertable, this is for one-slot item dragging for example.
		// TODO: this was copied from queue; see how well it does on queuestack
		if(slot == LAST_ITEM_SLOT) return true;
		return inv.getInvStack(slot).isEmpty() && (slot == 0 || !inv.getInvStack(slot-1).isEmpty());
	}
	
	@Override
	public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, InventoryWrapper inv, int slotIndex, SlotActionType actionType, int clickData) {
		
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		
		// A QuickCraft of one slot must be treated as a pickup, because it's very easy to drag over one slot, and it still gets counted as a QuickCraft.
		// We can't easily prevent QuickCraft from starting, but if we only make one slot insertable, then that's all the user can use for QuickCrafting.
		
		switch (actionType) {
		case CLONE: // copy stack in creative mode - allowed on any stack
			return false; // no override

		case PICKUP_ALL: // double-click on a slot to collect a stack of that item from anywhere in the inventory
			return false; // no override. It shouldn't take from blocked slots.

		case PICKUP: // normal click
			if(slotIndex == 0)
				return false; // allow all forms of normal click in slot 0. This will only extract items, not insert them (unless it's the only slot) because of canTakeItems/canInsertItems
			
			if (slotIndex == LAST_ITEM_SLOT) // allow all forms of normal click in slot 7. Note it's insertable and extractable.
				return false;
			
			// Insert items at the end of the queue by clicking anywhere other than slot 0 (even on slots that already have items). Right-click to insert one item.
			ItemStack cursor = plinv.getCursorStack();
			if(!cursor.isEmpty()) {
				if (clickData == 1 && cursor.getCount() >= 2) {
					// insert one item
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
			// Same as for queue: shift-clicking slot 0 cycles
			// On the inventory screen, this moves items from the hotbar into the player's non-hotbar inventory - which works fine. For a queue it cycles the queue, for a stack it does nothing, for others (memory) it might be useful.
			// It does tend to cause desyncs if you repeatedly click it. Not sure if that's a vanilla bug.
			// You wouldn't see that bug in vanilla, since shift-clicking clears the slot so you can't shift-click it again.
			// TODO: what happens if you shift-click slot 7 on the inventory screen? It should do nothing
			if (slotIndex == 0 || slotIndex == LAST_ITEM_SLOT)
				return false;
			return true;
			
		case THROW: // throw item. Should only be allowed in extractable slots already.
			return false;
		}
		return true; // block all unknown kinds of clicks
	}
	
	@Override
	public void afterInventoryClick(Container this_, PlayerInventory plinv, InventoryWrapper inv, int slotIndex, SlotActionType actionType, int clickData) {
		// Brute force! :)
		deinitialize(inv);
		initialize(inv);
	}
	
	@Override
	public void afterPossibleInventoryChange(Container this_, InventoryWrapper inv) {
		// Brute force! :)
		deinitialize(inv);
		initialize(inv);
	}
	
	@Override
	public boolean hasCustomInsert() {
		return true;
	}
	
	private int getLastFilledSlot(InventoryWrapper inv) {
		int lastFilled = 0; // return 0 if inventory is totally empty
		for(int k = 0; k < inv.getNumSlots(); k++) {
			if (!inv.getInvStack(k).isEmpty())
				lastFilled = k;
		}
		return lastFilled;
	}
	
	@Override
	public void insert(InventoryWrapper inv, ItemStack stack) {
		final int LAST_ITEM_SLOT = inv.getLastItemSlot();
		
		// When you shift-click a slot, the caller checks if all items were moved (stack.isEmpty() after the insert call)
		// If so, it sets the source slot to empty. Therefore, we can't possibly put a different item into the source slot, because that
		// will get deleted.
		// In particular, when shift-clicking out of LAST_ITEM_SLOT, we must leave LAST_ITEM_SLOT empty.
		// We can refill it in afterInventoryClick.
		
		// But also, if you are shift-clicking out of LAST_ITEM_SLOT, we don't even need to do anything because
		// the item is going back into the same slot it's coming out of. We can just fail the transfer, instead.
		// TODO: this problem would also be avoided if we just did a visual remapping to get LAST_ITEM_SLOT? But then we would need to visually remap the hotbar which is non-trivial.
		if (stack == inv.getInvStack(LAST_ITEM_SLOT))
			return;

		deinitialize(inv); // TODO: make this more efficient than brute force
		ItemStack curStackLast = inv.getInvStack(getLastFilledSlot(inv));
		
		
		// Basically we must change any non-empty slot in insert.
		
		// see comment below about the != check
		if(!curStackLast.isEmpty() && Container.canStacksCombine(curStackLast, stack) && curStackLast != stack) {
			int ntransfer = Math.min(stack.getCount(), curStackLast.getMaxCount() - curStackLast.getCount());
			curStackLast.increment(ntransfer);
			stack.decrement(ntransfer);
			if(stack.getCount() == 0) {
				initialize(inv); // TODO: make this more efficient than brute force (undoes previous deinitialize)
				return;
			}
		}
		for(int k = 0; k < inv.getNumSlots(); k++) {
			if (inv.getInvStack(k).isEmpty()) {
				
				if (inv.getInvStack(k) != stack) {
					inv.setInvStack(k, stack.copy());
					stack.setCount(0);
				}
				initialize(inv); // TODO: make this more efficient than brute force (undoes previous deinitialize)
				return;
			}
		}
		initialize(inv); // TODO: make this more efficient than brute force (undoes previous deinitialize)
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
		if(slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		
		// XXX: we don't have the inventory here to call getLastItemSlot, but it doesn't matter because this function is only called hwen inv is a player inventory
		final int LAST_ITEM_SLOT = 7;
		
		// don't merge the extractable slots with anything else
		if(slot == 0)
			return 0;
		if(slot == LAST_ITEM_SLOT)
			return 1;
		
		return 2; // merged rest of hotbar and main inventory
	}
	
	@Override
	protected boolean blocksAccessToHotbarSlot_(int slot) {
		// XXX: we don't have the inventory here to call getLastItemSlot, but it doesn't matter because this function is only called hwen inv is a player inventory
		return slot != 0 && slot != 7;
	}
}