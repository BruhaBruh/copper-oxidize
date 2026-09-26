package ru.bruhabruh.oxidize.listener;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.List;

public final class OxidizeListener implements Listener {

    private static final List<List<Material>> COPPER_BLOCKS = List.of(
            List.of(
                    Material.COPPER_BLOCK,
                    Material.EXPOSED_COPPER,
                    Material.WEATHERED_COPPER,
                    Material.OXIDIZED_COPPER
            ),
            List.of(
                    Material.CHISELED_COPPER,
                    Material.EXPOSED_CHISELED_COPPER,
                    Material.WEATHERED_CHISELED_COPPER,
                    Material.OXIDIZED_CHISELED_COPPER
            ),
            List.of(
                    Material.COPPER_GRATE,
                    Material.EXPOSED_COPPER_GRATE,
                    Material.WEATHERED_COPPER_GRATE,
                    Material.OXIDIZED_COPPER_GRATE
            ),
            List.of(
                    Material.CUT_COPPER,
                    Material.EXPOSED_CUT_COPPER,
                    Material.WEATHERED_CUT_COPPER,
                    Material.OXIDIZED_CUT_COPPER
            ),
            List.of(
                    Material.CUT_COPPER_STAIRS,
                    Material.EXPOSED_CUT_COPPER_STAIRS,
                    Material.WEATHERED_CUT_COPPER_STAIRS,
                    Material.OXIDIZED_CUT_COPPER_STAIRS
            ),
            List.of(
                    Material.CUT_COPPER_SLAB,
                    Material.EXPOSED_CUT_COPPER_SLAB,
                    Material.WEATHERED_CUT_COPPER_SLAB,
                    Material.OXIDIZED_CUT_COPPER_SLAB
            ),
            List.of(
                    Material.COPPER_BARS,
                    Material.EXPOSED_COPPER_BARS,
                    Material.WEATHERED_COPPER_BARS,
                    Material.OXIDIZED_COPPER_BARS
            ),
            List.of(
                    Material.COPPER_DOOR,
                    Material.EXPOSED_COPPER_DOOR,
                    Material.WEATHERED_COPPER_DOOR,
                    Material.OXIDIZED_COPPER_DOOR
            ),
            List.of(
                    Material.COPPER_TRAPDOOR,
                    Material.EXPOSED_COPPER_TRAPDOOR,
                    Material.WEATHERED_COPPER_TRAPDOOR,
                    Material.OXIDIZED_COPPER_TRAPDOOR
            ),
            List.of(
                    Material.COPPER_BULB,
                    Material.EXPOSED_COPPER_BULB,
                    Material.WEATHERED_COPPER_BULB,
                    Material.OXIDIZED_COPPER_BULB
            ),
            List.of(
                    Material.COPPER_CHAIN,
                    Material.EXPOSED_COPPER_CHAIN,
                    Material.WEATHERED_COPPER_CHAIN,
                    Material.OXIDIZED_COPPER_CHAIN
            ),
            List.of(
                    Material.COPPER_CHAIN,
                    Material.EXPOSED_COPPER_CHAIN,
                    Material.WEATHERED_COPPER_CHAIN,
                    Material.OXIDIZED_COPPER_CHAIN
            )
    );

    @EventHandler(ignoreCancelled = true)
    public void onPlayerOxidizeCopper(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        ItemStack item = event.getItem();
        if (!isWaterBottle(item)) return;

        Material nextStage = findNextStage(block);
        if (nextStage == null) return;

        updateToNextStage(block, nextStage);

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            int slot = getPlayerHandSlot(player, hand);
            consumeOneWaterBottle(player.getInventory(), slot, player.getLocation());
        }

        playSound(player, Key.key("entity.copper_golem_become_statue"));
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispenserOxidizeCopper(BlockDispenseEvent event) {
        if (!(event.getBlock().getState() instanceof Dispenser dispenser)) return;

        ItemStack dispensedItem = event.getItem();
        if (!isWaterBottle(dispensedItem)) return;

        BlockFace facing = getFacing(event.getBlock());
        if (facing == null) return;

        Block target = event.getBlock().getRelative(facing);
        Material nextStage = findNextStage(target);
        if (nextStage == null) return;

        Inventory inventory = dispenser.getInventory();
        int slot = findWaterBottleSlot(inventory);
        if (slot == -1) return;

        event.setCancelled(true);

        updateToNextStage(target, nextStage);
        consumeOneWaterBottle(inventory, slot, dispenser.getBlock().getLocation());
        playDispenseSound(event.getBlock(), Key.key("entity.copper_golem_become_statue"));
    }

    private static void consumeOneWaterBottle(Inventory inventory, int slot, Location dropLocation) {
        ItemStack current = inventory.getItem(slot);
        if (current == null) return;

        int newAmount = current.getAmount() - 1;

        if (newAmount <= 0) {
            inventory.setItem(slot, new ItemStack(Material.GLASS_BOTTLE));
            return;
        }

        current.setAmount(newAmount);
        inventory.setItem(slot, current);

        var leftover = inventory.addItem(new ItemStack(Material.GLASS_BOTTLE)).values();
        if (!leftover.isEmpty() && dropLocation != null && dropLocation.getWorld() != null) {
            leftover.forEach(item -> dropLocation.getWorld().dropItemNaturally(dropLocation, item));
        }
    }

    private static int getPlayerHandSlot(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND ? 40 : player.getInventory().getHeldItemSlot();
    }

    private static boolean isWaterBottle(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        var potionContents = item.getData(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) return false;
        PotionType potion = potionContents.potion();
        return potion == PotionType.WATER;
    }

    private static Material findNextStage(Block block) {
        List<Material> stages = COPPER_BLOCKS.stream()
                .filter(list -> list.contains(block.getType()))
                .findFirst()
                .orElse(null);
        if (stages == null) return null;

        int index = stages.indexOf(block.getType());
        if (index == -1 || index + 1 == stages.size()) return null;

        return stages.get(index + 1);
    }

    private static void updateToNextStage(Block block, Material nextStage) {
        String data = nextStage.key().asString()
                + block.getBlockData().getAsString().replaceFirst("^" + block.getType().key().asString(), "");
        block.setBlockData(Bukkit.createBlockData(data), true);
    }

    private static void playSound(Player player, Key soundKey) {
        player.playSound(Sound.sound(soundKey, Sound.Source.PLAYER, 1f, 1f));
    }

    private static void playDispenseSound(Block dispenserBlock, Key soundKey) {
        dispenserBlock.getWorld().playSound(
                Sound.sound(soundKey, Sound.Source.BLOCK, 1f, 1f),
                dispenserBlock.getX() + 0.5,
                dispenserBlock.getY() + 0.5,
                dispenserBlock.getZ() + 0.5
        );
    }

    private static BlockFace getFacing(Block dispenserBlock) {
        if (!(dispenserBlock.getBlockData() instanceof org.bukkit.block.data.type.Dispenser dispenserData)) {
            return null;
        }
        return dispenserData.getFacing();
    }

    private static int findWaterBottleSlot(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isWaterBottle(contents[i])) {
                return i;
            }
        }
        return -1;
    }
}