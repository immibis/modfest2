package compuglobalhypermeganet.captchalogue;

import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusQueuestack extends FetchModus {
	// Sorts items in a linear order like the queue or stack, but both the first and last inserted items are usable.
	// It sorts in queue order (new items are appended at the end, not the start).
	// The last inserted item always goes in slot 7, so that it's accessible on the hotbar.
	// Note that you can only insert items at one end (the slot 7 end), like in a queue.
	// You can insert items by clicking on the f
	
	// Acts like a queue, but the last inserted item is also usable.
	// AND: the slot order has been changed so that the last inserted item always goes in slot 7 so it's accessible on the hotbar.
	// AND: you can insert 
	// TODO: if we could decouple hotbar logic from inventory rendering, then we wouldn't need to move the last queue slot to the hotbar.
	
	public static final int LAST_ITEM_SLOT = 7;
	
	@Override
	public void initialize(PlayerInventory inv) {
		compactItemsToLowerIndices(inv, 0);
		
		// Find the end of the queue. (If queue is empty, leave it at slot 0)
		int highestUsedSlot = 35;
		for(; highestUsedSlot >= 1; highestUsedSlot--) {
			if(highestUsedSlot == MODUS_SLOT)
				continue;
			if (!inv.getInvStack(highestUsedSlot).isEmpty())
				break;
		}
		if(highestUsedSlot == MODUS_SLOT) // modus slot could be slot 0
			highestUsedSlot++;
		
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
			for(int k = highestUsedSlot; k > LAST_ITEM_SLOT; k--) {
				if(k == MODUS_SLOT) continue;
				int prevSlot = (k == MODUS_SLOT + 1 ? k-2 : k-1);
				inv.setInvStack(k, inv.getInvStack(prevSlot));
			}
			inv.setInvStack(LAST_ITEM_SLOT, lastItemSlotStack);
		}
		// (If it was equal to LAST_ITEM_SLOT, then we're lucky and we don't need to change anything)
	}
	
	@Override
	public void deinitialize(PlayerInventory inv) {
		// Form an orderly queue again, none of this LAST_ITEM_SLOT weirdness, so that other moduses can pick up the items in the right order.
		
		// If the queue is shorter than LAST_ITEM_SLOT, move LAST_ITEM_SLOT to the first empty slot.
		for(int k = 0; k < LAST_ITEM_SLOT; k++) {
			if(k == MODUS_SLOT) continue;
			if (inv.getInvStack(k).isEmpty()) {
				inv.setInvStack(k, inv.getInvStack(LAST_ITEM_SLOT));
				inv.setInvStack(LAST_ITEM_SLOT, ItemStack.EMPTY);
				return;
			}
		}
		
		// If the queue is longer than LAST_ITEM_SLOT, move the stack in LAST_ITEM_SLOT to the end, and rotate other items down.
		ItemStack lastItemSlotStack = inv.getInvStack(LAST_ITEM_SLOT);
		for(int k = LAST_ITEM_SLOT; k < 35; k++) {
			if(k == MODUS_SLOT) continue;
			int nextSlot = (k == MODUS_SLOT - 1 ? k+2 : k+1);
			ItemStack nextSlotStack = inv.getInvStack(nextSlot);
			if (nextSlotStack.isEmpty()) {
				inv.setInvStack(k, lastItemSlotStack);
				break;
			}
			else
				inv.setInvStack(k, inv.getInvStack(nextSlot));
		}
	}
	
	@Override
	public boolean canTakeFromSlot(PlayerInventory inv, int slot) {
		return slot == 0 || slot == LAST_ITEM_SLOT;
	}
	@Override
	public boolean canInsertToSlot(PlayerInventory inv, int slot) {
		// only allow one empty slot to be insertable, this is for one-slot item dragging for example.
		// TODO: this was copied from queue; see how well it does on queuestack
		if(slot == LAST_ITEM_SLOT) return true;
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
				return false; // allow all forms of normal click in slot 0. This will only extract items, not insert them (unless it's the only slot) because of canTakeItems/canInsertItems
			
			if (slotIndex == LAST_ITEM_SLOT) // allow all forms of normal click in slot 7. Note it's insertable and extractable.
				return false;
			
			// Insert items at the end of the queue by clicking anywhere other than slot 0 (even on slots that already have items). Right-click to insert one item.
			ItemStack cursor = inv.getCursorStack();
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
	public void afterInventoryClick(Container this_, PlayerInventory inv, int slotIndex, SlotActionType actionType, int clickData) {
		// Brute force! :)
		deinitialize(inv);
		initialize(inv);
	}
	
	@Override
	public void afterPossibleInventoryChange(Container this_, PlayerInventory inv) {
		// Brute force! :)
		deinitialize(inv);
		initialize(inv);
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
		ItemStack curStackLast = inv.main.get(getLastFilledSlot(inv));
		
		
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
		for(int k = 0; k < inv.main.size(); k++) {
			if (inv.main.get(k).isEmpty()) {
				
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
		if(slot == MODUS_SLOT)
			return BG_GROUP_MODUS;
		
		// don't merge the extractable slots with anything else
		if(slot == 0)
			return 0;
		if(slot == LAST_ITEM_SLOT)
			return 1;
		
		return 2; // merged rest of hotbar and main inventory
	}
	
	@Override
	protected boolean blocksAccessToHotbarSlot_(int slot) {
		return slot != 0 && slot != LAST_ITEM_SLOT;
	}
}