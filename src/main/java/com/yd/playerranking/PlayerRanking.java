package com.yd.playerranking;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerRanking extends JavaPlugin implements Listener {
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    private static Economy econ = null;
    private Map<UUID, Integer> combatPowerCache = new HashMap<>();

    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault is not installed! This plugin requires Vault for economy.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new TopPlayerPlaceholder(this)).register();
        } else {
            getLogger().severe("PlaceholderAPI is not installed! This plugin requires PlaceholderAPI.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadPlayerData();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerRankingPlugin has been enabled!");
        new BukkitRunnable() {
            @Override
            public void run() {
                savePlayerData();
                getLogger().info("Player data saved successfully.");
            }
        }.runTaskTimer(this, 0L, 12000L); // 매 10분마다 저장 (12000 틱)
    }

    @Override
    public void onDisable() {
        // 플러그인 비활성화 시 데이터 저장
        savePlayerData();
        getLogger().info("Player data saved on disable.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    private void loadPlayerData() {
        this.playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!this.playerDataFile.exists()) {
            this.playerDataFile.getParentFile().mkdirs();
            try {
                this.playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.playerDataConfig = YamlConfiguration.loadConfiguration(this.playerDataFile);
        if (this.playerDataConfig.getKeys(false).contains("level")) { // 예시: 기존 데이터 형식 확인
            for (String key : this.playerDataConfig.getKeys(false)) {
                if (isValidUUID(key)) { // key가 UUID 형식인지 확인
                    // 예시: players.<UUID>.level으로 이동
                    int level = this.playerDataConfig.getInt(key + ".level", 0);
                    this.playerDataConfig.set("players." + key + ".level", level);
                    this.playerDataConfig.set(key, null); // 기존 키 삭제
                }
            }
            savePlayerData(); // 변경 사항 저장
        }
    }

    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void savePlayerData() {
        try {
            this.playerDataConfig.save(this.playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getPlayerMoney(OfflinePlayer player) {
        return econ.getBalance(player);
    }

    // In PlayerRanking.java

    public void setPlayerLevel(OfflinePlayer player, int level) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Attempted to set level for a null player or UUID.");
            return;
        }
        this.playerDataConfig.set(player.getUniqueId().toString(), Integer.valueOf(level));
        savePlayerData();
    }

    public int getPlayerLevel(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Attempted to get level for a null player or UUID.");
            return 0;
        }
        if (player.isOnline())
            return player.getPlayer().getLevel();
        return this.playerDataConfig.getInt(player.getUniqueId().toString(), 0);
    }

    public List<OfflinePlayer> getAllNonOPPlayers() {
        List<OfflinePlayer> allPlayers = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : getServer().getOfflinePlayers())
            if (!offlinePlayer.isOp())
                allPlayers.add(offlinePlayer);
        return allPlayers;
    }

    public List<OfflinePlayer> getTopPlayers(int topN, Comparator<OfflinePlayer> comparator) {
        List<OfflinePlayer> allPlayers = getAllNonOPPlayers();
        allPlayers.sort(comparator);
        return new ArrayList<>(allPlayers.subList(0, Math.min(topN, allPlayers.size())));
    }

    public int calculateCombatPower(Player player) {
        int totalCombatPower = 0;

        // 플레이어의 인벤토리 순회
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();

                    // 무기인지 확인하고 전투력을 계산
                    if (displayName.contains("활") || displayName.contains("단검") || displayName.contains("검") || displayName.contains("창") || displayName.contains("대검") || displayName.contains("지팡이") || displayName.contains("권총") || displayName.contains("하프") || displayName.contains("건틀릿")) { // 무기 종류를 추가하세요
                        int weaponPower = calculateWeaponPower(displayName);
                        totalCombatPower += weaponPower;
                    }

                    // 방어구인지 확인하고 방어력을 계산
                    if (displayName.contains("투구") || displayName.contains("갑옷") || displayName.contains("바지") || displayName.contains("신발")) { // 방어구 종류를 추가하세요
                        int armorPower = calculateArmorPower(displayName);
                        totalCombatPower += armorPower;
                    }
                }
            }
        }
        return totalCombatPower;
    }

    private int calculateWeaponPower(String displayName) {
        int basePower = 0;
        int enhancementPower = 0;

        if (displayName.contains("0차")) basePower = 100;
        else if (displayName.contains("1차")) basePower = 3000;
        else if (displayName.contains("2차")) basePower = 10000;
        else if (displayName.contains("3차")) basePower = 50000;
        else if (displayName.contains("4차")) basePower = 100000;
        else if (displayName.contains("5차")) basePower = 300000;

        enhancementPower = getEnhancementBonus(displayName);

        return basePower + enhancementPower;
    }

    private int calculateArmorPower(String displayName) {
        int baseHealth = 0;
        int enhancementHealth = 0;

        if (displayName.contains("0차")) baseHealth = 250;
        else if (displayName.contains("1차")) baseHealth = 7500;
        else if (displayName.contains("2차")) baseHealth = 25000;
        else if (displayName.contains("3차")) baseHealth = 125000;
        else if (displayName.contains("4차")) baseHealth = 250000;
        else if (displayName.contains("5차")) baseHealth = 750000;

        enhancementHealth = getEnhancementBonus(displayName);

        return baseHealth + enhancementHealth;
    }

    private int getEnhancementBonus(String displayName) {
        int enhancementLevel = 0;
        if (displayName.contains("+1")) enhancementLevel = 1;
        else if (displayName.contains("+2")) enhancementLevel = 2;
        else if (displayName.contains("+3")) enhancementLevel = 3;
        else if (displayName.contains("+4")) enhancementLevel = 4;
        else if (displayName.contains("+5")) enhancementLevel = 5;
        else if (displayName.contains("+6")) enhancementLevel = 6;
        else if (displayName.contains("+7")) enhancementLevel = 7;
        else if (displayName.contains("+8")) enhancementLevel = 8;
        else if (displayName.contains("+9")) enhancementLevel = 9;
        else if (displayName.contains("+10")) enhancementLevel = 10;

        switch (enhancementLevel) {
            case 1: return 1000;
            case 2: return 2000;
            case 3: return 3000;
            case 4: return 4000;
            case 5: return 5000;
            case 6: return 6000;
            case 7: return 7000;
            case 8: return 8000;
            case 9: return 9000;
            case 10: return 10000;
            default: return 0;
        }
    }

    public int getPlayerCombatPower(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Attempted to get combat power for a null player or UUID.");
            return 0;
        }

        // If the player is online, calculate their combat power directly
        if (player.isOnline()) {
            return calculateCombatPower(player.getPlayer());
        }

        // Otherwise, return cached combat power (if available)
        return combatPowerCache.getOrDefault(player.getUniqueId(), 0);
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        setPlayerLevel(player, newLevel);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, getPlayerLevel(player));
        // Calculate and cache combat power
        int combatPower = calculateCombatPower(player);
        combatPowerCache.put(player.getUniqueId(), combatPower);
        getLogger().info("Combat Power for " + player.getName() + ": " + combatPower);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, getPlayerLevel(player));
        combatPowerCache.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, player.getLevel());
    }
}