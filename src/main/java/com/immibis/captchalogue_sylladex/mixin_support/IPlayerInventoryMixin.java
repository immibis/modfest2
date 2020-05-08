package com.immibis.captchalogue_sylladex.mixin_support;

import com.immibis.captchalogue_sylladex.FetchModusState;

import net.minecraft.item.ItemStack;

public interface IPlayerInventoryMixin {
	public FetchModusState getFetchModus();
	
	// Called asynchronously from the main loop after the inventory changes
	public void captchalogue_afterInventoryChanged();

	public boolean captchalogue_acceptAsModus(ItemStack stack);
}
