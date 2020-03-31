package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public abstract class InventoryWrapper {
	public abstract int getNumSlots();
	public abstract ItemStack getInvStack(int slot);
	public abstract void setInvStack(int slot, ItemStack stack);
	
	public abstract PlayerEntity getPlayer();
	
	public static class PlayerInventorySkippingModusSlot extends InventoryWrapper {
		private PlayerInventory base;
		public PlayerInventorySkippingModusSlot(PlayerInventory base) {
			this.base = base;
		}
		@Override
		public PlayerEntity getPlayer() {
			return base.player;
		}
		@Override
		public int getNumSlots() {
			return base.getInvSize() - 1;
		}
		@Override
		public ItemStack getInvStack(int slot) {
			return base.getInvStack(toUnderlyingSlotIndex(slot));
		}
		@Override
		public void setInvStack(int slot, ItemStack stack) {
			base.setInvStack(toUnderlyingSlotIndex(slot), stack);
		}
		public int fromUnderlyingSlotIndex(int slot) {
			if(slot == CaptchalogueMod.MODUS_SLOT)
				throw new IllegalArgumentException("slot is not mapped");
			if(slot >= CaptchalogueMod.MODUS_SLOT)
				slot--;
			return slot;
		}
		public int toUnderlyingSlotIndex(int slot) {
			if(slot >= CaptchalogueMod.MODUS_SLOT)
				slot++;
			return slot;
		}
	}
}
