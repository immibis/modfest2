package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public abstract class FetchModus {
	
	public static final int MODUS_SLOT = 8;
	public static final Identifier MODUS_SLOT_BG_IMAGE = new Identifier("compuglobalhypermeganet", "placeholder_fetch_modus");
	
	static void compactItemsToLowerIndices(PlayerInventory inventory, int start) {
		int to = start;
		int from = start;
		while(from < inventory.main.size()) {
			if(from != MODUS_SLOT && !inventory.main.get(from).isEmpty()) {
				if (from != to)
					inventory.main.set(to, inventory.main.get(from));
				to++;
				if(to == MODUS_SLOT)
					to++;
			}
			from++;
		}
		while(to < inventory.main.size()) {
			inventory.main.set(to, ItemStack.EMPTY);
			to++;
			if(to == MODUS_SLOT)
				to++;
		}
	}
	
	public static class Queue extends FetchModus {
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
					setStackInSlot(inv, k, stack);
					stack.setCount(0);
					return;
				}
			}
		}
	}
	
	public static class Stack extends FetchModus {
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
				return false; // no override needed - items don't need to be shifted
			}
			
			if (!inv.main.get(slot).isEmpty())
				return false; // either decrement of an accessible stack, or invalid access we couldn't block...
			
			// Shift items up
			for (int k = inv.main.size() - 1; k >= 0; k--) {
				int from = (k-1 == MODUS_SLOT ? k-2 : k-1);
				inv.main.set(k, inv.main.get(from));
			}
			// insert new item
			inv.main.set(0, stack);
			
			return false;
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
				curStack0.increment(ntransfer);
				stack.decrement(ntransfer);
				if(stack.getCount() == 0)
					return;
			}
			for(int k = 0; k < inv.main.size(); k++) {
				if (inv.main.get(k).isEmpty()) {
					setStackInSlot(inv, k, stack);
					stack.setCount(0);
					return;
				}
			}
		}
	}
	
	public static class Null extends FetchModus {
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
						inventory.player.dropItem(inventory.main.get(k), true, false);
					}
					inventory.main.set(k, ItemStack.EMPTY);
				}
			}
		}
		@Override
		public boolean setStackInSlot(PlayerInventory inventory, int slot, ItemStack stack) {
			inventory.player.dropItem(stack, true, false); // can't hold any items!
			return true;
		}
		
		@Override
		public boolean hasCustomInsert() {
			return true;
		}
		@Override
		public void insert(PlayerInventory inv, ItemStack stack) {
			// no-op
		}
	}

	public abstract boolean canTakeFromSlot(PlayerInventory inv, int slot);
	public abstract boolean canInsertToSlot(PlayerInventory inv, int slot);
	public abstract boolean setStackInSlot(PlayerInventory inventory, int slot, ItemStack stack); // return true to override
	public abstract void initialize(PlayerInventory inventory);
	
	public static FetchModus getModus(PlayerInventory inventory) {
		ItemStack modus = inventory.getInvStack(MODUS_SLOT);
		return getFlyweightModus(modus);
	}
	
	public static boolean isModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus)
			return true;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus)
			return true;
		return false;
	}
	
	public static FetchModus QUEUE = new Queue();
	public static FetchModus STACK = new Stack();
	public static FetchModus NULL = new Null();
	public static FetchModus getFlyweightModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus)
			return QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus)
			return STACK;
		return NULL;
	}
	public abstract boolean hasCustomInsert();
	public abstract void insert(PlayerInventory inv, ItemStack stack);
}
