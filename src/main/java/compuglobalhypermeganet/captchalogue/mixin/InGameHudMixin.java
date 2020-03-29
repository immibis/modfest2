package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(at = @At("HEAD"), method="renderHotbarItem(IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V", cancellable=true)
	public void overrideRenderHotbarItem(int x, int y, float partialTicks, PlayerEntity player, ItemStack stack, CallbackInfo info) {
		FetchModus modus = ((IPlayerInventoryMixin)player.inventory).getFetchModus();
		if (modus.affectsHotbarRendering() && !stack.isEmpty()) {
			// We have to know which hotbar slot we're being passed.
			// We can't do this for empty stacks, since they're all the same.
			for (int k = 0; k < 9; k++) {
				if (stack == player.inventory.getInvStack(k)) {
					if (k != FetchModus.MODUS_SLOT && modus.hidesHotbarSlot(k))
						info.cancel();
					return;
				}
			}
		}
	}
}
