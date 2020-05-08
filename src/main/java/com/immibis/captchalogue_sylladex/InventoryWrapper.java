package com.immibis.captchalogue_sylladex;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public abstract class InventoryWrapper {

	public abstract int getNumSlots();
	public abstract ItemStack getInvStack(int slot);
	public abstract void setInvStack(int slot, ItemStack stack);
	@Deprecated public abstract int toPlayerInventorySlotIndex(int slot);
	public abstract PlayerEntity getPlayer();
	
	/**
	 * Used for queuestack moduses.
	 * For player inventory, this returns the last readily accessible slot in the hotbar (slot 7, since 8 is the modus slot).
	 * For array sub-inventory, this returns the last slot.
	 */
	public abstract int getLastItemSlot();
	
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
			return base.main.size() - 1; // don't include offhand, armour, etc
		}
		@Override
		public ItemStack getInvStack(int slot) {
			return base.getInvStack(toUnderlyingSlotIndex(slot));
		}
		@Override
		public void setInvStack(int slot, ItemStack stack) {
			base.setInvStack(toUnderlyingSlotIndex(slot), stack);
		}
		public static int fromUnderlyingSlotIndex(int slot) {
			if(slot == CaptchalogueMod.MODUS_SLOT)
				throw new IllegalArgumentException("slot is not mapped");
			if(slot >= CaptchalogueMod.MODUS_SLOT)
				slot--;
			return slot;
		}
		public static int toUnderlyingSlotIndex(int slot) {
			if(slot >= CaptchalogueMod.MODUS_SLOT)
				slot++;
			return slot;
		}
		@Override
		public int toPlayerInventorySlotIndex(int slot) {
			return toUnderlyingSlotIndex(slot);
		}
		@Override
		public int getLastItemSlot() {
			return (CaptchalogueMod.MODUS_SLOT == 8 ? 7 : 8);
		}
	}

	public static class SpecifiedSlots extends InventoryWrapper {
		private InventoryWrapper base;
		private int[] slots;
		public SpecifiedSlots(InventoryWrapper base, int[] slots) {
			this.base = base;
			this.slots = slots;
		}

		@Override
		public int getNumSlots() {
			return slots.length;
		}

		@Override
		public ItemStack getInvStack(int slot) {
			return base.getInvStack(slots[slot]);
		}

		@Override
		public void setInvStack(int slot, ItemStack stack) {
			base.setInvStack(slots[slot], stack);
		}

		@Override
		public int toPlayerInventorySlotIndex(int slot) {
			return base.toPlayerInventorySlotIndex(slots[slot]);
		}

		@Override
		public PlayerEntity getPlayer() {
			return base.getPlayer();
		}

		@Override
		public int getLastItemSlot() {
			return slots.length - 1;
		}
	}
}
