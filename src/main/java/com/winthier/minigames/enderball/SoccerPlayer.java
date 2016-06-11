package com.winthier.minigames.enderball;

import com.winthier.reward.RewardBuilder;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer; 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@Data
public class SoccerPlayer {
    final EnderBall game;
    final UUID uuid;
    String name = null;
    SoccerTeam team = SoccerTeam.SPECTATOR;
    double foodLevel = 20.0;
    int noSprintTicks = 0;
    boolean ready = false;
    Location safeLocation;
    int goals = 0;
    boolean rewarded = false;
    boolean spectator = false;

    public SoccerPlayer(EnderBall game, UUID uuid) {
        this.game = game;
        this.uuid = uuid;
    }

    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getServer().getOfflinePlayer(uuid);
    }

    public SoccerTeam getTeam() { return team; }
    public void setTeam(SoccerTeam team) {
        this.team = team;
        game.getScores().setTeam(getOfflinePlayer(), team);
        game.getScores().updateTeamPlayers();
    }
    public boolean isPlaying() { return team.isPlaying(); }

    public boolean isReady() { return ready; }
    public void setReady() { if (team.isPlaying()) this.ready = true; }

    public boolean hasName() { return name != null; }
    public String getName() { return name != null ? name : "N/A"; }
    public void setName(String name) { this.name = name; }

    public Location getSafeLocation() { return safeLocation; }
    public void storeSafeLocation(Player player) { this.safeLocation = player.getLocation(); }

    public double getFoodLevel() { return foodLevel; }
    public void setFoodLevel(double foodLevel) { this.foodLevel = foodLevel; }
    public void changeFoodLevel(double increment) {
        this.foodLevel += increment;
        if (foodLevel < 0.0) foodLevel = 0.0;
        if (foodLevel > 20.0) foodLevel = 20.0;
    }

    public void dress(Player player) {
        dress(player, player.getInventory().getHeldItemSlot());
    }

    public void dress(Player player, int heldItemSlot) {
        player.getInventory().clear();
        player.getInventory().setItem(8, game.getConfiguration().get(Config.Key.Item.LEAVE));
        player.getInventory().setItem(7, game.getConfiguration().get(Config.Key.Item.MANUAL));
        if (!team.isPlaying()) return;
        player.getInventory().setArmorContents(game.getConfiguration().getUniform(team));
        for (int i = 0; i < 4; ++i) {
            int slot = 3 - i;
            if (heldItemSlot != slot) {
                player.getInventory().setItem(slot, game.getConfiguration().getUniform(team)[i]);
            }
        }
    }

    public void updateFoodLevel(Player player) {
        if (player.isSprinting()) {
            noSprintTicks = 60; // magic number; 3 seconds
            changeFoodLevel(-0.1);
        } else {
            noSprintTicks -= 1;
            if (noSprintTicks < 0) noSprintTicks = 0;
            if (noSprintTicks == 0) {
                changeFoodLevel(0.1);
            }
        }
        int foodLevel = (int)Math.round(this.foodLevel);
        if (player.getFoodLevel() != foodLevel) {
            player.setFoodLevel(foodLevel);
            player.setSaturation(20.0f);
        }
    }

    void reward() {
        if (isPlaying() && !rewarded) {
            rewarded = true;
            RewardBuilder reward = RewardBuilder.create().uuid(uuid).name(name);
            boolean winner = team == game.getWinner();
            reward.comment(String.format("Game of Enderball %s with %d goals scored.", (winner ? "won" : "played"), goals));
            ConfigurationSection config = game.getConfigFile("rewards");
            if (winner) reward.config(config.getConfigurationSection("win"));
            for (int i = 0; i < goals; ++i) reward.config(config.getConfigurationSection("goal"));
            reward.store();
        }
    }
}
