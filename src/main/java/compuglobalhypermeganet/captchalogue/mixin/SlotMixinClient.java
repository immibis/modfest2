package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Pair;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.mixin_support.ISlotMixin;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;

@Mixin(Slot.class)
public class SlotMixinClient {
	@Shadow @Final public Inventory inventory;

	@Inject(at = @At(value="HEAD"), method="getBackgroundSprite()Lcom/mojang/datafixers/util/Pair;", cancellable=true)
	public void getBackgroundSprite(CallbackInfoReturnable<Pair<Identifier, Identifier>> info) {
		if (inventory instanceof PlayerInventory) {
			if(((ISlotMixin)this).captchalogue_getSlotNum() == CaptchalogueMod.MODUS_SLOT) {
				info.setReturnValue(new Pair<Identifier, Identifier>(SpriteAtlasTexture.BLOCK_ATLAS_TEX, CaptchalogueMod.MODUS_SLOT_BG_IMAGE));
			}
		}
	}
	
}
