package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.InventoryWrapper;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	// After using an item, check whether the stack was used up, and notify the fetch modus.
	// Unfortunately Fabric's UseItemCallback is not suitable, because it's called too early. We want to run after the interaction.
	@Inject(at=@At("RETURN"), method="interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;")
	public void afterInteractBlock(PlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
		((IPlayerInventoryMixin)player.inventory).getFetchModus().afterPossibleInventoryChange(null, new InventoryWrapper.PlayerInventorySkippingModusSlot(player.inventory));
	}
}
