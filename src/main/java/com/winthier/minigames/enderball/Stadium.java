package com.winthier.minigames.enderball;

import com.winthier.minigames.MinigamesPlugin;
import com.winthier.minigames.player.PlayerInfo;
import com.winthier.minigames.util.Msg;
import com.winthier.minigames.util.SoundEffect;
import com.winthier.minigames.util.Title;
import java.util.List;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Stadium {
    public final EnderBall game;
    private final BukkitRunnable tickTask = new BukkitRunnable() { @Override public void run() { tick(); } };
    // State
    private State state = State.INIT;
    private SoccerTeam kickoffTeam = null;
    private SoccerTeam lastGoalTeam = null;
    private SoccerPlayer lastGoalPlayer = null;
    private Ball ball;
    private long stateTicks = 0;
    private long playTime = 0;
    private boolean kicked = false;

    public Stadium(EnderBall game) {
        this.game = game;
    }

    public void enable() {
        tickTask.runTaskTimer(MinigamesPlugin.getInstance(), 1L, 1L);
        this.ball = new Ball(game);
        game.addBall(ball);
    }

    public void disable() {
        tickTask.cancel();
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Util
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public boolean isInsideField(Entity e) {
        return isInsideField(e.getLocation());
    }

    public boolean isInsideField(Location loc) {
        if (game.getConfiguration().get(Config.Key.Rectangle.PLAYING_FIELD).contains(loc)) return true;
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_NORTH).contains(loc)) return true;
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_SOUTH).contains(loc)) return true;
        return false;
    }

    public boolean isBallInField() {
        if (game.getConfiguration().get(Config.Key.Rectangle.PLAYING_FIELD).contains(ball)) return true;
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_NORTH).contains(ball)) return true;
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_SOUTH).contains(ball)) return true;
        return false;
    }

    public SoccerTeam getGoalWithBall() {
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_NORTH).contains(ball)) return SoccerTeam.NORTH;
        if (game.getConfiguration().get(Config.Key.Cuboid.GOAL_SOUTH).contains(ball)) return SoccerTeam.SOUTH;
        return null;
    }

    public void updateBall() {
        ball.create();
        if (!isBallInField()) {
            if (game.getConfiguration().get(Config.Key.Rectangle.PLAYING_FIELD).clamAndBounce(ball)) {
                game.getConfiguration().get(Config.Key.SoundEffect.BALL_BOUNCE).play(ball.getLocation());
            }
        }
    }

    public void updatePlayers() {
        for (Player player : game.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        final SoccerPlayer sp = game.getSoccerPlayer(player);
        if (sp.getTeam().isPlaying()) {
            updateSoccerPlayer(player, sp);
        } else {
            updateSpectator(player, sp);
        }
    }

    private boolean checkBallCollision(Player player, Ball ball) {
        if (!ball.isEntity()) return false;
        Vector vec = ball.getEntity().getVelocity();
        if (vec.getY() > 0.0) return false;
        if (vec.getX() == 0.0 && vec.getZ() == 0.0) return false;
        final Location loc = player.getLocation();
        final Location bloc = ball.getLocation();
        if (Math.abs(bloc.getX() - loc.getX()) > 0.75) return false;
        if (Math.abs(bloc.getZ() - loc.getZ()) > 0.75) return false;
        //Msg.send(player, "%.02f %.02f", bloc.getY(), bloc.getY() - loc.getY());
        if (bloc.getY() < loc.getY()) return false;
        if (bloc.getY() - loc.getY() > 2.5) return false;
        vec.setX(0.0);
        vec.setZ(0.0);
        ball.getEntity().setVelocity(vec);
        return true;
    }

    public void updateSoccerPlayer(Player player, SoccerPlayer sp) {
        sp.updateFoodLevel(player);
        if (game.getConfiguration().get(Config.Key.Rectangle.PLAYER_ZONE).clam(player)) {
            Msg.send(player, "&cDon't leave the playing field");
        }
        if (checkBallCollision(player, ball)) {
            game.getConfiguration().get(Config.Key.SoundEffect.BALL_BLOCK).play(ball.getLocation());
        }
    }

    private void storeSafeLocations() {
        for (Player player : game.getOnlinePlayers()) {
            game.getSoccerPlayer(player).storeSafeLocation(player);
        }
    }

    public void updateSpectator(Player player, SoccerPlayer sp) {
        // TODO
    }

    public boolean canKick(Player player) {
        switch (this.state) {
        case SELECT_TEAM:
            if (!game.getSoccerPlayer(player).isReady() && game.getSoccerPlayer(player).isPlaying()) {
                game.getSoccerPlayer(player).setReady();
                Msg.send(player, "&aReady");
            }
            return false;
        case KICKOFF:
            return canKickOff(player);
        case INIT:
        case WAIT_FOR_PLAYERS:
        case START_GAME:
        case GOAL:
        case END:
            return false;
        case PLAY:
            return true;
        default:
            throw new IllegalArgumentException("canKick() forgot to consider state: " + this.state.name());
        }
    }

    public boolean canKickOff(Player player) {
        if (kickoffTeam == null) return true;
        return game.getSoccerPlayer(player).getTeam() == kickoffTeam;
    }

    /**
     * Can be null.
     */
    public void setKickoffTeam(SoccerTeam team) {
        if (team != null && !team.isPlaying()) throw new IllegalArgumentException("Kickoff team not playing: " + team.name());
        this.kickoffTeam = team;
    }

    public void randomizeKickoffTeam() {
        if (game.getRandom().nextBoolean()) {
            this.kickoffTeam = SoccerTeam.NORTH;
        } else {
            this.kickoffTeam = SoccerTeam.SOUTH;
        }
    }

    public boolean keepOut(Player player, Rectangle rectangle) {
        if (!rectangle.contains(player)) return false;
        Location loc = game.getSoccerPlayer(player).getSafeLocation();
        if (loc == null || rectangle.contains(loc)) {
            loc = game.getConfiguration().get(Config.Key.Location.KICKOFF);
        }
        player.teleport(loc);
        return true;
    }

    public boolean allowJoin() {
        switch (state) {
        case INIT:
        case WAIT_FOR_PLAYERS:
        case SELECT_TEAM:
            return true;
        default:
            return false;
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Tick
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void tick() {
        State nextState = this.state;
        updateBall();
        updatePlayers();
        long ticks = stateTicks++;
        switch (this.state) {
        case INIT:
            nextState = tickInit(ticks);
            break;
        case WAIT_FOR_PLAYERS:
            nextState = tickWaitForPlayers(ticks);
            break;
        case SELECT_TEAM:
            nextState = tickSelectTeam(ticks);
            break;
        case START_GAME:
            nextState = tickStartGame(ticks);
            break;
        case KICKOFF:
            nextState = tickKickoff(ticks);
            break;
        case PLAY:
            nextState = tickPlay(ticks);
            break;
        case GOAL:
            nextState = tickGoal(ticks);
            break;
        case END:
            nextState = tickEnd(ticks);
            break;
        }
        kicked = false;
        storeSafeLocations();
        setState(nextState);
    }

    private State tickInit(long ticks) {
        game.getConfiguration().getStadiumWorld().setTime(0L);
        game.getConfiguration().getStadiumWorld().setDifficulty(Difficulty.PEACEFUL);
        game.getConfiguration().getStadiumWorld().setPVP(false);
        return State.WAIT_FOR_PLAYERS;
        //return State.SELECT_TEAM;
    }

    private State tickWaitForPlayers(long ticks) {
        if (game.countPlayers() >= 2) return State.SELECT_TEAM;
        if (ticks % 200L == 0L) {
            game.announce("&3&lEnderball&r Waiting for more players...");
        }
        if (ticks > 10L * 60L * 20L) game.cancel();
        return State.WAIT_FOR_PLAYERS;
    }

    private State tickSelectTeam(long ticks) {
        final long timeBank = game.getConfiguration().get(Config.Key.Time.SELECT_TEAM) * 20L;
        final boolean timeUp = ticks > timeBank;
        final int teamDiff = Math.abs(game.countTeamPlayers(SoccerTeam.NORTH) - game.countTeamPlayers(SoccerTeam.SOUTH));
        final boolean teamsEven = teamDiff <= 1;
        final boolean allPlayersReady = game.countPlayers() == game.countReadyPlayers();
        if (ticks == 0) {
            game.getScores().setDefaultScoreboard(Scores.Type.TEAM_PLAYERS);
        } else if ((timeUp || allPlayersReady) && teamsEven) {
            return State.START_GAME;
        }
        if (ticks % 20L == 0L) {
            double mid = game.getConfiguration().get(Config.Key.Location.KICKOFF).getZ();
            for (Player player : game.getOnlinePlayers()) {
                Location loc = player.getLocation();
                SoccerTeam team = loc.getZ() < mid ? SoccerTeam.NORTH : SoccerTeam.SOUTH;
                SoccerPlayer sp = game.getSoccerPlayer(player);
                if (sp.getTeam() != team) {
                    game.getSoccerPlayer(player).setTeam(team);
                    game.getSoccerPlayer(player).dress(player);
                    Msg.send(player, "&3&lEnderball&r Team %s", game.getConfiguration().getTeamDisplayName(team));
                    player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(81, 1));
                    game.getConfiguration().get(Config.Key.SoundEffect.SELECT_TEAM).play(player, player.getEyeLocation());
                    game.getScores().updateTeamPlayers();
                }
            }
            long timeLeft = timeBank - ticks;
            game.getScores().setTimer(Math.max(0L, timeLeft / 20L));
            if (ticks % 200L == 0L) {
                if (timeUp) {
                    game.announce("&3&lEnderball&r Time's up. Make sure that both teams are somewhat even.");
                } else {
                    StringBuilder sb = new StringBuilder("&3&lEnderball&r Currently picking:&o");
                    for (Player player : game.getOnlinePlayers()) {
                        if (!game.getSoccerPlayer(player).isReady()) {
                            Msg.send(player, "&3&lEnderball&r Walk to side of team %s&r or %s&r, then hit the ball.",
                                     game.getConfiguration().getTeamDisplayName(SoccerTeam.NORTH),
                                     game.getConfiguration().getTeamDisplayName(SoccerTeam.SOUTH));
                            sb.append(" ").append(player.getName());
                        }
                    }
                    game.announce(sb.toString());
                    game.announceTeams();
                }
            }
        }
        return State.SELECT_TEAM;
    }

    private State tickStartGame(long ticks) {
        game.announce("&3&lEnderball&r The game is starting");
        game.getScores().setDefaultScoreboard(Scores.Type.TEAM_GOALS);
        game.teleportPlayersToStartingLocations();
        randomizeKickoffTeam();
        game.announceTeams();
        return State.KICKOFF;
    }

    public State tickKickoff(long ticks) {
        if (ticks == 0) {
            game.getScores().setTitle("Kickoff");
            if (kickoffTeam != null) {
                game.announce("&3&lEnderball&r Kickoff for %s", game.getConfiguration().getTeamDisplayName(kickoffTeam));
            } else {
                game.announce("&3&lEnderball&r Kickoff");
            }
        }
        if (kicked) {
            setKickoffTeam(null);
            return State.PLAY;
        }
        if (ticks % 20L == 0L) {
            final long timeBank = game.getConfiguration().get(Config.Key.Time.KICKOFF);
            final long timeLeft = Math.max(0L, timeBank - (ticks / 20L));
            if (timeLeft == 0L) {
                // Time's up; avoid kickoff abuse
                game.announce("&3&lEnderball&r Kickoff expired");
                setKickoffTeam(null);
                return State.PLAY;
            }
            game.getScores().setTimer(timeLeft);
        }
        Rectangle penalty = game.getConfiguration().get(kickoffTeam == SoccerTeam.NORTH ? Config.Key.Rectangle.PENALTY_NORTH : Config.Key.Rectangle.PENALTY_SOUTH);
        for (Player player : game.getOnlinePlayers()) {
            SoccerPlayer sp = game.getSoccerPlayer(player);
            if (sp.isPlaying() && sp.getTeam() != kickoffTeam) {
                if (keepOut(player, penalty)) {
                    Msg.send(player, "&cStay out of the penalty area during kickoff");
                }
            }
        }
        return State.KICKOFF;
    }

    public State tickPlay(long ticks) {
        if (ticks == 0) {
            int players = 0;
            for (Player player: game.getOnlinePlayers()) {
                SoccerPlayer sp = game.getSoccerPlayer(player);
                if (sp.isPlaying()) players += 1;
            }
            if (players <= 1) game.cancel();
        }
        playTime += 1;
        if (playTime % 20L == 0L) {
            final long timeBank = game.getConfiguration().get(Config.Key.Time.GAME) * 20L * 60L;
            final long timeLeft = Math.max(0L, timeBank - playTime);
            game.getScores().setTimer(timeLeft / 20L);
            if (timeLeft == 0L) {
                return State.END;
            }
        }
        SoccerTeam goalWithBall = getGoalWithBall();
        if (goalWithBall != null) {
            SoccerPlayer lastKicker = ball.getLastKicker();
            if (lastKicker != null) {
                game.getScores().addScore(Scores.Type.PLAYER_GOALS, lastKicker.getOfflinePlayer(), 1);
            } else {
                game.getLogger().warning("Ball has no last kicker!");
            }
            game.getScores().addScore(Scores.Type.TEAM_GOALS, goalWithBall.other(), 1);
            this.lastGoalTeam = goalWithBall.other();
            this.lastGoalPlayer = lastKicker;
            return state.GOAL;
        }
        return State.PLAY;
    }

    public State tickGoal(long ticks) {
        if (ticks == 0) {
            String playerName = this.lastGoalPlayer != null ? this.lastGoalPlayer.getName() : "Someone";
            String playerPrefix = this.lastGoalPlayer != null ? game.getConfiguration().getTeamPrefix(this.lastGoalPlayer.getTeam()) : "";
            game.announce("&3&lEnderball&r Goal for %s&r! Scorer: %s%s", game.getConfiguration().getTeamDisplayName(lastGoalTeam), playerPrefix, playerName);
            boolean selfGoal = (lastGoalPlayer.getTeam() != lastGoalTeam);
            if (selfGoal) {
                game.announceTitle("&8Own Goal", "&7Scored by " + playerName);
            } else {
                game.announceTitle(playerPrefix + "Goal", playerPrefix + "Scored by " + playerName);
            }
            FireworkTask.launch(game, lastGoalTeam);
            for (Player player : game.getOnlinePlayers()) {
                game.getConfiguration().get(Config.Key.SoundEffect.GOAL).play(player, ball.getLocation());
            }
            if (lastGoalPlayer != null && lastGoalTeam == lastGoalPlayer.getTeam()) {
                int thisTeam = 0;
                int otherTeam = 0;
                for (Player player : game.getOnlinePlayers()) {
                    SoccerPlayer sp = game.getSoccerPlayer(player);
                    if (sp.isPlaying()) {
                        if (sp.getTeam() == lastGoalTeam) {
                            thisTeam += 1;
                        } else {
                            otherTeam += 1;
                        }
                    }
                }
                if (otherTeam > 0 && otherTeam >= thisTeam) {
                    lastGoalPlayer.setGoals(lastGoalPlayer.getGoals() + 1);
                }
            }
        } else if (ticks > 100) {
            setKickoffTeam(lastGoalTeam.other());
            Location loc = null;
            if (lastGoalTeam == SoccerTeam.NORTH) {
                loc = game.getConfiguration().get(Config.Key.Location.KICKOFF_SOUTH);
            } else {
                loc = game.getConfiguration().get(Config.Key.Location.KICKOFF_NORTH);
            }
            ball.put(loc);
            return State.KICKOFF;
        }
        for (int i = 0; i < 4; ++i) { // Applause
            final List<SoundEffect> applause = game.getConfiguration().get(Config.Key.SoundEffectList.APPLAUSE);
            final SoundEffect sound = applause.get(game.getRandom().nextInt(applause.size()));
            final double x = game.getRandom().nextDouble() - game.getRandom().nextDouble();
            final double z = game.getRandom().nextDouble() - game.getRandom().nextDouble();
            final Vector vector = new Vector(x, 0.0, z).normalize().multiply(12.0);
            for (Player player : game.getOnlinePlayers()) {
                sound.play(player, player.getEyeLocation().add(vector));
            }
        }
        return State.GOAL;
    }

    public State tickEnd(long ticks) {
        if (ticks == 0) {
            game.announceTeams();
            SoccerTeam winner = game.getWinner();
            String name = winner != null ? game.getConfiguration().getTeamDisplayName(winner) : Msg.format("&2DRAW");
            game.announce("&3&lEnderball&r Game Over. The winner is: %s", name);
            String prefix = winner != null ? game.getConfiguration().getTeamPrefix(winner) : Msg.format("&8");
            game.announceTitle(prefix + "Game Over",
                               winner != null ?
                               prefix + game.getConfiguration().getTeamDisplayName(winner) + " wins!" :
                               "&7It's a draw!");
            for (Player player : game.getOnlinePlayers()) {
                player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERDRAGON_DEATH, 1.0f, 1.0f);
            }
            if (winner != null) FireworkTask.launch(game, winner);
            for (PlayerInfo info: game.getPlayers()) {
                game.getSoccerPlayer(info.getUuid()).reward();
            }
        } else if (ticks > 60L * 20L) {
            game.cancel();
        }
        return State.END;
    }

    private void resetState() {
        game.getScores().setTitle(null);
        stateTicks = 0L;
    }

    public void setState(State state) {
        if (state != this.state) {
            this.state = state;
            resetState();
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Events
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public boolean onKickBall(Player player, Ball ball, Kick.Strength strength, Kick.Height height) {
        boolean result = canKick(player);
        if (result) {
            kicked = true;
            game.getScores().addScore(Scores.Type.PLAYER_KICKS, player, 1);
            ball.setLastKicker(player);
        }
        return result;
    }

    public void onPlayerQuit(Player player) {
        switch (this.state) {
        case INIT:
        case WAIT_FOR_PLAYERS:
        case SELECT_TEAM:
            MinigamesPlugin.leavePlayer(player);
        }
    }

    public static enum State {
        INIT,
        WAIT_FOR_PLAYERS,
        SELECT_TEAM,
        START_GAME,
        KICKOFF,
        PLAY,
        GOAL,
        END,
        ;
    }
}
