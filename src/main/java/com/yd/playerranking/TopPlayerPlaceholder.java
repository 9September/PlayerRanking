package com.yd.playerranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;

public class TopPlayerPlaceholder extends PlaceholderExpansion {
    private final PlayerRanking plugin;

    public TopPlayerPlaceholder(PlayerRanking plugin) {
        this.plugin = plugin;
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getIdentifier() {
        return "topplayers";
    }

    public String getAuthor() {
        return "YD";
    }

    public String getVersion() {
        return "1.0";
    }

    // In TopPlayerPlaceholder.java

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (identifier.startsWith("level_top")) {
            String rest = identifier.substring("level_top".length());
            if (rest.matches("\\d+")) {
                int rank = Integer.parseInt(rest) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingInt(this.plugin::getPlayerLevel).reversed());
                if (topPlayers.size() > rank) {
                    OfflinePlayer topPlayer = topPlayers.get(rank);
                    return topPlayer.getName() != null ? topPlayer.getName() : "Unknown";
                } else {
                    return "";
                }
            } else if (rest.matches("\\d+_level")) {
                int rank = Integer.parseInt(rest.substring(0, rest.indexOf("_"))) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingInt(this.plugin::getPlayerLevel).reversed());
                if (topPlayers.size() > rank) {
                    return String.valueOf(this.plugin.getPlayerLevel(topPlayers.get(rank)));
                } else {
                    return "";
                }
            }
        } else if (identifier.startsWith("money_top")) {
            String rest = identifier.substring("money_top".length());
            if (rest.matches("\\d+")) {
                int rank = Integer.parseInt(rest) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingDouble(this.plugin::getPlayerMoney).reversed());
                if (topPlayers.size() > rank) {
                    return topPlayers.get(rank).getName() != null ? topPlayers.get(rank).getName() : "Unknown";
                } else {
                    return "";
                }
            } else if (rest.matches("\\d+_money")) {
                int rank = Integer.parseInt(rest.substring(0, rest.indexOf("_"))) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingDouble(this.plugin::getPlayerMoney).reversed());
                if (topPlayers.size() > rank) {
                    return String.format("%.2f", this.plugin.getPlayerMoney(topPlayers.get(rank)));
                } else {
                    return "";
                }
            }
        } else if (identifier.startsWith("combatpower_top")) {
            String rest = identifier.substring("combatpower_top".length());
            if (rest.matches("\\d+")) {
                int rank = Integer.parseInt(rest) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingInt(this.plugin::getPlayerCombatPower).reversed());
                if (topPlayers.size() > rank) {
                    OfflinePlayer topPlayer = topPlayers.get(rank);
                    return topPlayer.getName() != null ? topPlayer.getName() : "Unknown";
                } else {
                    return "";
                }
            } else if (rest.matches("\\d+_combatpower")) {
                int rank = Integer.parseInt(rest.substring(0, rest.indexOf("_"))) - 1;
                List<OfflinePlayer> topPlayers = this.plugin.getTopPlayers(5, Comparator.comparingInt(this.plugin::getPlayerCombatPower).reversed());
                if (topPlayers.size() > rank) {
                    return String.valueOf(this.plugin.getPlayerCombatPower(topPlayers.get(rank)));
                } else {
                    return "";
                }
            }
        }

        return null;
    }


}
/*
            [level]
            Player Name
            %topplayers_level_top1%
            %topplayers_level_top2%
            %topplayers_level_top3%
            %topplayers_level_top4%
            %topplayers_level_top5%

            Player Levels
            %topplayers_level_top1_level%
            %topplayers_level_top2_level%
            %topplayers_level_top3_level%
            %topplayers_level_top4_level%
            %topplayers_level_top5_level%


            [money]
            Player Name
            %topplayers_money_top1%
            %topplayers_money_top2%
            %topplayers_money_top3%
            %topplayers_money_top4%
            %topplayers_money_top5%

            Player Money Balances
            %topplayers_money_top1_money%
            %topplayers_money_top2_money%
            %topplayers_money_top3_money%
            %topplayers_money_top4_money%
            %topplayers_money_top5_money%


            [combat]
            Player Name
            %topplayers_combat_top1%
            %topplayers_combat_top2%
            %topplayers_combat_top3%
            %topplayers_combat_top4%
            %topplayers_combat_top5%

            Player Combat Powers
            %topplayers_combat_top1_power%
            %topplayers_combat_top2_power%
            %topplayers_combat_top3_power%
            %topplayers_combat_top4_power%
            %topplayers_combat_top5_power%

*/
