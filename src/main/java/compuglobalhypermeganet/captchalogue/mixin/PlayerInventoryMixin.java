package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import compuglobalhypermeganet.captchalogue.FetchModus.Queue;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventoryMixin {
	
	public FetchModus fetchModus;
	
	@Override
	public FetchModus getFetchModus() {return fetchModus;}
	
	@Inject(at = @At("TAIL"), method="<init>*")
	public void initFetchModus(CallbackInfo info) {
		fetchModus = new FetchModus.Queue(); // default modus type
	}
	
	@Inject(at = @At("HEAD"), method="serialize(Lnet/minecraft/nbt/ListTag;)Lnet/minecraft/nbt/ListTag;")
	public void serializeFetchModus(ListTag tag, CallbackInfoReturnable<ListTag> info) {
		if (fetchModus != null) {
			CompoundTag modusTag = FetchModus.serialize(fetchModus);
			modusTag.put("FETCH_MODUS_MARKER", ByteTag.of((byte)0));
			tag.add(modusTag);
		}
	}
	
	@Inject(at = @At("HEAD"), method="deserialize(Lnet/minecraft/nbt/ListTag;)V")
	public void deserializeFetchModus(ListTag tag, CallbackInfo info) {
		for(int k = tag.size() - 1; k >= 0; k--) {
			Tag child = tag.get(k);
			if (child instanceof CompoundTag && ((CompoundTag)child).contains("FETCH_MODUS_MARKER")) {
				((CompoundTag)child).remove("FETCH_MODUS_MARKER");
				fetchModus = FetchModus.deserialize((CompoundTag)child);
				tag.remove(k);
				break;
			}
		}
	}
}
