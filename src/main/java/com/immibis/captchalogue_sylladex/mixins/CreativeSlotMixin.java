package com.immibis.captchalogue_sylladex.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.immibis.captchalogue_sylladex.mixin_support.ICreativeSlotMixin;

import net.minecraft.container.Slot;

/**
 * Yes, we really need the super-interface, even though this is already an interface.
 * The mixin transformer won't allow us to do (something instanceof CreativeSlotMixin) on the server,
 * because it complains that CreativeSlotMixin is in the mixin package and can't be loaded. (But on the client, it works fine!) 
 */
@Mixin(targets = {"net/minecraft/client/gui/screen/ingame/CreativeInventoryScreen$CreativeSlot"})
public class CreativeSlotMixin implements ICreativeSlotMixin {
	// Doesn't build for some reason - can't find the mapping for "slot"
	//@Override
	//@Accessor("slot") public Slot captchalogue_getBaseSlot();
	
	@Shadow
	private Slot slot;
	
	@Override
	public Slot captchalogue_getBaseSlot() {
		return slot;
	}
}
