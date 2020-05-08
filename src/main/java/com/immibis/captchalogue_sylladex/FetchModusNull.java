package com.immibis.captchalogue_sylladex;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FetchModusNull extends FetchModusType {
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
			PlayerEntity player = inv.getPlayer();
			if(player.world.isClient())
				return;
			
			for(int k = 0; k < inv.getNumSlots(); k++) {
				ItemStack stack = inv.getInvStack(k);
				if(!stack.isEmpty()) {
					inv.setInvStack(k, ItemStack.EMPTY);
					CaptchalogueMod.launchExcessItems(player, stack);
				}
			}
		}
		
		@Override
		public boolean canInsertToSlot(int slot) {
			return false;
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			return false;
		}

		@Override
		public boolean hasCustomInsert() {
			return true;
		}
		
		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			if (allowViolentExpulsion) {
				CaptchalogueMod.launchExcessItems(inv.getPlayer(), stack.copy());
				stack.setCount(0);
			}
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			if(!serverSync)
				initialize(); // drop all items
		}
	}
}