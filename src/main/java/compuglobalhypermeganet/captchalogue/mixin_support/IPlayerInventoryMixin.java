package compuglobalhypermeganet.captchalogue.mixin_support;

import compuglobalhypermeganet.captchalogue.FetchModusState;

public interface IPlayerInventoryMixin {
	public FetchModusState getFetchModus();
	
	// Called asynchronously from the main loop after the inventory changes
	public void captchalogue_afterInventoryChanged();
}
