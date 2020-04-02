package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compuglobalhypermeganet.captchalogue.InventoryUtils;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerInventory;

@Environment(EnvType.CLIENT)
@Mixin(PlayerInventory.class)
public class PlayerInventoryClientMixin {

	private int lastSelectedSlot;
	
	@Inject(at = @At("HEAD"), method = "scrollInHotbar(D)V")
	public void beforeScrollInHotbar(CallbackInfo info) {
		lastSelectedSlot = ((PlayerInventory)(Object)this).selectedSlot;
	}
	
	@Inject(at = @At("RETURN"), method = "scrollInHotbar(D)V")
	public void afterScrollInHotbar(CallbackInfo info) {
		PlayerInventory inv = ((PlayerInventory)(Object)this);
		InventoryUtils.ensureSelectedSlotIsUnblocked(((IPlayerInventoryMixin)inv).getFetchModus(), inv, lastSelectedSlot, true);
		lastSelectedSlot = -1;
	}
	
}
