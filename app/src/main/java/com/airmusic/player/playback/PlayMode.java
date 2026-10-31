package com.airmusic.player.playback;

public enum PlayMode {
    SEQUENCE("sequence"),
    REPEAT_ONE("repeat_one"),
    SHUFFLE("shuffle"),
    FOLDER_LOOP("folder_loop");

    public final String key;

    PlayMode(String key) {
        this.key = key;
    }

    public static PlayMode fromKey(String key) {
        for (PlayMode mode : values()) {
            if (mode.key.equals(key)) return mode;
        }
        return SEQUENCE;
    }

    public PlayMode next() {
        PlayMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
