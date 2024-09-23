package com.yd.playerranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TopPlayerPlaceholder extends PlaceholderExpansion {
    private final PlayerRanking plugin;

    public TopPlayerPlaceholder(PlayerRanking plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "topplayers";
    }

    @Override
    public String getAuthor() {
        return "YD";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (identifier.startsWith("level_top")) {
            return handleTopRequest(identifier, "level", this.plugin::getPlayerLevel, Comparator.comparingInt(this.plugin::getPlayerLevel));
        } else if (identifier.startsWith("money_top")) {
            return handleTopRequest(identifier, "money", this.plugin::getPlayerMoney, Comparator.comparingDouble(this.plugin::getPlayerMoney));
        } else if (identifier.startsWith("combat_top")) {
            return handleTopRequest(identifier, "combat", this.plugin::getPlayerCombatPower, Comparator.comparingInt(this.plugin::getPlayerCombatPower));
        }
        return null;
    }

    /**
     * 공통 처리 메서드
     * 플레이어 이름만 반환하거나, 값만 반환할 수 있습니다.
     */
    private <T extends Comparable<T>> String handleTopRequest(String identifier, String type, Function<OfflinePlayer, T> valueGetter, Comparator<OfflinePlayer> comparator) {
        String rest = identifier.substring((type + "_top").length());

        // 랭크와 반환 타입 결정
        boolean returnValue = false;
        int rank = -1;

        if (rest.matches("\\d+")) {
            // 플레이어 이름만 출력
            rank = Integer.parseInt(rest) - 1;
        } else if (rest.matches("\\d+_" + getSuffix(type))) {
            // 값만 출력
            rank = Integer.parseInt(rest.substring(0, rest.indexOf("_"))) - 1;
            returnValue = true;
        } else {
            return null;
        }

        if (rank < 0) {
            return null;
        }

        // 모든 비OP 플레이어 가져오기
        List<OfflinePlayer> allPlayers = this.plugin.getAllNonOPPlayers();

        // 특정 타입의 데이터가 있는 플레이어만 필터링
        List<OfflinePlayer> playersWithData = allPlayers.stream()
                .filter(p -> hasPlayerData(p, type))
                .sorted(comparator.reversed())
                .collect(Collectors.toList());

        if (playersWithData.size() > rank) {
            OfflinePlayer topPlayer = playersWithData.get(rank);
            if (returnValue) {
                // 특정 값 반환
                T value = valueGetter.apply(topPlayer);
                if (value == null) {
                    return "NULL";
                }
                if (value instanceof Double) {
                    return String.format("%.2f", value);
                } else if (value instanceof Integer) {
                    return String.valueOf(value);
                } else {
                    return value.toString();
                }
            } else {
                // 플레이어 이름 반환
                return topPlayer.getName() != null ? topPlayer.getName() : "NULL";
            }
        } else {
            return "NULL";
        }
    }

    /**
     * 특정 플레이어의 특정 데이터가 존재하는지 확인
     */
    private boolean hasPlayerData(OfflinePlayer player, String type) {
        switch (type) {
            case "level":
                return plugin.getPlayerDataConfig().contains("players." + player.getUniqueId().toString() + ".level");
            case "money":
                return true; // Vault를 통해 항상 돈 데이터를 가져올 수 있다고 가정
            case "combat":
                return plugin.getPlayerDataConfig().contains("players." + player.getUniqueId().toString() + ".combat_power");
            default:
                return false;
        }
    }

    /**
     * 각 타입별 접미사를 반환합니다.
     */
    private String getSuffix(String type) {
        switch (type) {
            case "level":
                return "level";
            case "money":
                return "money";
            case "combat":
                return "power"; // 전투력의 경우 "power" 사용
            default:
                return "";
        }
    }
}
