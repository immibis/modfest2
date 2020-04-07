package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.FetchModusState;
import compuglobalhypermeganet.captchalogue.InventoryWrapper;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@ModifyArg(at = @At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V"), method="renderHotbar(F)V", index=4)
	public ItemStack overrideRenderHotbarItem(int x, int y, float partialTicks, PlayerEntity player, ItemStack stack) {
		FetchModusState modus = ((IPlayerInventoryMixin)player.inventory).getFetchModus();
		if (modus.affectsHotbarRendering() && !stack.isEmpty()) {
			// We have to know which hotbar slot we're being passed.
			// We can't do this for empty stacks, since they're all the same.
			for (int k = 0; k < 9; k++) {
				if (stack == player.inventory.getInvStack(k)) {
					if (k == CaptchalogueMod.MODUS_SLOT)
						return stack;
					return modus.modifyHotbarRenderItem(InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(k), stack);
				}
			}
		}
		return stack;
	}
}
