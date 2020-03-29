package compuglobalhypermeganet.captchalogue;

import net.minecraft.entity.player.PlayerInventory;

public interface IContainerMixin {
	public Object getFetchModusState(FetchModus modus, PlayerInventory inv);
}
