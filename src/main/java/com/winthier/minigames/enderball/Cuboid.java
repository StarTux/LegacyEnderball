package com.winthier.minigames.enderball;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class Cuboid extends Rectangle {
    private final int minY, maxY;

    public Cuboid(Location loc1, Location loc2) {
        super(loc1, loc2);
        final int y1 = loc1.getBlockY();
        final int y2 = loc2.getBlockY();
        this.minY = Math.min(y1, y2);
        this.maxY = Math.max(y1, y2);
    }

    protected boolean contains(int x, int y, int z) {
        if (!super.contains(x, z)) return false;
        if (y < minY || y > maxY) return false;
        return true;
    }

    @Override
    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ());
    }

    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
}
