package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	// Item use functions generally don't call setInvStack if they take items from an existing stack. Sometimes not even if it becomes empty.
	// So we have to consider that the inventory could have changed any time the player right-clicks.
	@Inject(at=@At("RETURN"), method="interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;")
	public void afterInteractBlock(PlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
		// Trigger hooks
		player.inventory.setInvStack(player.inventory.selectedSlot, player.inventory.getInvStack(player.inventory.selectedSlot));
	}
}
