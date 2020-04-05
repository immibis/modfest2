package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.container.Slot;

@Mixin(targets = {"net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen$CreativeSlot"})
public interface CreativeSlotMixin {
	@Accessor("slot") public Slot captchalogue_getBaseSlot();
}
