package com.immibis.captchalogue_sylladex;

import com.immibis.captchalogue_sylladex.mixin_support.IContainerScreenMixin;
import com.immibis.captchalogue_sylladex.mixin_support.IPlayerInventoryMixin;
import com.immibis.captchalogue_sylladex.mixin_support.ISlotMixin;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * The inventory stores is represented in the inventory by storing the items in the order they were inserted.
 * The tree is constructed on demand.
 * In leaf mode, slots 0-7 come last, so that the first 8 leaves correspond to the hotbar. In root mode, they come in numeric order.
 */
public class FetchModusTree extends FetchModusType {
	
	private boolean isRootMode;
	public FetchModusTree(boolean isRootMode) {
		this.isRootMode = isRootMode;
	}
	
	public static class Node {
		public Node parent, left, right, continuation;
		public int invSlot;
		public ItemStack stack;
		
		public Node layoutSibling, layoutNextLevel; // linked lists used in layout algorithm
		
		public boolean isVisible;
		
		public int layoutX;
		public int layoutY;
		
		// TODO: display continuation as increased item count
		
		boolean isContinuation() {
			return parent != null && parent.continuation == this;
		}
	}
	
	public static class InventoryTree {
		public Node root;
		public InventoryWrapper inv;
		public Node[] nodeBySlot = new Node[35];
		public boolean isRootMode;
		
		public InventoryTree(InventoryWrapper inv, boolean isRootMode) {
			this.inv = inv;
			this.isRootMode = isRootMode;
			this.nodeBySlot = new Node[inv.getNumSlots()];
			for(int k = 0; k < nodeBySlot.length; k++) {
				nodeBySlot[k] = new Node();
				nodeBySlot[k].invSlot = k;
				nodeBySlot[k].isVisible = false;
			}
			refreshNodeTree();
		}
		
		public void insert(Node parent, Node n) {
			int compareResult = CaptchalogueMod.compareItemsForTree(n.stack, parent.stack);
			if(compareResult == 0) {
				// continuation holds identical items which are coalesced into one node for display
				Node next = parent;
				while(next.continuation != null)
					next = next.continuation;
				next.continuation = n;
				n.parent = next;
				//parent.stack.increment(n.stack.getCount()); // TODO: display all continuations in the base node
			} else if (compareResult < 0) {
				if(parent.left == null) {
					parent.left = n;
					n.parent = parent;
				} else
					insert(parent.left, n);
			} else {
				if(parent.right == null) {
					parent.right = n;
					n.parent = parent;
				} else
					insert(parent.right, n);
			}
		}
		
		private static final int[] ROOT_MODE_SLOT_MAP = new int[35];
		private static final int[] LEAF_MODE_SLOT_MAP = new int[35];
		static {
			for(int k = 0; k < 35; k++) {
				ROOT_MODE_SLOT_MAP[k] = k;
				LEAF_MODE_SLOT_MAP[k] = (k<27 ? k+8 : k-27); // leaf nodes last
			}
		}
		
		public void refreshNodeTree() {
			for(Node n : nodeBySlot) {
				n.parent = n.left = n.right = n.continuation = null;
				n.isVisible = false;
				n.stack = null;
			}
			root = null;
			
			int[] SLOTMAP = isRootMode ? ROOT_MODE_SLOT_MAP : LEAF_MODE_SLOT_MAP;
			
			for(int k = 0; k < inv.getNumSlots(); k++) {
				ItemStack stack = inv.getInvStack(SLOTMAP[k]);
				if(!stack.isEmpty()) {
					Node n = nodeBySlot[SLOTMAP[k]];
					n.stack = stack.copy();
					if(root == null) {
						root = n;
					} else {
						insert(root, n);
					}
					if (!n.isContinuation())
						n.isVisible = true;
				}
			}
		}

		public boolean canTakeFromSlot(boolean isRootMode, int slot) {
			if(isRootMode) {
				return root != null && root == nodeBySlot[slot];
			} else {
				// leaf mode
				Node n = nodeBySlot[slot];
				return n.isVisible && n.left == null && n.right == null;
			}
		}

		public Node findUnusedNode() {
			for(Node n : nodeBySlot)
				if(n.stack == null) {
					if(n.parent != null || n == root) throw new AssertionError();
					if(n.isVisible) throw new AssertionError();
					return n;
				}
			return null;
		}

		private int rewriteLeafPosition;
		private int rewriteLeafBreakPoint;
		private int rewriteNonLeafPosition;
		private void rewriteStepDepthFirst(Node node, boolean isContinuation) {
			if(node == null)
				return;
			int nodeSlotIndex;
			if (node.left == null && node.right == null && !isContinuation) {
				// Leaf slot; prefer hotbar
				if (rewriteLeafPosition < rewriteLeafBreakPoint)
					nodeSlotIndex = rewriteLeafPosition++;
				else {
					nodeSlotIndex = rewriteNonLeafPosition++;
					if(nodeSlotIndex >= 35) // XXX hardcoded maximum
						throw new AssertionError("can't happen - no free slots");
				}
			} else {
				// Non-leaf slot; prefer main inventory
				if (rewriteNonLeafPosition < 35) // XXX hardcoded maximum
					nodeSlotIndex = rewriteNonLeafPosition++;
				else {
					nodeSlotIndex = rewriteLeafPosition++;
					if(nodeSlotIndex >= rewriteLeafBreakPoint)
						throw new AssertionError("can't happen - no free slots");
				}
			}
			inv.setInvStack(nodeSlotIndex, node.stack);
			rewriteStepDepthFirst(node.continuation, true);
			rewriteStepDepthFirst(node.left, false);
			rewriteStepDepthFirst(node.right, false);
		}
		public void rewriteUnderlyingInventory() {
			if(isRootMode) {
				// Root mode: Slot 0 gets the root node, slot 1-34 get all the rest, in a valid insertion order (either breadth-first or depth-first will work)
				rewriteLeafPosition = 0;
				rewriteLeafBreakPoint = 0;
				rewriteNonLeafPosition = 0;
			} else {
				// Write first 8 leaf nodes to slots 0-7, and remainder of nodes to slots 8-34.
				// Leaf nodes can also go in the non-leaf node section if there are too many.
				// XXX: hardcoded hotbar size (8)
				rewriteLeafPosition = 0;
				rewriteLeafBreakPoint = 8;
				rewriteNonLeafPosition = 8;
			}
			
			rewriteStepDepthFirst(root, false);
			
			// Clear unused slots
			for(int k = rewriteLeafPosition; k < rewriteLeafBreakPoint; k++)
				inv.setInvStack(k, ItemStack.EMPTY);
			for(int k = rewriteNonLeafPosition; k < inv.getNumSlots(); k++)
				inv.setInvStack(k, ItemStack.EMPTY);
			
			refreshNodeTree(); // nodes have all moved positions; need to refresh tree with the correct node for each slot 
		}
		
		private void replaceNodePositionInTree(Node n, Node with) {
			if(n.parent == null) {
				if(root == n)
					root = with;
			} else {
				if(n == n.parent.left)
					n.parent.left = with;
				if(n == n.parent.right)
					n.parent.right = with;
				n.parent = null;
			}
			if(n.left != null) {
				with.left = n.left;
				n.left.parent = with;
				n.left = null;
			}
			if(n.right != null) {
				with.right = n.right;
				n.right.parent = with;
				n.right = null;
			}
		}
		
		private void dropSubtreeItems(Node node) {
			if(node == null)
				return;
			
			ItemStack stack = inv.getInvStack(node.invSlot);
			if(!stack.isEmpty()) {
				inv.setInvStack(node.invSlot, ItemStack.EMPTY);
				inv.getPlayer().dropItem(stack, false, true);
			}
			
			dropSubtreeItems(node.left);
			dropSubtreeItems(node.right);
			node.left = null;
			node.right = null;
		}
		
		private void dropEmptySlotChildren(Node node) {
			if(node == null)
				return;
			if (inv.getInvStack(node.invSlot).isEmpty()) {
				if(node.continuation != null) {
					replaceNodePositionInTree(node, node.continuation);
					dropEmptySlotChildren(node.continuation);
					node.continuation = null;
				} else {
					dropSubtreeItems(node);
					if(node.parent == null) {
						if(root == node)
							root = null;
					} else {
						if(node.parent.left == node)
							node.parent.left = null;
						if(node.parent.right == node)
							node.parent.right = null;
					}
					return;
				}
			}
			dropEmptySlotChildren(node.left);
			dropEmptySlotChildren(node.right);
		}
		public void dropEmptySlotChildren() {dropEmptySlotChildren(root);}
	}
	
	// TODO: setInvStack should invalidate the tree structure (especially when sent from the server). Otherwise you have to wait for the next refresh based on forceRecalcFrames
	
	public static class GuiState extends FetchModusGuiState {
		
		private static final int SCROLLBAR_WIDTH=16;
		
		private InventoryWrapper inv;
		
		private float scrollPosition = 0;
		private float maxScrollPosition = 0;
		
		Container cont;
		State invState;
		
		public GuiState(Container cont, InventoryWrapper inv, boolean isRootMode) {
			this.cont = cont;
			this.inv = inv;
			// TODO: could this possibly crash when changing moduses?
			this.invState = (State)((IPlayerInventoryMixin)inv.getPlayer().inventory).getFetchModus();
		}
		
		private void setupLayoutLinkedLists() {
			for(Node n : invState.tree.nodeBySlot) {
				n.layoutNextLevel = null;
				n.layoutSibling = null;
			}
			
			Node currentLevelStart = invState.tree.root;
			while(currentLevelStart != null) {
				Node nextLevelStart = null;
				Node nextLevelLast = null;
				
				for(Node n = currentLevelStart; n != null; n = n.layoutSibling) {
					if (n.left != null) {
						if(nextLevelStart == null)
							nextLevelStart = n.left;
						else
							nextLevelLast.layoutSibling = n.left;
						nextLevelLast = n.left;
					}
					if (n.right != null) {
						if(nextLevelStart == null)
							nextLevelStart = n.right;
						else
							nextLevelLast.layoutSibling = n.right;
						nextLevelLast = n.right;
					}
				}
				
				currentLevelStart.layoutNextLevel = nextLevelStart;
				currentLevelStart = nextLevelStart;
			}
		}
		
		private void calculateLayoutTopDown(Node level, int y, float x1, float x2) {
			setupLayoutLinkedLists();
			while(level != null) {
				float count = 0;
				for(Node n = level; n != null; n = n.layoutSibling) count++;
				
				float k = 0;
				for(Node n = level; n != null; n = n.layoutSibling, k++) {
					n.layoutY = y;
					n.layoutX = (int)((k + 0.5f) / count * (x2 - x1) + x1 + 0.5);
				}
				
				y += 30;
				
				level = level.layoutNextLevel;
			}
		}
		
		/* Old layout algorithm
		private void calculateLayoutTopDown(Node n, int y, float x1, float x2) {
			n.layoutX = (int)((x1 + x2) / 2 - 8);
			n.layoutY = y;
			
			y += 32;
			
			if(n.left != null)
				calculateLayoutTopDown(n.left, y, x1, n.layoutX + 8);
			if(n.right != null)
				calculateLayoutTopDown(n.right, y, n.layoutX + 8, x2);
		} // */
		
		IContainerScreenMixin contScreen;
		boolean maybeChanged = false;
		@Override
		public void onBeforeDraw(IContainerScreenMixin contScreen) {
			this.contScreen = contScreen; // XXX hacky place to initialize this!
			
			// TODO: don't recalculate layout every frame
			if (invState.tree.root != null) {
				// 18 pixels on the left for the modus slot
				// 18 pixels on the right to account for the slot size. 2 pixels on the left for borders.
				calculateLayoutTopDown(invState.tree.root, (int)area.y+5, (float)area.x + 18 + 2, (float)(area.x + area.width - 18 - SCROLLBAR_WIDTH));
			}
			
			int maxLayoutY = 0;
			for(Slot s : cont.slots) {
				if(s.inventory instanceof PlayerInventory) {
					int slotIndex = ((ISlotMixin)s).captchalogue_getSlotNum();
					if (slotIndex < 0 || slotIndex > 35)
						continue;
					if(slotIndex == CaptchalogueMod.MODUS_SLOT) {
						((ISlotMixin)s).captchalogue_setPosition((int)area.x+1, (int)area.y+1);
						continue;
					}
					if(slotIndex > CaptchalogueMod.MODUS_SLOT)
						slotIndex--;
					
					Node n = invState.tree.nodeBySlot[slotIndex];
					if(!n.isVisible) {
						((ISlotMixin)s).captchalogue_setPosition(-1000, -1000); // XXX hack
					} else {
						((ISlotMixin)s).captchalogue_setPosition(n.layoutX, n.layoutY - (int)scrollPosition);
						maxLayoutY = Math.max(maxLayoutY, n.layoutY);
					}
				}
			}
			
			maxScrollPosition = Math.max(0, maxLayoutY - (int)area.y + 24 - (int)area.height);
			scrollPosition = Math.min(scrollPosition, maxScrollPosition);
		}
		
		// TODO: shift-clicking on a non-inventory slot doesn't update the tree structure.
		
		private void appendLine(Drawer d, Node from, float fx1, float fx2, Node to, float tx1, float tx2) {
			float COL = Drawer.COL_LEFT_TOP;
			
			//float sp = scrollPosition;
			//d.appendVertex(from.layoutX+fx1, from.layoutY+17-sp, COL, COL, COL, 1.0f);
			//d.appendVertex(from.layoutX+fx2, from.layoutY+17-sp, COL, COL, COL, 1.0f);
			//d.appendVertex(  to.layoutX+tx2,   to.layoutY- 1-sp, COL, COL, COL, 1.0f);
			//d.appendVertex(  to.layoutX+tx1,   to.layoutY- 1-sp, COL, COL, COL, 1.0f);

			float x1 = from.layoutX+8;
			float x2 = to.layoutX+8;
			// scrollPosition converted to int for consistency with the slot positions. Otherwise the slots scroll more coarsely than the connector lines.
			float y1 = from.layoutY+8-(int)scrollPosition;
			float y2 = to.layoutY+8-(int)scrollPosition;
			
			float len = (float)Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
			
			// normal vector to add thickness to the line
			float nx = (y2 - y1)/len;
			float ny = (x1 - x2)/len;
			
			d.appendVertex(x1+nx, y1+ny, COL, COL, COL, 1.0f);
			d.appendVertex(x1-nx, y1-ny, COL, COL, COL, 1.0f);
			d.appendVertex(x2-nx, y2-ny, COL, COL, COL, 1.0f);
			d.appendVertex(x2+nx, y2+ny, COL, COL, COL, 1.0f);
		}
		
		@Override
		public void drawAdditionalBackground(Drawer d) {
			
			d.restrictRendering((int)area.x, (int)area.y, (int)(area.x+area.width), (int)(area.y+area.height));
			d.beginRenderingSolidQuads();
			d.drawBorder(area.x+18, area.y, area.x+area.width-SCROLLBAR_WIDTH, area.y+area.height);
			// TODO: do we really need to loop over container slots? Can't we loop over tree.nodeBySlot instead?
			for(Slot s : cont.slots) {
				if(s.inventory instanceof PlayerInventory) {
					int slotIndex = ((ISlotMixin)s).captchalogue_getSlotNum();
					if(slotIndex == CaptchalogueMod.MODUS_SLOT || slotIndex < 0 || slotIndex > 35)
						continue;
					if(slotIndex > CaptchalogueMod.MODUS_SLOT)
						slotIndex--;
					
					Node n = invState.tree.nodeBySlot[slotIndex];
					if (n.isVisible) {
						// draw slot background
						/*d.appendSolidQuad(n.layoutX-1, n.layoutY-1, n.layoutX+17, n.layoutY+17, Drawer.COL_SLOT, Drawer.COL_SLOT, Drawer.COL_SLOT, 1.0f);
						d.appendSolidQuad(n.layoutX-1, n.layoutY-1, n.layoutX+16, n.layoutY+16, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, Drawer.COL_LEFT_TOP, 1.0f);
						d.appendSolidQuad(n.layoutX  , n.layoutY  , n.layoutX+17, n.layoutY+17, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, Drawer.COL_RIGHT_BOTTOM, 1.0f);
						d.appendSolidQuad(n.layoutX  , n.layoutY  , n.layoutX+16, n.layoutY+16, Drawer.COL_SLOT, Drawer.COL_SLOT, Drawer.COL_SLOT, 1.0f);*/
						
						// drawn afterwards
						//d.drawBorder(n.layoutX-1, n.layoutY-1-(int)scrollPosition, n.layoutX+17, n.layoutY+17-(int)scrollPosition);
						
						if (n.left != null) {
							// draw line to left child
							appendLine(d, n, 4, 6, n.left, 7, 9);
						}
						if (n.right != null) {
							// draw line to right child
							appendLine(d, n, 10, 12, n.right, 7, 9);
						}
					}
				}
			}
			
			// Draw slot backgrounds
			for(Node n : invState.tree.nodeBySlot) {
				if(n.isVisible) {
					d.drawBorder(n.layoutX-1, n.layoutY-1-(int)scrollPosition, n.layoutX+17, n.layoutY+17-(int)scrollPosition);
				}
			}
			
			// draw scrollbar
			if(maxScrollPosition > 0) {
				d.drawBorder(area.x+area.width-SCROLLBAR_WIDTH, area.y, area.x+area.width, area.y+area.height);
				final int SCROLL_THUMB_HEIGHT = 10;
				int scrollbarTop = (int)((area.height - 2 - SCROLL_THUMB_HEIGHT) * (scrollPosition / maxScrollPosition) + 0.5);
				int scrollbarBottom = scrollbarTop + SCROLL_THUMB_HEIGHT;
				d.drawInverseBorder(area.x+area.width-SCROLLBAR_WIDTH+1, area.y+1+scrollbarTop, area.x+area.width-1, area.y+1+scrollbarBottom);
			}
			
			d.endRenderingSolidQuads();
			d.unrestrictRendering();
		}
		
		private int savedSlotStackCount;
		@Override
		public void beforeDrawSlot(Slot slot, Drawer d) {
			savedSlotStackCount = slot.getStack().getCount();
			
			int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
			if(slotIndex >= 0 && slotIndex < 36 && slotIndex != CaptchalogueMod.MODUS_SLOT) {
				if(slotIndex >= CaptchalogueMod.MODUS_SLOT)
					slotIndex--;
				
				int totalCount = 0;
				for(Node node = invState.tree.nodeBySlot[slotIndex]; node != null; node = node.continuation)
					totalCount += inv.getInvStack(node.invSlot).getCount();
				
				// Only called on the client, so there's no risk of item duplication by doing this, only desyncs
				slot.getStack().setCount(totalCount);
			}
			
			d.restrictRendering((int)area.x, (int)area.y+1, (int)(area.x+area.width-SCROLLBAR_WIDTH-1), (int)(area.y+area.height-1));
		}
		@Override
		public void afterDrawSlot(Slot slot, Drawer d) {
			if(!slot.getStack().isEmpty())
				slot.getStack().setCount(savedSlotStackCount);
			d.unrestrictRendering();
		}
		
		@Override
		public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
			if(maxScrollPosition <= 0)
				return false;
			if(x >= area.x+area.width-SCROLLBAR_WIDTH && y >= area.y && x < area.x+area.width && y < area.y+area.height) {
				final int DEADZONE = 3;
				scrollPosition = (float)(((y - DEADZONE) - area.y) / (area.height - DEADZONE*2) * maxScrollPosition);
				if(scrollPosition < 0) scrollPosition = 0;
				if(scrollPosition > maxScrollPosition) scrollPosition = maxScrollPosition;
				return true;
			}
			return false;
		}
		@Override
		public boolean mouseClicked(double x, double y, int button) {
			// If user clicks in the scrollable area but outside of the any slot, treat it as a click on any empty slot (which triggers insert())
			// Iterating backwards slightly decreases the chance of clicking on a non-empty slot because of a conflict, hopefully, since items are added from the bottom up.
			// It's also slightly faster since the last slot is most likely to be empty, since we add from the beginning.
			if(x >= area.x+18 && y >= area.y && x < area.x+area.width-SCROLLBAR_WIDTH && y < area.y+area.height && !inv.getPlayer().inventory.getCursorStack().isEmpty() && contScreen != null && contScreen.captchalogue_getSlotAt(x, y) == null) {
				for(int k = cont.slots.size() - 1; k >= 0; k--) {
					Slot s = cont.slots.get(k);
					if(s.inventory instanceof PlayerInventory) {
						int slot = ((ISlotMixin)s).captchalogue_getSlotNum();
						if(slot >= 0 && slot < 36 && slot != CaptchalogueMod.MODUS_SLOT && s.getStack().isEmpty()) {
							contScreen.captchalogue_onMouseClick(s, k, button, SlotActionType.PICKUP);
							return true;
						}
					}
				}
				return false;
			}
			return mouseDragged(x,y,button,0,0);
		}
		// TODO: scroll wheel support!
		
		@Override
		public int overridesIsPointOverSlot(Slot slot, double x, double y) {
			// can't hover over slots when outside of the visible area
			int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
			if(slotIndex >= 0 && slotIndex < 36 && slotIndex != CaptchalogueMod.MODUS_SLOT) {
				if (y < area.y || y >= area.y+area.height)
					return 0;
			}
			return -1;
		}
	}
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if (slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		return BG_GROUP_INVISIBLE;
	}

	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
	
	@Override
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return new State(inv, isRootMode);
	}
	
	public static class State extends FetchModusState {
		private InventoryWrapper inv;
		private boolean isRootMode;
		private InventoryTree tree;
		public State(InventoryWrapper inv, boolean isRootMode) {
			this.inv = inv;
			this.isRootMode = isRootMode;
			tree = new InventoryTree(inv, isRootMode);
		}
		
		@Override public void initialize() {
			tree.refreshNodeTree();
			tree.rewriteUnderlyingInventory();
		}
		
		@Override public boolean hasCustomInsert() {return true;}
		@Override public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			// note: tree modus doesn't do violent expulsion
			if(stack.isEmpty())
				return;
			
			// First, try to merge with any existing stacks. Inserting to existing stacks in a tree is allowed and doesn't affect the structure.
			// Order doesn't really matter, since identical items are combined into the same visual tree node. The user can access all stacks of identical items, or none of them.
			for(int k = 0; k < inv.getNumSlots(); k++) {
				ItemStack stackInSlot = inv.getInvStack(k);
				if(!stackInSlot.isEmpty() && Container.canStacksCombine(stack, stackInSlot)) {
					int nt = Math.min(stack.getCount(), stackInSlot.getMaxCount() - stackInSlot.getCount());
					if(nt > 0) {
						stackInSlot.increment(nt);
						stack.decrement(nt);
						if(stack.isEmpty())
							return;
					}
				}
			}
			
			// Now we have some items that couldn't be merged.
			
			Node node = tree.findUnusedNode();
			if(node == null)
				return; // inventory full; can't insert remaining items
			
			node.stack = stack.copy(); // This corrupts the tree structure; note the item ISN'T in the underlying inventory
			stack.setCount(0);
			
			if(tree.root == null)
				tree.root = node;
			else
				tree.insert(tree.root, node);
			
			// Note at this point the item is in our tree structure, but not in the underlying inventory!
			tree.rewriteUnderlyingInventory();
			// now it is
		}
		
		@Override
		public boolean canInsertToSlot(int slot) {
			// Insert into filled slots is allowed (must be the same type of item). Insert into empty slots is allowed. Tree modus has no insert restrictions.
			// TODO: for queue/stack/etc we only wanted to enable one empty slot, for shift-clicking or something to work correctly? Do we also need that here?
			return true;
		}
		@Override
		public boolean canTakeFromSlot(int slot) {
			Node node = tree.nodeBySlot[slot];
			if(isRootMode)
				return node.isVisible && node == tree.root;
			else
				return node.isVisible && node.left == null && node.right == null; // continuation doesn't have to be null
		}

		@Override
		public boolean affectsHotbarRendering() {
			return true;
		}
		@Override
		public ItemStack modifyHotbarRenderItem(int slot, ItemStack stack) {
			if (blocksAccessToHotbarSlot_(slot) || stack.isEmpty())
				return ItemStack.EMPTY;
			
			// Display the total amount of available items in all continuation slots
			Node node = tree.nodeBySlot[slot];
			while(node.parent != null && node == node.parent.continuation)
				node = node.parent;
			if(node.continuation == null)
				return stack; // only one slot, no chained continuations, so the stack is unchanged
			
			int total = 0;
			while(node != null) {
				total += inv.getInvStack(node.invSlot).getCount();
				node = node.continuation;
			}
			stack = stack.copy();
			stack.setCount(total);
			return stack;
		}
		@Override
		protected boolean blocksAccessToHotbarSlot_(int slot) {
			if(isRootMode)
				return slot != 0;
			if(inv.getInvStack(slot).isEmpty())
				return false; // no reason to block empty slots if they're available.
			// Only unblock leaf slots and continuations thereof.
			// Non-leaf nodes might be placed in the hotbar if there isn't enough space in the main inventory.
			Node node = tree.nodeBySlot[slot];
			while(node.parent != null && node == node.parent.continuation)
				node = node.parent;
			return (node.left != null || node.right != null);
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			if (!serverSync)
				tree.dropEmptySlotChildren();
			tree.refreshNodeTree();
			if (!serverSync)
				tree.rewriteUnderlyingInventory();
		}
		
		@Override
		public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, int slotIndex, SlotActionType actionType, int clickData) {
			
			if(actionType != SlotActionType.PICKUP // normal click
			 && actionType != SlotActionType.QUICK_MOVE // shift-click
			 && actionType != SlotActionType.THROW) // press Q
				return true;
			
			if (!plinv.getCursorStack().isEmpty() && actionType == SlotActionType.PICKUP) {
				if (clickData == 1) {
					// right-click deposits one item
					ItemStack depositStack = plinv.getCursorStack().copy();
					depositStack.setCount(1);
					insert(depositStack, true);
					if (depositStack.getCount() == 0)
						plinv.getCursorStack().decrement(1);
				} else {
					insert(plinv.getCursorStack(), true);
				}
				return true;

			} else {
				if(!plinv.player.world.isClient())
					tree.refreshNodeTree(); // tree on the server doesn't auto-update; update it before each action
				if(!tree.canTakeFromSlot(isRootMode, slotIndex)) {
					return true; // block the click
				}
				
				Node node = tree.nodeBySlot[slotIndex];
				
				if(actionType == SlotActionType.PICKUP && (node.left != null || node.right != null)) {
					// Only left-clicks allowed on nodes that have children. Treat a right-click as a left-click.
					// The player must pick up all items in the stack at once, so that stuff underneath gets dropped.
					// (Leaf nodes can be right-clicked, since that's equivalent to taking the whole stack and then putting some stuff back)
					plinv.setCursorStack(inv.getInvStack(node.invSlot));
					inv.setInvStack(node.invSlot, ItemStack.EMPTY);
					return true;
				}
				
				return false; // allow the click
			}
		}
		
		@Override
		public FetchModusGuiState createGuiState(Container cont) {
			return new GuiState(cont, inv, isRootMode);
		}
	}
}