package com.winthier.minigames.enderball;

import com.winthier.minigames.MinigamesPlugin;
import com.winthier.minigames.event.player.PlayerLeaveEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class EventListener implements Listener {
    private final EnderBall game;

    public EventListener(EnderBall game) {
        this.game = game;
    }

    public void enable() {
        MinigamesPlugin.getEventManager().registerEvents(this, game);
    }

    /**
     * Dragon egg landing
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        final Entity entity = event.getEntity();
        final Block block = event.getBlock();
        final Ball ball = game.getBall(entity);
        if (ball == null) return;
        // 
        event.setCancelled(true);
        ball.toBlock();
        game.getConfiguration().get(Config.Key.SoundEffect.BALL_LAND).play(ball.getLocation());
    }

    /**
     * Player kicking ball on the ground
     */
    @SuppressWarnings("deprecation") // spawnFallingBlock(Location, Material, byte) is deprecated
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer().getItemInHand().getType() == Material.WRITTEN_BOOK) {
            event.setUseInteractedBlock(Event.Result.DENY);
        } else {
            event.setCancelled(true);
        }
        final Player player = event.getPlayer();
        switch (event.getAction()) {
        case RIGHT_CLICK_AIR:
        case RIGHT_CLICK_BLOCK:
            onRightClick(player);
        }
        final Block block = event.getClickedBlock();
        if (block == null) return;
        final Ball ball = game.getBall(block);
        if (ball == null) return;
        //
        Kick.Strength strength = player.isSprinting() ? Kick.Strength.LONG : Kick.Strength.SHORT;
        Kick.Height height = getKickHeight(event.getAction());
        if (game.getStadium().onKickBall(player, ball, strength, height)) {
            ball.toEntity();
            final Entity entity = ball.getEntity();
            Vector vec = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            vec.setY(0.0).normalize().setY(height.height).multiply(strength.strength);
            entity.setVelocity(vec);
            // Sound effect
            switch (strength) {
            case SHORT:
                game.getConfiguration().get(Config.Key.SoundEffect.BALL_KICK_SHORT).play(ball.getLocation());
                break;
            case LONG:
                game.getConfiguration().get(Config.Key.SoundEffect.BALL_KICK_LONG).play(ball.getLocation());
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        event.setCancelled(true);
        onRightClick(event.getPlayer());
    }

    private void onRightClick(Player player) {
        if (player.getInventory().getHeldItemSlot() == 8) {
            MinigamesPlugin.leavePlayer(player);
        }
    }

    private Kick.Height getKickHeight(Action action) {
        switch (action) {
        case LEFT_CLICK_AIR:
        case LEFT_CLICK_BLOCK:
            return Kick.Height.HIGH;
        case RIGHT_CLICK_AIR:
        case RIGHT_CLICK_BLOCK:
        default:
            return Kick.Height.LOW;
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        game.getStadium().onPlayerQuit(event.getPlayer());
        if (game.getOnlinePlayers().size() == 1) {
            game.cancel();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.setScoreboard(game.getScores().getDefaultScoreboard());
        if (game.getSoccerPlayer(player).isSpectator()) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            player.setWalkSpeed(0.3f);
            game.getSoccerPlayer(player).dress(player);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveEvent event) {
        if (game.getOnlinePlayers().size() == 1) {
            game.cancel();
        }
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        final SoccerPlayer sp = game.getSoccerPlayer(player);
        if (!sp.isPlaying()) return;
        if (event.getPreviousSlot() < 4 || event.getNewSlot() < 4) {
            sp.dress(player, event.getNewSlot());
        }
    }
}
