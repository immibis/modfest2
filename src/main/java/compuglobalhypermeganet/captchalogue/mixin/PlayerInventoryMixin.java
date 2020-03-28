package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventoryMixin {
	@Override
	public FetchModus getFetchModus() {
		return FetchModus.getModus((PlayerInventory)(Object)this);
	}
	
	@Inject(at = @At("HEAD"), method="insertStack(ILnet/minecraft/item/ItemStack;)Z", cancellable=true)
	public void overrideInsertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> info) {
		FetchModus modus = getFetchModus();
		if (modus.hasCustomInsert()) {
			// This returns true if any items were successfully inserted.
			// modus.insert updates the stack's count.
			// Slot number is ignored!
			int previousCount = stack.getCount();
			modus.insert((PlayerInventory)(Object)this, stack);
			info.setReturnValue(stack.getCount() < previousCount);
		}
	}
	@Inject(at = @At("HEAD"), method="offerOrDrop(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)V", cancellable=true)
	public void overrideOfferOrDrop(World world, ItemStack stack, CallbackInfo info) {
		if (!world.isClient()) {
			FetchModus modus = getFetchModus();
			if (modus.hasCustomInsert()) {
				modus.insert((PlayerInventory)(Object)this, stack);
				if (stack.getCount() == 0) {
					info.cancel();
				}
				// Proceed to the standard implementation; this may 
			}
		}
	}
}
