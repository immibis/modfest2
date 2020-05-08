package com.immibis.captchalogue_sylladex;

import net.minecraft.item.ItemStack;

public class FetchModusArray extends FetchModusType {
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
		public boolean canInsertToSlot(int slot) {
			return true;
		}

		@Override
		public boolean canTakeFromSlot(int slot) {
			return true;
		}

		@Override
		public boolean hasCustomInsert() {
			return false;
		}

		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			// Even though we have hasCustomInsert false, this is called from FetchModusHashtableOfX(FetchModusArray).
			InventoryUtils.insertStack(inv, stack, 0, inv.getNumSlots());
		}
		
	}
	
	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
}