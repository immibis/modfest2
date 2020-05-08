package com.immibis.captchalogue_sylladex;

import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusStack extends FetchModusType {
	
	
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
		
		// don't merge the extractable slot with anything else
		if(slot == 0)
			return 0;
		
		return 1; // merged rest of hotbar and main inventory
	}
	
	@Override
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return new State(inv);
	}
	
	public static class State extends FetchModusState {
		private InventoryWrapper inv;
		public State(InventoryWrapper inv) {
			this.inv = inv;
		}
		
		@Override
		public void initialize() {
			compactItemsToLowerIndices(inv, 0, true);
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			return slot == 0;
		}
		@Override
		public boolean canInsertToSlot(int slot) {
			if(slot == 0)
				return true;
			// Allow insert into the first empty slot. This is redirected by overrideInventoryClick so it inserts onto the top of the stack.
			// We allow this because we need to have an empty slot to insert into, for some operations to work (like shift-click from a chest).
			return inv.getInvStack(slot).isEmpty() && (slot == 0 || !inv.getInvStack(slot - 1).isEmpty());
		}
		
		@Override
		public boolean hasCustomInsert() {
			return true;
		}
		
		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			if(stack.isEmpty())
				return;
			
			ItemStack curStack0 = inv.getInvStack(0);
			if(!curStack0.isEmpty() && Container.canStacksCombine(curStack0, stack)) {
				int ntransfer = Math.min(stack.getCount(), curStack0.getMaxCount() - curStack0.getCount());
				if(ntransfer > 0) {
					curStack0.increment(ntransfer);
					stack.decrement(ntransfer);
					if(stack.getCount() == 0)
						return;
				}
			}
			for(int k = 0; k < inv.getNumSlots(); k++) {
				if (inv.getInvStack(k).isEmpty()) {
					
					// Shift items up
					for (int i = inv.getNumSlots() - 1; i >= 1; i--)
						inv.setInvStack(i, inv.getInvStack(i-1));
					// insert new item
					inv.setInvStack(0, stack.copy());
					
					stack.setCount(0);
					return;
				}
			}
			
			if (allowViolentExpulsion) {
				// no free slots? launch whatever is at the bottom of the stack, then push the new item.
				ItemStack launchItems = inv.getInvStack(inv.getNumSlots() - 1);
				if(launchItems.isEmpty())
					throw new AssertionError("unreachable - we have no empty slots, not even this one");
				inv.setInvStack(inv.getNumSlots()-1, ItemStack.EMPTY);
				CaptchalogueMod.launchExcessItems(inv.getPlayer(), launchItems);
				insert(stack, true); // retry. Violent expulsion shouldn't happen again, but pass false just in case, to prevent infinite recursion.
			}
		}

		@Override
		protected boolean blocksAccessToHotbarSlot_(int slot) {
			return slot != 0;
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			// Brute force! :)
			if (!serverSync || inv.getInvStack(0).isEmpty())
				initialize();
		}

		@Override
		public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, int slotIndex, SlotActionType actionType, int clickData) {
			
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
				ItemStack cursor = plinv.getCursorStack();
				if(!cursor.isEmpty()) {
					if (clickData == 1 && cursor.getCount() >= 2) {
						// insert one item. TODO: de-duplicate this code with FetchModusQueue
						ItemStack one = cursor.copy();
						one.setCount(1);
						insert(one, true);
						if(one.isEmpty())
							cursor.decrement(1);
					} else {
						insert(cursor, true);
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
	}
}