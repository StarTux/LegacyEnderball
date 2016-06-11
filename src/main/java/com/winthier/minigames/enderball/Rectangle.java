package com.winthier.minigames.enderball;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class Rectangle {
    private final int maxZ, minZ, minX, maxX;

    public Rectangle(Location loc1, Location loc2) {
        final int x1 = loc1.getBlockX();
        final int x2 = loc2.getBlockX();
        final int z1 = loc1.getBlockZ();
        final int z2 = loc2.getBlockZ();
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);
    }

    protected boolean contains(int x, int z) {
        if (x < minX || x > maxX) return false;
        if (z < minZ || z > maxZ) return false;
        return true;
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getZ());
    }

    public boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockZ());
    }

    public boolean contains(Ball ball) {
        if (ball.isBlock()) return contains(ball.getBlock());
        if (ball.isEntity()) return contains(ball.getEntity());
        return false;
    }

    public boolean clamAndBounce(Ball ball) {
        if (ball.isEntity()) {
            return clamAndBounce(ball.getEntity());
        } else if (ball.isBlock()) {
            // Do nothing? Should never happen?
        }
        return false;
    }

    public boolean clamAndBounce(Entity entity) {
        final Location loc = entity.getLocation();
        final Vector vec = entity.getVelocity();
        final int x = loc.getBlockX();
        final int z = loc.getBlockZ();
        boolean result = false;
        if (x < minX) {
            loc.setX((double)minX + 0.5);
            vec.setX(-vec.getX());
            result = true;
        } else if (x > maxX) {
            loc.setX((double)maxX + 0.5);
            vec.setX(-vec.getX());
            result = true;
        }
        if (z < minZ) {
            loc.setZ((double)minZ + 0.5);
            vec.setZ(-vec.getZ());
            result = true;
        } else if (z > maxZ) {
            loc.setZ((double)maxZ + 0.5);
            vec.setZ(-vec.getZ());
            result = true;
        }
        if (result) {
            entity.teleport(loc);
            entity.setVelocity(vec);
        }
        return result;
    }

    public boolean clam(Entity entity) {
        final Location loc = entity.getLocation();
        final int x = loc.getBlockX();
        final int z = loc.getBlockZ();
        boolean result = false;
        if (x < minX) {
            loc.setX((double)minX + 0.5);
            result = true;
        } else if (x > maxX) {
            loc.setX((double)maxX + 0.5);
            result = true;
        }
        if (z < minZ) {
            loc.setZ((double)minZ + 0.5);
            result = true;
        } else if (z > maxZ) {
            loc.setZ((double)maxZ + 0.5);
            result = true;
        }
        if (result) {
            entity.teleport(loc);
        }
        return result;
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }

    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
}
