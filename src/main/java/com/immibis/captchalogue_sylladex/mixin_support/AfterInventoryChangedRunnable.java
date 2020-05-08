package com.immibis.captchalogue_sylladex.mixin_support;

public class AfterInventoryChangedRunnable implements Runnable {
	private final IPlayerInventoryMixin inv;
	public AfterInventoryChangedRunnable(IPlayerInventoryMixin inv) {
		this.inv = inv;
	}
	@Override
	public void run() {
		inv.captchalogue_afterInventoryChanged();
	}
}
