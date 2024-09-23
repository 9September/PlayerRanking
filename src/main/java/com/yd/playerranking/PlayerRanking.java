package com.yd.playerranking;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault가 설치되지 않았습니다! 이 플러그인은 경제 기능을 위해 Vault가 필요합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new TopPlayerPlaceholder(this)).register();
        } else {
            getLogger().severe("PlaceholderAPI가 설치되지 않았습니다! 이 플러그인은 PlaceholderAPI가 필요합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadPlayerData();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerRankingPlugin이 활성화되었습니다!");

        new BukkitRunnable() {
            @Override
            public void run() {
                savePlayerData();
                getLogger().info("플레이어 데이터가 성공적으로 저장되었습니다.");
            }
        }.runTaskTimer(this, 0L, 12000L); // 매 10분마다 저장 (12000 틱)
    }

    @Override
    public void onDisable() {
        // 플러그인 비활성화 시 데이터 저장
        savePlayerData();
        getLogger().info("플러그인 비활성화 시 데이터가 저장되었습니다.");
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

    /**
     * 플레이어 데이터 설정 파일을 불러옵니다.
     */
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

        // 데이터 마이그레이션: 기존 데이터 형식에서 새로운 형식으로 이동
        Set<String> rootKeys = new HashSet<>(this.playerDataConfig.getKeys(false));
        boolean needsMigration = false;

        for (String key : rootKeys) {
            if (isValidUUID(key)) { // key가 UUID 형식인지 확인
                if (this.playerDataConfig.contains(key + ".level") || this.playerDataConfig.contains(key + ".combat_power")) {
                    needsMigration = true;
                    break;
                }
            }
        }

        if (needsMigration) {
            for (String key : rootKeys) {
                if (isValidUUID(key)) {
                    // 레벨 마이그레이션
                    if (this.playerDataConfig.contains(key + ".level")) {
                        int level = this.playerDataConfig.getInt(key + ".level", 0);
                        this.playerDataConfig.set("players." + key + ".level", level);
                        getLogger().info("UUID " + key + "의 레벨 데이터를 players." + key + ".level로 마이그레이션했습니다.");
                    }

                    // 전투력 마이그레이션
                    if (this.playerDataConfig.contains(key + ".combat_power")) {
                        int combatPower = this.playerDataConfig.getInt(key + ".combat_power", 0);
                        this.playerDataConfig.set("players." + key + ".combat_power", combatPower);
                        getLogger().info("UUID " + key + "의 전투력 데이터를 players." + key + ".combat_power로 마이그레이션했습니다.");
                    }

                    // 기존 루트 키 삭제
                    this.playerDataConfig.set(key, null);
                }
            }
            savePlayerData(); // 변경 사항 저장
            getLogger().info("플레이어 데이터 마이그레이션이 완료되었습니다.");
        }
    }

    /**
     * 주어진 문자열이 유효한 UUID 형식인지 확인합니다.
     */
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 플레이어 데이터를 저장합니다.
     */
    private void savePlayerData() {
        try {
            this.playerDataConfig.save(this.playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 플레이어의 돈을 반환합니다.
     */
    public double getPlayerMoney(OfflinePlayer player) {
        return econ.getBalance(player);
    }

    /**
     * 플레이어의 레벨을 설정합니다.
     */
    public void setPlayerLevel(OfflinePlayer player, int level) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Null 플레이어 또는 UUID의 레벨을 설정하려고 시도했습니다.");
            return;
        }
        String path = "players." + player.getUniqueId().toString() + ".level";
        this.playerDataConfig.set(path, level); // 경로 수정
        savePlayerData();
        getLogger().info("플레이어 " + player.getName() + "의 레벨을 " + level + "으로 저장했습니다.");
    }

    /**
     * 플레이어의 레벨을 가져옵니다.
     */
    public int getPlayerLevel(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Null 플레이어 또는 UUID의 레벨을 가져오려 시도했습니다.");
            return 0;
        }
        if (player.isOnline()) {
            int level = player.getPlayer().getLevel();
            getLogger().info("온라인 플레이어 " + player.getName() + "의 레벨: " + level);
            return level;
        }
        String path = "players." + player.getUniqueId().toString() + ".level";
        if (this.playerDataConfig.contains(path)) {
            int level = this.playerDataConfig.getInt(path, 0);
            getLogger().info("플레이어 " + player.getName() + "의 레벨: " + level);
            return level;
        } else {
            getLogger().info("플레이어 " + player.getName() + "의 레벨 데이터가 없습니다.");
            return 0;
        }
    }

    /**
     * OP가 아닌 모든 플레이어 목록을 가져옵니다.
     */
    public List<OfflinePlayer> getAllNonOPPlayers() {
        List<OfflinePlayer> allPlayers = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : getServer().getOfflinePlayers())
            if (!offlinePlayer.isOp())
                allPlayers.add(offlinePlayer);
        return allPlayers;
    }

    /**
     * 상위 N명의 플레이어를 가져옵니다.
     */
    public List<OfflinePlayer> getTopPlayers(int topN, Comparator<OfflinePlayer> comparator) {
        List<OfflinePlayer> allPlayers = getAllNonOPPlayers();
        allPlayers.sort(comparator);
        return new ArrayList<>(allPlayers.subList(0, Math.min(topN, allPlayers.size())));
    }

    /**
     * 플레이어의 전투력을 계산합니다.
     */
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
                    if (displayName.contains("활") || displayName.contains("단검") || displayName.contains("검") ||
                            displayName.contains("창") || displayName.contains("대검") || displayName.contains("지팡이") ||
                            displayName.contains("권총") || displayName.contains("하프") || displayName.contains("건틀릿")) { // 무기 종류를 추가하세요
                        int weaponPower = calculateWeaponPower(displayName);
                        totalCombatPower += weaponPower;
                    }

                    // 방어구인지 확인하고 방어력을 계산
                    if (displayName.contains("투구") || displayName.contains("갑옷") ||
                            displayName.contains("바지") || displayName.contains("신발")) { // 방어구 종류를 추가하세요
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

    /**
     * 플레이어의 전투력을 가져옵니다.
     */
    public int getPlayerCombatPower(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Null 플레이어 또는 UUID의 전투력을 가져오려 시도했습니다.");
            return 0;
        }
        String path = "players." + player.getUniqueId().toString() + ".combat_power";
        if (this.playerDataConfig.contains(path)) {
            int combatPower = this.playerDataConfig.getInt(path, 0);
            getLogger().info("플레이어 " + player.getName() + "의 전투력: " + combatPower);
            return combatPower;
        } else {
            getLogger().info("플레이어 " + player.getName() + "의 전투력 데이터가 없습니다.");
            return 0;
        }
    }

    /**
     * 플레이어의 전투력을 설정합니다.
     */
    public void setPlayerCombatPower(OfflinePlayer player, int combatPower) {
        if (player == null || player.getUniqueId() == null) {
            getLogger().warning("Null 플레이어 또는 UUID의 전투력을 설정하려 시도했습니다.");
            return;
        }
        String path = "players." + player.getUniqueId().toString() + ".combat_power";
        this.playerDataConfig.set(path, combatPower); // 경로 수정
        savePlayerData();
        getLogger().info("플레이어 " + player.getName() + "의 전투력을 " + combatPower + "으로 저장했습니다.");
    }

    /**
     * PlaceholderExpansion에서 playerDataConfig에 접근할 수 있도록 getter 추가
     */
    public FileConfiguration getPlayerDataConfig() {
        return this.playerDataConfig;
    }

    // 이벤트 핸들러들

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
        // 전투력 계산 및 저장
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
        getLogger().info("플레이어 " + player.getName() + "의 전투력: " + combatPower);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, getPlayerLevel(player));
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, player.getLevel());
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            int combatPower = calculateCombatPower(player);
            setPlayerCombatPower(player, combatPower); // 전투력 업데이트
        }
    }
}
