package com.winthier.minigames.enderball;

public enum SoccerTeam {
    SPECTATOR,
    NORTH,
    SOUTH,
    ;

    public boolean isPlaying() {
        if (this == SPECTATOR) return false;
        return true;
    }

    public static SoccerTeam[] both() {
        SoccerTeam[] both = new SoccerTeam[2];
        both[0] = NORTH;
        both[1] = SOUTH;
        return both;
    }

    public SoccerTeam other() {
        switch (this) {
        case SPECTATOR: return this;
        case NORTH: return SOUTH;
        case SOUTH: return NORTH;
        default: return SPECTATOR;
        }
    }
}
