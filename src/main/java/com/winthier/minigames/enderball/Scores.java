package com.winthier.minigames.enderball;

import com.winthier.minigames.util.Msg;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Scores {
    private final EnderBall game;
    private final Map<Type, Scoreboard> scoreboards = new EnumMap<>(Type.class);
    private final String SIDEBAR = "sidebar";
    // private final String BELOW_NAME = "belowName";
    // private final String PLAYER_LIST = "playerList";
    private final String DUMMY = "dummy";
    private String title = null;
    private long timer = 0L;
    private Type defaultScoreboard = Type.TEAM_GOALS;

    public Scores(EnderBall game) {
        this.game = game;
    }

    public void enable() {
        for (Type type : Type.values()) {
            Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
            scoreboards.put(type, scoreboard);
            Objective sidebarObjective = scoreboard.registerNewObjective(SIDEBAR, DUMMY);
            sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            // Objective belowNameObjective = scoreboard.registerNewObjective(BELOW_NAME, DUMMY);
            // belowNameObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            // Objective playerListObjective = scoreboard.registerNewObjective(PLAYER_LIST, DUMMY);
            // playerListObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            // Team Colors
            //if (type.isPlayerScore()) {
            for (SoccerTeam team : SoccerTeam.both()) {
                Team scoreTeam = scoreboard.registerNewTeam(team.name());
                scoreTeam.setPrefix(game.getConfiguration().getTeamPrefix(team));
            }
                //}
            if (!type.isPlayerScore()) {
                for (SoccerTeam team : SoccerTeam.both()) {
                    sidebarObjective.getScore(game.getConfiguration().getTeamDisplayName(team)).setScore(0);
                }
            }
            // Below name
        }
        updateDisplayName();
    }

    public Scoreboard getDefaultScoreboard() { return scoreboards.get(defaultScoreboard); }
    public void setDefaultScoreboard(Type type) {
        this.defaultScoreboard = type;
        for (Player player : game.getOnlinePlayers()) player.setScoreboard(scoreboards.get(type));
    }

    public void updateTeamPlayers() {
        for (SoccerTeam st : SoccerTeam.both()) {
            setScore(Scores.Type.TEAM_PLAYERS, st, game.countTeamPlayers(st));
        }
    }

    private void updateDisplayName() {
        for (Type type : Type.values()) {
            Scoreboard scoreboard = scoreboards.get(type);
            Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
            String name = this.title != null ? this.title : type.displayName;
            long minutes = timer / 60;
            long seconds = timer % 60;
            String displayName = Msg.format("&a%s &8|&7 Time &r%02d:%02d", name, minutes, seconds);
            objective.setDisplayName(displayName);
        }
    }

    public void setTitle(String title) {
        this.title = title;
        updateDisplayName();
    }
    public void setTimer(long timer) {
        this.timer = timer;
        updateDisplayName();
    }

    public void setTeam(OfflinePlayer player, SoccerTeam team) {
        if (!team.isPlaying()) throw new RuntimeException(team.name() + " is not playing");
        for (Type type : Type.values()) {
            //if (!type.isPlayerScore()) continue;
            Scoreboard scoreboard = scoreboards.get(type);
            scoreboard.getTeam(team.name()).addPlayer(player);
        }
    }

    public void addScore(Type type, OfflinePlayer player, int amount) {
        if (!type.isPlayerScore()) throw new RuntimeException(type.name() + " is not player score");
        Scoreboard scoreboard = scoreboards.get(type);
        Score score = scoreboard.getObjective(SIDEBAR).getScore(player.getName());
        score.setScore(score.getScore() + amount);
    }

    public void addScore(Type type, SoccerTeam team, int amount) {
        if (type.isPlayerScore()) throw new RuntimeException(type.name() + " is player score");
        if (!team.isPlaying()) throw new RuntimeException(team.name() + " is not playing");
        Scoreboard scoreboard = scoreboards.get(type);
        String name = game.getConfiguration().getTeamDisplayName(team);
        Score score = scoreboard.getObjective(SIDEBAR).getScore(name);
        score.setScore(score.getScore() + amount);
    }

    public void setScore(Type type, SoccerTeam team, int amount) {
        if (type.isPlayerScore()) throw new RuntimeException(type.name() + " is player score");
        if (!team.isPlaying()) throw new RuntimeException(team.name() + " is not playing");
        Scoreboard scoreboard = scoreboards.get(type);
        String name = game.getConfiguration().getTeamDisplayName(team);
        Score score = scoreboard.getObjective(SIDEBAR).getScore(name);
        score.setScore(amount);
    }

    public int getScore(Type type, SoccerTeam team) {
        if (type.isPlayerScore()) throw new RuntimeException(type.name() + " is player score");
        if (!team.isPlaying()) throw new RuntimeException(team.name() + " is not playing");
        Scoreboard scoreboard = scoreboards.get(type);
        String name = game.getConfiguration().getTeamDisplayName(team);
        Score score = scoreboard.getObjective(SIDEBAR).getScore(name);
        return score.getScore();
    }

    public Scoreboard getScoreboard(Type type) {
        return scoreboards.get(type);
    }

    public static enum Type {
        TEAM_PLAYERS("Select Team"),
        TEAM_GOALS("Goals"),
        PLAYER_GOALS("Goals"),
        PLAYER_KICKS("Kicks"),
        ;
        private final String displayName;
        Type(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        public boolean isPlayerScore() { return this != TEAM_GOALS && this != TEAM_PLAYERS; }
    }
}
