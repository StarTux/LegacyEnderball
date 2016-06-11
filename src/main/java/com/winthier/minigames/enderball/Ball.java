package com.winthier.minigames.enderball;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Ball {
    public static final Material MATERIAL = Material.DRAGON_EGG;
    private final EnderBall game;
    // State
    private State state = State.NONE;
    private Block block;
    private Entity entity;
    private SoccerPlayer lastKicker = null;

    public Ball(EnderBall game) {
        this.game = game;
    }

    public void unset() {
        this.state = State.NONE;
        this.block = null;
        this.entity = null;
    }

    public void setBlock(Block block) {
        this.block = block;
        this.entity = null;
        this.state = State.BLOCK;
    }

    public void setEntity(Entity entity) {
        this.block = null;
        this.entity = entity;
        this.state = State.ENTITY;
    }

    public State getState() {
        return state;
    }

    public boolean isBlock() {
        return state == State.BLOCK;
    }

    public boolean isEntity() {
        return state == State.ENTITY;
    }

    public Block getBlock() {
        return block;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean exists() {
        switch (state) {
        case NONE:
            return false;
        case BLOCK:
            return block.getType() == MATERIAL;
        case ENTITY:
            return entity.isValid();
        default:
            return false;
        }
    }

    /**
     * Remove the block or entity representing this ball. Do not
     * change the state of this object.
     */
    public void remove() {
        switch (state) {
        case NONE:
            break;
        case BLOCK:
            if (block.getType() == MATERIAL) {
                block.setType(Material.AIR);
            }
            break;
        case ENTITY:
            entity.remove();
            break;
        }
    }

    public void toBlock() {
        switch (state) {
        case NONE:
            this.block = game.getConfiguration().get(Config.Key.Location.KICKOFF).getBlock();
            this.state = State.BLOCK;
            break;
        case BLOCK:
            break;
        case ENTITY:
            this.block = entity.getLocation().getBlock();
            this.entity.remove();
            this.entity = null;
            this.state = State.BLOCK;
            break;
        }
        create();
    }

    @SuppressWarnings("deprecation")
    public void toEntity() {
        switch (state) {
        case NONE: {
            final Location loc = game.getConfiguration().get(Config.Key.Location.KICKOFF).clone().add(0.5, 0.0, 0.5);
            this.entity = loc.getWorld().spawnFallingBlock(loc, MATERIAL, (byte)0);
            this.state = State.ENTITY;
            break;
        }
        case BLOCK: {
            if (block.getType() == MATERIAL) {
                block.setType(Material.AIR);
            }
            final Location loc = block.getLocation().add(0.5, 0.0, 0.5);
            this.entity = block.getWorld().spawnFallingBlock(loc, MATERIAL, (byte)0);
            this.block = null;
            this.state = State.ENTITY;
            break;
        }
        case ENTITY:
            break;
        }
    }

    public void create() {
        switch (state) {
        case NONE:
            this.block = game.getConfiguration().get(Config.Key.Location.KICKOFF).getBlock();
            this.state = State.BLOCK;
            break;
        case BLOCK:
            if (block.getType() != MATERIAL) {
                block.setType(MATERIAL);
            }
            break;
        case ENTITY:
            if (!entity.isValid()) {
                this.block = entity.getLocation().getBlock();
                this.entity = null;
                this.state = State.BLOCK;
                this.block.setType(MATERIAL);
            }
            break;
        }
    }

    public void put(Location loc) {
        remove();
        this.state = State.BLOCK;
        this.entity = null;
        this.block = loc.getBlock();
        create();
    }

    public Location getLocation() {
        switch (state) {
        case NONE: return null;
        case BLOCK: return block.getLocation();
        case ENTITY: return entity.getLocation();
        default: return null;
        }
    }

    public SoccerPlayer getLastKicker() {
        return lastKicker;
    }

    public void setLastKicker(SoccerPlayer lastKicker) {
        this.lastKicker = lastKicker;
    }

    public void setLastKicker(Player player) {
        this.lastKicker = game.getSoccerPlayer(player);
    }

    private enum State {
        NONE,
        BLOCK,
        ENTITY,
        ;
    }
}
