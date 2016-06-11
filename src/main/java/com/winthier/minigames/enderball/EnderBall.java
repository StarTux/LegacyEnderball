package com.winthier.minigames.enderball;

import com.winthier.minigames.MinigamesPlugin;
import com.winthier.minigames.game.Game;
import com.winthier.minigames.player.PlayerInfo;
import com.winthier.minigames.util.BukkitFuture;
import com.winthier.minigames.util.Enums;
import com.winthier.minigames.util.Msg;
import com.winthier.minigames.util.Players;
import com.winthier.minigames.util.WorldLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * = TODO =
 * +Recognize Teams
 * +Control Ball Velocity
 * +  Running > Farther
 * -Vote for team color
 */

public class EnderBall extends Game {
    private final String STADIUM_WORLD_NAME = "EnderBall/Default";
    private final EventListener eventListener = new EventListener(this);
    private final Stadium stadium = new Stadium(this);
    private final Scores scores = new Scores(this);
    // State
    private final List<Ball> balls = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());
    private final Set<BukkitRunnable> tasks = new HashSet<>();
    // Config
    private final Config config = new Config(this);

    public EnderBall() {
    }

    @Override
    public void onEnable() {
        WorldLoader.loadWorlds(this, new BukkitFuture<WorldLoader>() { @Override public void run() { worldsLoaded(get()); } }, STADIUM_WORLD_NAME);
    }

    @Override
    public void onDisable() {
        stadium.disable();
        for (BukkitRunnable task : tasks) {
            try {
                task.cancel();
            } catch (IllegalStateException ise) {}
        }
        tasks.clear();
    }

    private void worldsLoaded(WorldLoader loader) {
        config.setStadiumWorld(loader.getWorld(STADIUM_WORLD_NAME));
        config.load();
        eventListener.enable();
        scores.enable();
        stadium.enable();
        ready();
    }

    @Override
    public boolean onCommand(Player player, String command, String[] args) {
        if (false) {
        } else if ("EndGame".equalsIgnoreCase(command) && args.length == 0) {
            if (!player.isOp()) return false;
            Msg.send(player, "&eEnding the game");
            cancel();
        } else if ("Ball".equalsIgnoreCase(command) && args.length == 0) {
            if (!player.isOp()) return false;
            createBall(player.getLocation().getBlock());
            Msg.send(player, "&eBall spawned");
        } else if ("Team".equalsIgnoreCase(command) && args.length == 1) {
            if (!player.isOp()) return false;
            SoccerTeam team = Enums.parseEnum(SoccerTeam.class, args[0]);
            if (team == null) throw new CommandException("Unknown team: " + args[0]);
            getSoccerPlayer(player).setTeam(team);
            getSoccerPlayer(player).dress(player);
            Msg.send(player, "&eSet team to %s", team.name());
        } else if ("Stats".equalsIgnoreCase(command) && args.length == 1) {
            Scores.Type type = Enums.parseEnum(Scores.Type.class, args[0]);
            if (type == null) throw new CommandException("Unknown stats: " + args[0]);
            player.setScoreboard(scores.getScoreboard(type));
        } else if ("State".equalsIgnoreCase(command) && args.length == 1) {
            if (!player.isOp()) return false;
            Stadium.State state = Enums.parseEnum(Stadium.State.class, args[0]);
            if (state == null) throw new CommandException("Unknown state: " + args[0]);
            stadium.setState(state);
        } else if ("Test".equalsIgnoreCase(command) && args.length == 0) {
            if (!player.isOp()) return false;
            Msg.send(player, "&eField: %b, North: %b, South: %b",
                     config.get(Config.Key.Rectangle.PLAYING_FIELD).contains(player),
                     config.get(Config.Key.Cuboid.GOAL_NORTH).contains(player),
                     config.get(Config.Key.Cuboid.GOAL_SOUTH).contains(player));
            player.setScoreboard(scores.getDefaultScoreboard());
        } else {
            return false;
        }
        return true;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Events
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    @Override
    public void onPlayerReady(final Player player) {
        Players.reset(player);
        SoccerPlayer sp = getSoccerPlayer(player);
        sp.setName(player.getName());
        if (sp.isSpectator()) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            player.setScoreboard(scores.getDefaultScoreboard());
            player.setWalkSpeed(0.3f);
            sp.dress(player);
        }
    }
    
    @Override
    public boolean joinPlayers(List<UUID> uuids) {
        if (stadium.allowJoin() && super.joinPlayers(uuids)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean joinSpectators(List<UUID> uuids) {
        if (super.joinSpectators(uuids)) {
            for (UUID uuid: uuids) {
                getSoccerPlayer(uuid).setSpectator(true);
            }
            return true;
        } else {
            return false;
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Getters and Setters
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public Config getConfiguration() {
        return config;
    }

    public Stadium getStadium() {
        return stadium;
    }

    public Scores getScores() {
        return scores;
    }

    public int countPlayers() {
        return getPlayerUuids().size();
    }

    public int countTeamPlayers(SoccerTeam team) {
        int count = 0;
        for (UUID uuid : getPlayerUuids()) {
            if (getSoccerPlayer(uuid).getTeam() == team) count += 1;
        }
        return count;
    }

    public int countPlayingPlayers() {
        int count = 0;
        for (UUID uuid : getPlayerUuids()) {
            if (getSoccerPlayer(uuid).getTeam().isPlaying()) count += 1;
        }
        return count;
    }

    public int countReadyPlayers() {
        int count = 0;
        for (UUID uuid : getPlayerUuids()) {
            if (getSoccerPlayer(uuid).isReady()) count += 1;
        }
        return count;
    }

    public Ball getBall(Block block) {
        for (Ball ball : balls) {
            if (ball.isBlock()) {
                if (block.equals(ball.getBlock())) {
                    return ball;
                }
            }
        }
        return null;
    }

    public Ball getBall(Entity e) {
        for (Ball ball : balls) {
            if (ball.isEntity()) {
                if (e.equals(ball.getEntity())) {
                    return ball;
                }
            }
        }
        return null;
    }

    public SoccerPlayer getSoccerPlayer(UUID uuid) {
        return getPlayer(uuid).<SoccerPlayer>getCustomData(SoccerPlayer.class);
    }

    public SoccerPlayer getSoccerPlayer(Player player) {
        return getPlayer(player.getUniqueId()).<SoccerPlayer>getCustomData(SoccerPlayer.class);
    }

    public Random getRandom() {
        return this.random;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Utility
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void unsetBall(Ball ball) {
        ball.unset();
    }

    public void setBall(Block block, Ball ball) {
        unsetBall(ball);
        ball.setBlock(block);
    }

    public void setBall(Entity entity, Ball ball) {
        unsetBall(ball);
        ball.setEntity(entity);
    }

    public Ball createBall(Block block) {
        Ball ball = new Ball(this);
        ball.setBlock(block);
        ball.create();
        balls.add(ball);
        return ball;
    }

    public void addBall(Ball ball) {
        balls.add(ball);
    }

    public SoccerTeam getWinner() {
        final int a = scores.getScore(Scores.Type.TEAM_GOALS, SoccerTeam.NORTH);
        final int b = scores.getScore(Scores.Type.TEAM_GOALS, SoccerTeam.SOUTH);
        if (a > b) return SoccerTeam.NORTH;
        if (a < b) return SoccerTeam.SOUTH;
        return null;
    }

    public void storeTask(BukkitRunnable task) {
        this.tasks.add(task);
    }

    public void cancelTask(BukkitRunnable task) {
        task.cancel();
        tasks.remove(task);
    }

    public void teleportPlayersToStartingLocations() {
        Iterator<Location> teamA = null, teamB = null;
        for (Player player : getOnlinePlayers()) {
            SoccerPlayer sp = getSoccerPlayer(player);
            Location loc = null;
            switch (sp.getTeam()) {
            case SPECTATOR:
                MinigamesPlugin.leavePlayer(player);
                continue;
            case NORTH:
                if (teamA == null || !teamA.hasNext()) {
                    teamA = getConfiguration().get(Config.Key.LocationList.PLAYERS_NORTH).iterator();
                }
                loc = teamA.next();
                break;
            case SOUTH:
                if (teamB == null || !teamB.hasNext()) {
                    teamB = getConfiguration().get(Config.Key.LocationList.PLAYERS_SOUTH).iterator();
                }
                loc = teamB.next();
                break;
            }
            player.teleport(loc);
        }
    }

    @Override
    public Location getSpawnLocation(Player player)
    {
        return config.getStadiumWorld().getSpawnLocation();
    }

    public void announceTeams() {
        announce("&c&lTeam Red");
        for (PlayerInfo info: getPlayers()) {
            if (getSoccerPlayer(info.getUuid()).getTeam() == SoccerTeam.NORTH) {
                announce("&c %s", getSoccerPlayer(info.getUuid()).getName());
            }
        }
        announce("&9&lTeam Blue");
        for (PlayerInfo info: getPlayers()) {
            if (getSoccerPlayer(info.getUuid()).getTeam() == SoccerTeam.SOUTH) {
                announce("&9 %s", getSoccerPlayer(info.getUuid()).getName());
            }
        }
    }
}
