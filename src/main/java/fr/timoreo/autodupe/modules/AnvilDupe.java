package fr.timoreo.autodupe.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AnvilDupe extends Module {
    private BlockPos target;
    private int startCount, prevSlot;
    private int first = -1;
    private boolean didDupe = false;
    private boolean pickingUp = false;
    private ItemStack toDupe = null;

    public AnvilDupe() {
        super(Categories.World, "auto-anvil-dupe", "Automatically dupes using anvil dupe");
    }


    //slot 0 = item to dupe
    //slot 1 = unused, + symb
    //slot 2 = result item

    private static boolean isAnvil(ItemStack is) {
        return is.getItem() == Items.DAMAGED_ANVIL || is.getItem() == Items.CHIPPED_ANVIL || is.getItem() == Items.ANVIL;
    }

    private static boolean isAnvil(Block b) {
        return b == Blocks.DAMAGED_ANVIL || b == Blocks.CHIPPED_ANVIL || b == Blocks.ANVIL;
    }

    @Override
    public void onActivate() {
        target = null;
        startCount = InvUtils.find(Items.OBSIDIAN).getCount();
        prevSlot = mc.player.getInventory().selectedSlot;
        didDupe = false;
        first = -1;
        toDupe = mc.player.getInventory().getStack(0).copy();
    }

    @Override
    public void onDeactivate() {
        if (mc.crosshairTarget == null) return;
        InvUtils.swap(prevSlot);
        toDupe = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.crosshairTarget == null) {
                toggle();
                return;
            }
            if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            BlockState state = mc.world.getBlockState(pos);

            if (isAnvil(mc.world.getBlockState(pos.down()).getBlock())) {
                //hey we already have an anvil !
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                return;
            }

            if (state.getMaterial().isReplaceable() || isAnvil(state.getBlock())) {
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            } else return;
        }

        if (PlayerUtils.distanceTo(target) > mc.interactionManager.getReachDistance()) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        if (mc.world.getBlockState(target).getMaterial().isReplaceable()) {
            FindItemResult echest = InvUtils.findInHotbar(AnvilDupe::isAnvil);
            didDupe = false;
            if (!echest.found()) {
                error("No Anvils in hotbar, disabling");
                toggle();
                return;
            }

            BlockUtils.place(target, echest, true, 0, true);
        }
        if (isAnvil(mc.world.getBlockState(target).getBlock())) {
            //start dupin :real_troll:

            if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
                //we are not in an inventory okay let's open the anvil
                ActionResult res = mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, (BlockHitResult) mc.crosshairTarget);
                if (res == ActionResult.SUCCESS) {
                    if (mc.player.currentScreenHandler instanceof AnvilScreenHandler) {
                        //opened !
                        doDupeTick();
                    }//hemmm let's... just wait til next tick

                }//idk something went wrong or idk, let's just wait til next tick

            } else if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler)) {
                //wtf you opened another inventory ? wow you probably wanna toggle off, here
                toggle();
            } else {
                doDupeTick();
            }
        }
    }

    private void doDupeTick() {
        if (mc.player.currentScreenHandler instanceof AnvilScreenHandler) {
            if (pickingUp) {
                //and drop it
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
                pickingUp = false;
                return;
            }

            if (didDupe) {
                //dupe didn't trigger :( let's try again
                //pick up first hotbar item
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, 0, SlotActionType.PICKUP, mc.player);
                pickingUp = true;
                didDupe = false;
                return;
            }
            if (mc.player.experienceLevel == 0) {
                error("Out of xp ! disabling");
                toggle();
                return;
            }
            if (!mc.player.currentScreenHandler.getSlot(0).hasStack()) {
                if (!mc.player.currentScreenHandler.getSlot(30).hasStack()) {
                    return; //didn't pick up anything yet
                }
                if (!mc.player.currentScreenHandler.getSlot(30).getStack().isItemEqual(toDupe)) {
                    didDupe = true; //go for dropping again
                    return; //wrong item !
                }
                //put the shulker in if it's not there
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, 0, SlotActionType.QUICK_MOVE, mc.player);
                first = 4; //give it a bit
                return; //let's give it a bit of a break
            }
            if (first == 2) {
                first = -1;
                //not renamed ? let's do this
                //rename item
                String newName;
                String name = mc.player.currentScreenHandler.getSlot(0).getStack().getName().asString();
                if (!name.endsWith(" ")) {
                    newName = name + " "; //add a space
                } else {
                    newName = name.substring(0, name.length() - 1);
                }
                ((AnvilScreenHandler) mc.player.currentScreenHandler).setNewItemName(newName);
                mc.player.networkHandler.sendPacket(new RenameItemC2SPacket(newName));
                return;
            }
            if (first > 0) {
                first--;
                return;
            }
            if (((AnvilScreenHandler) mc.player.currentScreenHandler).getLevelCost() != 1) {
                return; //either you are playing around repairing stuff (not funny btw) or he's not ready
            }
            //item is there but is the inv full ?
            if (mc.player.getInventory().getEmptySlot() == -1) {
                //yep inv is full
                //we dupin now
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 0, SlotActionType.PICKUP, mc.player);
                //got shulker out !
                didDupe = true;
            }
        }
    }
}
