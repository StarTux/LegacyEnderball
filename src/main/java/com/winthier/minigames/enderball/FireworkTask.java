package com.winthier.minigames.enderball;

import com.winthier.minigames.MinigamesPlugin;
import java.util.Iterator;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class FireworkTask extends BukkitRunnable {
    private final EnderBall game;
    private final SoccerTeam team;
    private static final long TICK_DELAY = 10L;
    private Iterator<Location> north, south;

    public static void launch(EnderBall game, SoccerTeam team) {
        FireworkTask task = new FireworkTask(game, team);
        task.start();
    }

    private FireworkTask(EnderBall game, SoccerTeam team) {
        this.game = game;
        this.team = team;
    }

    private void start() {
        north = game.getConfiguration().get(Config.Key.LocationList.FIREWORKS_NORTH).iterator();
        south = game.getConfiguration().get(Config.Key.LocationList.FIREWORKS_SOUTH).iterator();
        game.storeTask(this);
        runTaskTimer(MinigamesPlugin.getInstance(), TICK_DELAY, TICK_DELAY);
    }

    @Override
    public void run() {
        boolean result = false;
        if (north.hasNext()) {
            result = true;
            launchFirework(north.next());
        }
        if (south.hasNext()) {
            result = true;
            launchFirework(south.next());
        }
        if (!result) game.cancelTask(this);
    }

    private void launchFirework(Location loc) {
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        for (int i = 0; i < 3; ++i) {
            meta.addEffect(FireworkEffect.builder()
                           .with(FireworkEffect.Type.BALL_LARGE)
                           .withColor(game.getConfiguration().getTeamFireworkColor(team))
                           .build());
        }
        meta.setPower(2);
        firework.setFireworkMeta(meta);
    }
}
