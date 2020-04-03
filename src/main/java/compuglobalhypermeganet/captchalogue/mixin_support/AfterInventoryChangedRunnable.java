package compuglobalhypermeganet.captchalogue.mixin_support;

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
