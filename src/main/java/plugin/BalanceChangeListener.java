// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import net.trueog.diamondbankog.PlayerBalanceChangedEvent;
import net.trueog.diamondbankog.PlayerBalanceChangedListener;

// Updates the balance cache for the MiniPlaceholder upon balance changes.
public class BalanceChangeListener implements PlayerBalanceChangedListener {

    // Hook into DiamondBank-OG's balance change event.
    @Override
    public void onUpdate(PlayerBalanceChangedEvent event) {

        // The event already carries the up-to-date shard totals, so compute the
        // total directly instead of calling getTotalShards() — that would re-acquire
        // the transaction lock and risk deadlocking against the main thread.
        final long totalShards = event.getPlayerShards().getBank() + event.getPlayerShards().getInventory()
                + event.getPlayerShards().getEnderChest();

        // Cache the new balance for reference by the MiniPlaceholder.
        DiamondBankMiniPlaceholdersOG.cacheShardTotal(event.getUuid(), totalShards);

    }

}