package compuglobalhypermeganet.captchalogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

public class FetchModusMemory extends FetchModus {
	@Override
	public void initialize(PlayerInventory inventory) {
	}
	
	@Override public boolean hasCustomInsert() {return false;}
	@Override public void insert(PlayerInventory inv, ItemStack stack) {throw new AssertionError("unreachable");}
	
	// Give the player two normal slots to hold items they can use.
	public static final int ABNORMAL_RANGE_START = 2;
	public static boolean isNormalSlot(int id) {
		return id < ABNORMAL_RANGE_START;
	}
	
	@Override
	public Object createContainerState(Container cont, PlayerInventory inv) {
		return new State(inv);
	}
	
	@Override
	public boolean overrideInventoryClick(Container cont, PlayerInventory inv, int slotIndex, SlotActionType actionType, int clickData) {
		
		if (isNormalSlot(slotIndex))
			return false; // no override
		
		if(actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) // normal click or shift-click on slot
			return true;
		
		State state = (State)((IContainerMixin)cont).getFetchModusState(this, inv);
		
		// Client generates random seed; sends it on every click (in ClickWindowC2SPacket); server copies it.
		// If the client has changed the random seed then the server re-randomizes its inventory mapping based on the new seed.
		if (inv.player.world.isClient()) {
			FetchModus.currentPacketFetchModusState.set(state.randomSeed);
		} else if (FetchModus.currentPacketFetchModusState.get() != state.randomSeed) {
			state.setup(inv, FetchModus.currentPacketFetchModusState.get());
		}
		
		if (!inv.getCursorStack().isEmpty()) {
			if (clickData == 1) {
				// right-click deposits one item
				ItemStack depositStack = inv.getCursorStack().copy();
				depositStack.setCount(1);
				InventoryUtils.insertStack(inv, depositStack, ABNORMAL_RANGE_START, 36);
				if (depositStack.getCount() == 0)
					inv.getCursorStack().decrement(1);
			} else {
				InventoryUtils.insertStack(inv, inv.getCursorStack(), ABNORMAL_RANGE_START, 36);
			}
			return true;
		}
		
		// If a match was previously made, allow the item to be picked up! and reset the selection state immediately.
		if (state.haveMatch(inv) && (slotIndex == state.revealedSlot1 || slotIndex == state.revealedSlot2)) {
			// randomLayout[slotIndex] can't be -1 because then we wouldn't have a match.
			
			int underlyingSlotId = state.randomLayout[slotIndex];
			ItemStack clickedStack = inv.getInvStack(underlyingSlotId);
			if (clickData == 1 || clickedStack.getCount() < 2) {
				// right-click withdraws half stack (rounded up)
				
				inv.setCursorStack(clickedStack.split((clickedStack.getCount() + 1) / 2));
				
				// don't reset the game when only we withdraw half.
				// But do hide the one that wasn't clicked on. This allows the player to withdraw more, but only from the same slot.
				if (state.revealedSlot1 == slotIndex)
					state.revealedSlot2 = -1;
				else
					state.revealedSlot1 = -1;
				
			} else {
				inv.setCursorStack(clickedStack);
				inv.setInvStack(underlyingSlotId, ItemStack.EMPTY);
				
				state.revealedSlot1 = -1;
				state.revealedSlot2 = -1;
				state.timeoutAt = 0;
			}
			
			return true;
		}
		
		if (state.timeoutAt != 0 || state.haveMatch(inv)) {
			// timeout fires immediately
			state.revealedSlot1 = -1;
			state.revealedSlot2 = -1;
			state.timeoutAt = 0;
		}
		
		if (slotIndex == state.revealedSlot2)
			return true; // click same slot twice - no-op
		
		state.revealedSlot1 = state.revealedSlot2;
		state.revealedSlot2 = slotIndex;
		
		if (state.revealedSlot1 >= 0 || (state.randomLayout[slotIndex] < 0 || inv.getInvStack(state.randomLayout[slotIndex]).isEmpty())) {
			// Time out display after a short time.
			// If the next click is close to the two-second timeout, there could be a server/client desync...
			// TODO: use the existing mechanism to prevent this possibility?
			// TODO: don't time out on the server; instead, just tell it whether the client timed out or not.<

			if (!state.haveMatch(inv)) // After the player matches two items, they have unlimited time to grab them.
				state.timeoutAt = System.nanoTime() + 1000000000L;
		}
		
		if (state.haveMatch(inv) && inv.player.world.isClient()) {
			// Play the "experience level gained" jingle
			inv.player.world.playSound(
				inv.player,
				inv.player.getX(), inv.player.getY(), inv.player.getZ(),
				SoundEvents.ENTITY_PLAYER_LEVELUP,
				inv.player.getSoundCategory(),
				0.5F, 1.0F
			);
		}
		return true;
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public boolean overrideDrawSlot(ContainerScreen<?> screen, int screenX, int screenY, Slot slot, PlayerInventory inv, int slotIndex, int cursorX, int cursorY) {
		
		if (isNormalSlot(slotIndex))
			return false; // no override
		
		State state = (State)((IContainerMixin)screen.getContainer()).getFetchModusState(this, inv);
		
		// really hacky place to put the timeout check!
		if (state.timeoutAt != 0 && System.nanoTime() - state.timeoutAt > 0) {
			state.revealedSlot1 = -1;
			state.revealedSlot2 = -1;
			state.timeoutAt = 0;
		}
		
		if(slotIndex == state.revealedSlot1 || slotIndex == state.revealedSlot2) {
			// draw the item in the slot
			
			ItemStack stack = (state.randomLayout[slotIndex] < 0 ? ItemStack.EMPTY : inv.main.get(state.randomLayout[slotIndex]));
			
			if (cursorX >= slot.xPosition && cursorY >= slot.yPosition && cursorX < slot.xPosition+16 && cursorY < slot.yPosition+16) {
				// Mostly copied from the normal slot hover highlight code
				RenderSystem.disableDepthTest();
				//RenderSystem.colorMask(true, true, true, false);
				float x1 = slot.xPosition, y1 = slot.yPosition, x2 = slot.xPosition+16, y2 = slot.yPosition+16;
				RenderSystem.disableTexture();
				RenderSystem.enableBlend();
				RenderSystem.disableAlphaTest();
				RenderSystem.defaultBlendFunc();
				RenderSystem.shadeModel(7425); // ???
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder bb = tessellator.getBuffer();
				bb.begin(7, VertexFormats.POSITION_COLOR);
				bb.vertex(x1, y1, 0).color(1.0f, 1.0f, 1.0f, 0.5f).next();
				bb.vertex(x2, y1, 0).color(1.0f, 1.0f, 1.0f, 0.5f).next();
				bb.vertex(x2, y2, 0).color(1.0f, 1.0f, 1.0f, 0.5f).next();
				bb.vertex(x1, y2, 0).color(1.0f, 1.0f, 1.0f, 0.5f).next();
				tessellator.draw();
				RenderSystem.shadeModel(7424); // ???
				RenderSystem.disableBlend();
				RenderSystem.enableAlphaTest();
				RenderSystem.enableTexture();

				//screen.fillGradient(slot.xPosition, slot.yPosition, slot.xPosition+16, slot.yPosition+16, -2130706433, -2130706433);
				//RenderSystem.colorMask(true, true, true, true);
				RenderSystem.enableDepthTest();
			}
			
			if (!stack.isEmpty()) {
				ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
				
				itemRenderer.zOffset = 100.0F;
	
				RenderSystem.enableDepthTest();
				itemRenderer.renderGuiItem(MinecraftClient.getInstance().player, stack, slot.xPosition, slot.yPosition);
				itemRenderer.renderGuiItemOverlay(MinecraftClient.getInstance().textRenderer, stack, slot.xPosition, slot.yPosition, null);
				itemRenderer.zOffset = 0.0f;
				// Leaves depth test enabled - this behaviour matches ContainerScreen.drawSlot
			
			} else {
				Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEX).apply(FetchModus.MEMORY_MODUS_CROSS_IMAGE);
				MinecraftClient.getInstance().getTextureManager().bindTexture(sprite.getAtlas().getId());
				DrawableHelper.blit(slot.xPosition, slot.yPosition, 0, 16, 16, sprite);
			}
			return true;
		}
		
		Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEX).apply(FetchModus.MEMORY_MODUS_QUESTION_MARK_IMAGE);
		MinecraftClient.getInstance().getTextureManager().bindTexture(sprite.getAtlas().getId());
		DrawableHelper.blit(slot.xPosition, slot.yPosition, 0, 16, 16, sprite);
		
		return true;
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public Slot overrideFocusedSlot(ContainerScreen<?> screen, PlayerInventory inv, int slot, Slot focusedSlot) {
		if (isNormalSlot(slot) || slot == MODUS_SLOT)
			return focusedSlot;
		State state = (State)((IContainerMixin)screen.getContainer()).getFetchModusState(this, inv);
		if (slot == state.revealedSlot1 || slot == state.revealedSlot2) {
			// Look for the real slot which holds the item we are hovering over
			for(Slot otherSlot : screen.getContainer().slots) {
				if(otherSlot.inventory == inv && ((ISlotMixin)otherSlot).captchalogue_getSlotNum() == state.randomLayout[slot])
					return otherSlot;
			}
			return null;
		}
		return null;
	}
	
	@Override
	public boolean canInsertToSlot(PlayerInventory inv, int slot) {
		return isNormalSlot(slot);
	}
	@Override
	public boolean canTakeFromSlot(PlayerInventory inv, int slot) {
		return isNormalSlot(slot);
	}
	
	public static class State {
		// [MODUS_SLOT] is unused.
		// Maps display slot number -> underlying slot number.
		public int[] randomLayout = new int[36];
		
		public int revealedSlot1 = -1;
		public int revealedSlot2 = -1;
		
		public long timeoutAt;
		
		public int randomSeed;
		
		public State(PlayerInventory inv) {
			setup(inv, inv.player.world.random.nextInt());
		}
		
		public void setup(PlayerInventory inv, int randomSeed) {
			this.randomSeed = randomSeed;
			revealedSlot1 = revealedSlot2 = -1;
			timeoutAt = 0;
			
			// java.util.Random is guaranteed to use the same algorithm on all Java implementations.
			Random random = new Random(randomSeed);

			List<Integer> availableLayoutSlots = new ArrayList<>();
			for(int k = 0; k < 36; k++)
				if(k != MODUS_SLOT && !isNormalSlot(k))
					availableLayoutSlots.add(k);
			// TODO: is Collections.shuffle(Random) always the same algorithm in different Java implementations?
			Collections.shuffle(availableLayoutSlots, random);
			
			List<Integer> usedSlots = new ArrayList<>();
			for(int k = 0; k < 36; k++)
				if (!inv.main.get(k).isEmpty() && k != MODUS_SLOT && !isNormalSlot(k))
					usedSlots.add(k);
			// If the player has used too few inventory slots, the memory game matrix will have empty squares.
			// With 12 usedSlots, each slot (including the empty ones we're about to add) occurs roughly 3 times on the game board.
			while(usedSlots.size() < availableLayoutSlots.size() / 3)
				usedSlots.add(-1);
			// If the player has used too MANY inventory slots, cull some. They might not be able to retrieve every item without reopening the GUI!
			// 17 is the maximum to ensure that every available slot can be matched (it must occur at least twice in the grid).
			while(usedSlots.size() > availableLayoutSlots.size() / 2)
				usedSlots.remove(random.nextInt(usedSlots.size()));
			
			// Assign every usedSlot to 2 random game tiles
			for(int slotID : usedSlots) {
				randomLayout[availableLayoutSlots.remove(availableLayoutSlots.size() - 1)] = slotID;
				randomLayout[availableLayoutSlots.remove(availableLayoutSlots.size() - 1)] = slotID;
			}
			// Assign all remaining game tiles to a random slot (even one that's already assigned more than twice)
			for(int layoutSlot : availableLayoutSlots)
				randomLayout[layoutSlot] = usedSlots.get(random.nextInt(usedSlots.size()));
			
			// TODO: remove debug print
			//System.out.println((inv.player.world.isClient() ? "client" : "server")+": "+randomSeed+" "+Arrays.toString(randomLayout));
			//System.out.println((inv.player.world.isClient() ? "client" : "server")+": "+inv.player.getEntityId()+" "+cont.syncId);
		}
		
		// Note: A match happens if two identical item stacks are revealed, even if they're not actually the same underlying inventory slot.
		// Otherwise it would be too confusing for the player.
		public boolean haveMatch(PlayerInventory inv) {
			if (revealedSlot1 < 0 || revealedSlot2 < 0)
				return false;
			if (randomLayout[revealedSlot1] < 0 || randomLayout[revealedSlot2] < 0)
				return false;
			return ItemStack.areItemsEqual(inv.getInvStack(randomLayout[revealedSlot1]), inv.getInvStack(randomLayout[revealedSlot2]));
		}
	}

	@Override
	public boolean affectsHotbarRendering() {
		return true;
	}
	@Override
	public boolean hidesHotbarSlot(int k) {
		return k >= ABNORMAL_RANGE_START;
	}
	@Override
	protected boolean blocksAccessToHotbarSlot_(int slot) {
		return hidesHotbarSlot(slot);
	}

	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
}