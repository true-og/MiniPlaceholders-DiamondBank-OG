// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Fetches the balance for the MiniPlaceholder upon player login.
public class JoinQuitListener implements Listener {

    // When a player joins the server, do this...
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {

        // Populate the in-memory balance cache asynchronously.
        DiamondBankMiniPlaceholdersOG.refreshShards(event.getPlayer().getUniqueId());

    }

    // When a player leaves or is kicked from the server, do this...
    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {

        // Drop the player from the cache.
        DiamondBankMiniPlaceholdersOG.clearPlayerFromCache(event.getPlayer().getUniqueId());

    }

}