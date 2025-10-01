// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.util.UUID;

import org.bukkit.Bukkit;

import net.trueog.diamondbankog.DiamondBankException;
import net.trueog.diamondbankog.PlayerBalanceChangedEvent;
import net.trueog.diamondbankog.PlayerBalanceChangedListener;

// Fetches the balance for the MiniPlaceholder upon balance changes.
public class BalanceChangeListener implements PlayerBalanceChangedListener {

    // Hook into DiamondBank-OG's balance change event.
    @Override
    public void onUpdate(PlayerBalanceChangedEvent event) {

        // Get the UUID of the player whose balance changed.
        final UUID playerUUID = event.getUuid();

        // Fetch diamond shard balance asynchronously.
        Bukkit.getScheduler().runTaskAsynchronously(DiamondBankMiniPlaceholdersOG.getPlugin(), () -> {

            try {

                // Ask DiamondBank-OG for the player's total shard count.
                final long totalShards = DiamondBankMiniPlaceholdersOG.diamondBankAPI().getTotalShards(playerUUID);

                // Cache the new balance for reference by the MiniPlaceholder.
                DiamondBankMiniPlaceholdersOG.cacheShardTotal(playerUUID, totalShards);

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

            }

        });

    }

}