package com.winthier.minigames.enderball;

import com.winthier.minigames.util.Msg;
import com.winthier.minigames.util.SoundEffect;
import com.winthier.minigames.util.WorldLocation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class Config {
    private final EnderBall game;
    // Presets
    private World stadiumWorld;
    // Local Config
    private int groundLevel;
    private final Map<Key.Location, Location> locations = new EnumMap<>(Key.Location.class);
    private final Map<Key.Rectangle, Rectangle> rectangles = new EnumMap<>(Key.Rectangle.class);
    private final Map<Key.Cuboid, Cuboid> cuboids = new EnumMap<>(Key.Cuboid.class);
    private final Map<Key.LocationList, List<Location>> locationLists = new EnumMap<>(Key.LocationList.class);
    // Global Config
    private final Map<Key.SoundEffect, SoundEffect> soundEffects = new EnumMap<>(Key.SoundEffect.class);
    private final Map<Key.SoundEffectList, List<SoundEffect>> soundEffectLists = new EnumMap<>(Key.SoundEffectList.class);
    private final Map<SoccerTeam, ItemStack[]> uniforms = new EnumMap<>(SoccerTeam.class);
    private final Map<Key.Time, Long> times = new EnumMap<>(Key.Time.class);
    private final Map<Key.Item, ItemStack> items = new EnumMap<>(Key.Item.class);

    public Config(EnderBall game) {
        this.game = game;
    }

    public World getStadiumWorld() { return this.stadiumWorld; }
    public void setStadiumWorld(World world) { this.stadiumWorld = world; }

    public void load() {
        ConfigurationSection config;
        ConfigurationSection section;
        config = game.getWorldConfig(stadiumWorld, "field");
        // Load random settings
        groundLevel = config.getInt("GroundLevel");
        // Load locations
        for (Key.Location key : Key.Location.values()) {
            String value = config.getString(key.key);
            if (value == null) throw new RuntimeException("Configuration missing: " + key.key);
            Location loc = WorldLocation.fromString(value).toLocation(stadiumWorld);
            locations.put(key, loc);
        }
        // Load rectangles
        for (Key.Rectangle key : Key.Rectangle.values()) {
            List<String> values = config.getStringList(key.key);
            if (values == null || values.size() != 2) throw new RuntimeException("Configuration missing: " + key.key);
            Location loc1 = WorldLocation.fromString(values.get(0)).toLocation(stadiumWorld);
            Location loc2 = WorldLocation.fromString(values.get(1)).toLocation(stadiumWorld);
            Rectangle rect = new Rectangle(loc1, loc2);
            rectangles.put(key, rect);
        }
        // Load cuboids
        for (Key.Cuboid key : Key.Cuboid.values()) {
            List<String> values = config.getStringList(key.key);
            if (values == null || values.size() != 2) throw new RuntimeException("Configuration missing: " + key.key);
            Location loc1 = WorldLocation.fromString(values.get(0)).toLocation(stadiumWorld);
            Location loc2 = WorldLocation.fromString(values.get(1)).toLocation(stadiumWorld);
            Cuboid cube = new Cuboid(loc1, loc2);
            cuboids.put(key, cube);
        }
        // Location lists
        for (Key.LocationList key : Key.LocationList.values()) {
            List<String> values = config.getStringList(key.key);
            if (values == null || values.isEmpty()) throw new RuntimeException("Configuration missing: " + key.key);
            List<Location> list = new ArrayList<Location>();
            loadLocationList(values, list);
            locationLists.put(key, list);
        }
        // GLOBAL - Default from /config/EnderBall/config.yml; then from network packet
        // Times
        config = game.getConfig();
        ConfigurationSection defaultConfig = game.getConfigFile("config");
        for (String key : defaultConfig.getKeys(true)) config.addDefault(key, defaultConfig.get(key));
        for (Key.Time key : Key.Time.values()) {
            if (!config.contains(key.key)) throw new RuntimeException("config: Configuration missing: " + key.key);
            times.put(key, config.getLong(key.key));
        }
        // Sound effects
        config = game.getConfigFile("sounds");
        for (Key.SoundEffect key : Key.SoundEffect.values()) {
            section = config.getConfigurationSection(key.key);
            if (section == null) throw new RuntimeException("sounds: Configuration missing: " + key.key);
            soundEffects.put(key, SoundEffect.fromConfig(section));
        }
        for (Key.SoundEffectList key : Key.SoundEffectList.values()) {
            if (!config.isList(key.key)) throw new RuntimeException("sounds: Configuration missing: " + key.key);
            List<SoundEffect> list = new ArrayList<>();
            for (Map<?, ?> map : config.getMapList(key.key)) {
                section = new MemoryConfiguration().createSection("tmp", map);
                list.add(SoundEffect.fromConfig(section));
            }
            soundEffectLists.put(key, list);
        }
        // Items
        config = game.getConfigFile("items");
        for (Key.Item key : Key.Item.values()) {
            if (!config.isItemStack(key.key)) throw new RuntimeException("items: Configuration missing: " + key.key);
            ItemStack item = config.getItemStack(key.key);
            items.put(key, item);
        }
        // Temp: Set team colors.
        // North => Red
        // South => Blue.
        // Could be customizable later on.
        for (SoccerTeam team : SoccerTeam.values()) {
            ItemStack[] slots = new ItemStack[4];
            if (team != SoccerTeam.SPECTATOR) {
                slots[3] = null;
                slots[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
                slots[1] = new ItemStack(Material.LEATHER_LEGGINGS);
                slots[0] = new ItemStack(Material.LEATHER_BOOTS);
            }
            for (ItemStack slot : slots) {
                if (slot == null || slot.getType() == Material.AIR) continue;
                ItemMeta meta = slot.getItemMeta();
                if (!(meta instanceof LeatherArmorMeta)) continue;
                LeatherArmorMeta leather = (LeatherArmorMeta)meta;
                Color color = team == SoccerTeam.NORTH ? Color.RED : Color.BLUE;
                leather.setColor(color);
                leather.setDisplayName(getTeamDisplayName(team));
                slot.setItemMeta(meta);
                uniforms.put(team, slots);
            }
        }
    }

    public long get(Key.Time key) {
        return times.get(key);
    }

    public Location get(Key.Location key) {
        return locations.get(key);
    }

    public Rectangle get(Key.Rectangle key) {
        return rectangles.get(key);
    }

    public Cuboid get(Key.Cuboid key) {
        return cuboids.get(key);
    }

    public List<Location> get(Key.LocationList key) {
        return locationLists.get(key);
    }

    public int getGroundLevel() { return groundLevel; }

    public SoundEffect get(Key.SoundEffect key) {
        return soundEffects.get(key);
    }

    public List<SoundEffect> get(Key.SoundEffectList key) {
        return soundEffectLists.get(key);
    }

    public ItemStack[] getUniform(SoccerTeam team) {
        return uniforms.get(team);
    }

    public ItemStack get(Key.Item key) {
        return items.get(key).clone();
    }

    public String getTeamPrefix(SoccerTeam team) {
        switch (team) {
        case NORTH: return Msg.format("&c");
        case SOUTH: return Msg.format("&9");
        default: return "";
        }
    }

    public String getTeamDisplayName(SoccerTeam team) {
        String name = "";
        switch (team) {
        case NORTH:
            name = "Red";
            break;
        case SOUTH:
            name = "Blue";
            break;
        }
        return getTeamPrefix(team) + name;
    }

    public Color getTeamFireworkColor(SoccerTeam team) {
        switch (team) {
        case NORTH: return Color.RED;
        case SOUTH: return Color.BLUE;
        default: return null;
        }
    }

    private void loadLocationList(List<String> stringList, List<Location> locationList) {
        for (String string : stringList) {
            WorldLocation wloc = WorldLocation.fromString(string);
            Location loc = wloc.toLocation(stadiumWorld);
            locationList.add(loc);
        }
    }

    public static class Key {
        public static enum Random {
            GROUND_LEVEL("GroundLevel"),
            ;
            public final String key;
            Random(String key) { this.key = key; }
        }
        public static enum Time {
            SELECT_TEAM("SelectTeam"),
            GAME("Game"),
            KICKOFF("Kickoff"),
            ;
            public final String key;
            Time(String key) { this.key = "time." + key; }
        }
        public static enum Location {
            KICKOFF("Kickoff"),
            KICKOFF_NORTH("KickoffNorth"),
            KICKOFF_SOUTH("KickoffSouth"),
            PENALTY_NORTH("PenaltyNorth"),
            PENALTY_SOUTH("PenaltySouth"),
            ;
            public final String key;
            Location(String key) { this.key = "locations." + key; }
        }
        public static enum Rectangle {
            PLAYING_FIELD("PlayingField"),
            PLAYER_ZONE("PlayerZone"),
            PENALTY_NORTH("PenaltyNorth"),
            PENALTY_SOUTH("PenaltySouth"),
            ;
            public final String key;
            Rectangle(String key) { this.key = "rectangles." + key; }
        }
        public static enum Cuboid {
            GOAL_NORTH("GoalNorth"),
            GOAL_SOUTH("GoalSouth"),
            ;
            public final String key;
            Cuboid(String key) { this.key = "cuboids." + key; }
        }
        public static enum LocationList {
            FIREWORKS_NORTH("fireworks.North"),
            FIREWORKS_SOUTH("fireworks.South"),
            PLAYERS_NORTH("players.North"),
            PLAYERS_SOUTH("players.South"),
            ;
            public final String key;
            LocationList(String key) { this.key = key; }
        }
        public static enum SoundEffect {
            BALL_KICK_SHORT("BallKickShort"),
            BALL_KICK_LONG("BallKickLong"),
            BALL_BOUNCE("BallBounce"),
            BALL_LAND("BallLand"),
            BALL_BLOCK("BallBlock"),
            GOAL("Goal"),
            SELECT_TEAM("SelectTeam"),
            ;
            public final String key;
            SoundEffect(String key) { this.key = key; }
        }
        public static enum SoundEffectList {
            APPLAUSE("applause"),
            ;
            public final String key;
            SoundEffectList(String key) { this.key = key; }
        }
        public static enum Item {
            LEAVE("Leave"),
            MANUAL("Manual"),
            ;
            public final String key;
            Item(String key) { this.key = key; }
        }
    }
}
