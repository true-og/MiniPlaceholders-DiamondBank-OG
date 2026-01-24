// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.trueog.diamondbankog.DiamondBankAPIJava;
import net.trueog.diamondbankog.DiamondBankException;
import net.trueog.utilitiesog.UtilitiesOG;

// Main plugin class.
public class DiamondBankMiniPlaceholdersOG extends JavaPlugin {

    // Declare internal fields.
    private static DiamondBankMiniPlaceholdersOG plugin;
    private static DiamondBankAPIJava diamondBankAPI;
    private static String pluginPrefix;
    // Cache for total shards for each player.
    private static final Map<UUID, Long> SHARD_CACHE = new ConcurrentHashMap<>();
    // Helps prevent duplicate fetches.
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    // When the server loads the plugin, do this...
    @Override
    public void onEnable() {

        // Populate the plugin instance with the real live instance.
        plugin = this;

        // Get the plugin name and format it into a prefix.
        pluginPrefix = ("[" + getPlugin().getName() + "]");

        // Initialize the DiamondBank-OG API.
        final RegisteredServiceProvider<DiamondBankAPIJava> provider = getServer().getServicesManager()
                .getRegistration(DiamondBankAPIJava.class);

        // If the DiamondBank-OG API failed to initialize, do this...
        if (provider == null) {

            // Tell Bukkit to disable this plugin, and inform the console.
            disableSelf("DiamondBank-OG API is null – disabling " + getPlugin().getName() + "!");
            return;

        }

        // Assign the active instance of DiamondBank-OG to the API handler.
        diamondBankAPI = provider.getProvider();

        // Listen to DiamondBank balance change events to keep the balance cache hot.
        diamondBankAPI.registerEventListener(new BalanceChangeListener());

        // Listen to Bukkit join/quit to pre-warm and clean the balance cache.
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(), this);

        // Pre-warm balance cache for already-online players, such as in plugin reloads.
        Bukkit.getOnlinePlayers().forEach(p -> refreshShards(p.getUniqueId()));

        // Fancy Async MiniPlaceholder declaration that only runs once per player per
        // balance update.
        UtilitiesOG.registerAudiencePlaceholder("diamondbankog_balance",
                player -> getFormattedDiamondBalance(player.getUniqueId()));

    }

    // Plugin instance getter for secondary classes.
    public static DiamondBankMiniPlaceholdersOG getPlugin() {

        return plugin;

    }

    // DiamondBank-OG API getter for secondary classes.
    public static DiamondBankAPIJava diamondBankAPI() {

        return diamondBankAPI;

    }

    // Receive player balance from within DiamondBank-OG's
    // PlayerBalanceChangedEvent.
    static void cacheShardTotal(UUID uuid, long totalShards) {

        // Store player balance in the cache.
        SHARD_CACHE.put(uuid, totalShards);

    }

    // Triggers another DiamondBank-OG API balance update on-demand, but only if it
    // differs from the current value.
    static void refreshShards(UUID uuid) {

        // Avoids concurrent, identical balance fetches for the same player.
        if (!IN_FLIGHT.add(uuid)) {

            // Do nothing, task already completed.
            return;

        }

        // Runs code off of the main server thread.
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {

            try {

                // Ask DiamondBank-OG for the player's total shard count.
                final long totalShards = diamondBankAPI().getTotalShards(uuid);

                // Cache the player balance for reference by individual players audience
                // MiniPlaceholders.
                SHARD_CACHE.put(uuid, totalShards);

            }
            // If the DiamondBank-OG economy is disabled, do this...
            catch (DiamondBankException.EconomyDisabledException error) {

                // Commit sudoku, inform console of the DiamondBank-OG economy being disabled.
                DiamondBankMiniPlaceholdersOG.disableSelf("The DiamondBank-OG economy is disabled — disabling "
                        + DiamondBankMiniPlaceholdersOG.getPlugin().getName() + "!");

            }
            // If the DiamondBank-OG database is having problems, do this...
            catch (DiamondBankException.DatabaseException error) {

                // Commit sudoku, inform console of the DiamondBank-OG database error.
                DiamondBankMiniPlaceholdersOG.disableSelf("DiamondBank-OG database error — disabling "
                        + DiamondBankMiniPlaceholdersOG.getPlugin().getName() + ". Cause: " + error.getMessage());

            } finally {

                // Remove the lock from the anti double lookup mechanism.
                IN_FLIGHT.remove(uuid);

            }

        });

    }

    // Returns a formatted diamond balance string derived from the DiamondBank-OG
    // API.
    static String getFormattedDiamondBalance(UUID uuid) {

        // Attempt to fetch the current shard total from the cache.
        final Long shardTotal = SHARD_CACHE.get(uuid);
        // If the cache is empty, do this...
        if (shardTotal == null) {

            // Kick off an Async balance cache refresh.
            refreshShards(uuid);

            // Return a neutral "loading" indicator until the player's balance does exist in
            // the cache.
            return "&eLoading... re-try";

        }

        // Translate Diamond Shards into Diamonds by dividing the shard total long by 9.
        final long diamonds = shardTotal / 9L;

        // Return formatted Diamond Balance.
        return "&b" + diamonds;

    }

    // Prevents stale cache entries.
    static void clearPlayerFromCache(UUID uuid) {

        // Remove a player from both in-memory balance caches.
        SHARD_CACHE.remove(uuid);
        IN_FLIGHT.remove(uuid);

    }

    // Helps this plugin kill itself gracefully (in minecraft).
    public static void disableSelf(String reason) {

        // Run a meta-task on the Bukkit server outside of the context of this plugin.
        Bukkit.getScheduler().runTask(getPlugin(), () -> {

            // Attempt to fetch the active instance of this plugin.
            final DiamondBankMiniPlaceholdersOG pluginInstance = getPlugin();

            // If this plugin is already disabled, do this...
            if (!pluginInstance.isEnabled()) {

                // Do nothing, task already completed.
                return;

            }

            // Inform console of this plugin being disabled.
            UtilitiesOG.logToConsole(pluginPrefix, reason);

            // Commit sudoku.
            Bukkit.getPluginManager().disablePlugin(pluginInstance);

        });

    }

}