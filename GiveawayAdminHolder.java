package com.yourserver.giveaway;

import java.util.List;

/** Marks the admin key menu (/giveaway admin). Extends the normal holder so it is closed/protected the same way. */
public class GiveawayAdminHolder extends GiveawayGUIHolder {

    private final List<String> keyIds;
    private final long durationSeconds;

    public GiveawayAdminHolder(List<String> keyIds, long durationSeconds) {
        this.keyIds = keyIds;
        this.durationSeconds = durationSeconds;
    }

    public List<String> keyIds() {
        return keyIds;
    }

    public long durationSeconds() {
        return durationSeconds;
    }
}
