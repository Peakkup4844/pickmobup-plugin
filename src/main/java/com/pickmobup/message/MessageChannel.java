package com.pickmobup.message;

/**
 * Where a message is shown to the player.
 * <ul>
 *   <li>{@link #CHAT} – normal chat line (with prefix)</li>
 *   <li>{@link #ACTIONBAR} – text above the hotbar</li>
 *   <li>{@link #BOSSBAR} – boss bar at the top of the screen</li>
 *   <li>{@link #NONE} – do not show anything</li>
 * </ul>
 */
public enum MessageChannel {
    CHAT,
    ACTIONBAR,
    BOSSBAR,
    NONE
}
