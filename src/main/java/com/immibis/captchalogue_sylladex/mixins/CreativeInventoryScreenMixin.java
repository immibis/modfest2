package com.immibis.captchalogue_sylladex.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.immibis.captchalogue_sylladex.mixin_support.IContainerScreenMixin;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
	/**
	 * For some reason the CreativeInventoryScreen entirely ignores clicks on slots where canTakeItems is false.
	 * This normally doesn't affect the player inventory slots since they return true from canTakeItems, but we make some of the player inventory slots non-takeable (e.g. slots in a queue except for the first one).
	 * This would entirely break those slots, if we didn't patch it here.
	 * (This only affects the creative inventory screen)
	 */
	@Redirect(at=@At(value="INVOKE", target="Lnet/minecraft/container/Slot;canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z"), method="onMouseClick(Lnet/minecraft/container/Slot;IILnet/minecraft/container/SlotActionType;)V")
	public boolean canTakeItems(Slot slot, PlayerEntity player) {
		if(slot.inventory instanceof PlayerInventory)
			return true;
		return slot.canTakeItems(player);
	}
	
	@Inject(at=@At("RETURN"), method="setSelectedTab(Lnet/minecraft/item/ItemGroup;)V")
	private void afterSetSelectedTab(ItemGroup group, CallbackInfo info) {
		((IContainerScreenMixin)this).captchalogue_invalidateLayout();
	}
}
