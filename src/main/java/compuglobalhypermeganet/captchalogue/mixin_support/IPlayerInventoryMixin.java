package compuglobalhypermeganet.captchalogue.mixin_support;

import compuglobalhypermeganet.captchalogue.FetchModusType;

public interface IPlayerInventoryMixin {
	public FetchModusType getFetchModus();
	
	// Called asynchronously from the main loop after the inventory changes
	public void captchalogue_afterInventoryChanged();
}
