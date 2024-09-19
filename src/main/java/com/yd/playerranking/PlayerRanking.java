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

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerRanking extends JavaPlugin implements Listener {
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    private static Economy econ = null;

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
    }

    private void savePlayerData() {
        try {
            this.playerDataConfig.save(this.playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPlayerLevel(OfflinePlayer player, int level) {
        this.playerDataConfig.set(player.getUniqueId().toString() + ".level", level);
        savePlayerData();
    }

    public void setPlayerCombatPower(OfflinePlayer player, int combatPower) {
        this.playerDataConfig.set(player.getUniqueId().toString() + ".combat_power", combatPower);
        savePlayerData();
    }

    public int getPlayerLevel(OfflinePlayer player) {
        if (player.isOnline())
            return player.getPlayer().getLevel();
        return this.playerDataConfig.getInt(player.getUniqueId().toString() + ".level", 0);
    }

    public double getPlayerMoney(OfflinePlayer player) {
        return econ.getBalance(player);
    }

    public int getPlayerCombatPower(OfflinePlayer player) {
        return this.playerDataConfig.getInt(player.getUniqueId().toString() + ".combat_power", 0);
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
        return allPlayers.subList(0, Math.min(topN, allPlayers.size()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, player.getLevel());
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, player.getLevel());
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        setPlayerLevel(player, player.getLevel());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int combatPower = calculateCombatPower(player);
        setPlayerCombatPower(player, combatPower);
    }

    public int calculateCombatPower(Player player) {
        int totalAttackPower = 0;
        int totalHealthIncrease = 0;

        // Get player's inventory
        PlayerInventory inventory = player.getInventory();

        // Get all items from inventory (including armor slots)
        List<ItemStack> items = new ArrayList<>();
        items.addAll(Arrays.asList(inventory.getContents()));
        items.addAll(Arrays.asList(inventory.getArmorContents()));

        for (ItemStack item : items) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            String itemName = meta.hasDisplayName() ? meta.getDisplayName() : "";

            int stage = getItemStage(itemName);
            int enhancement = getEnhancementLevel(itemName, stage);

            if (isWeapon(item)) {
                int attackPower = getWeaponBaseAttack(stage) + getWeaponEnhancementAttack(enhancement);
                totalAttackPower += attackPower;
            } else if (isArmor(item)) {
                int healthIncrease = getArmorEnhancementHealth(enhancement);
                totalHealthIncrease += healthIncrease;
            }
        }

        // 전투력은 공격력과 체력 증가를 합산하여 계산하거나, 필요에 따라 다른 방식으로 처리할 수 있습니다.
        // 여기서는 공격력과 체력 증가를 단순 합산하여 전투력으로 사용하겠습니다.
        int totalCombatPower = totalAttackPower + totalHealthIncrease;
        return totalCombatPower;
    }

    public boolean isWeapon(ItemStack item) {
        // Define weapon materials
        Material type = item.getType();
        return type.toString().endsWith("_SWORD") || type.toString().endsWith("_AXE") || type.toString().endsWith("_BOW") || type.toString().endsWith("_CROSSBOW");
    }

    public boolean isArmor(ItemStack item) {
        // Define armor materials
        Material type = item.getType();
        return type.toString().endsWith("_HELMET") || type.toString().endsWith("_CHESTPLATE") ||
                type.toString().endsWith("_LEGGINGS") || type.toString().endsWith("_BOOTS");
    }

    public int getItemStage(String itemName) {
        // Extract stage from item name (e.g., "3차" means stage 3)
        for (int stage = 0; stage <= 5; stage++) {
            if (itemName.contains(stage + "차")) {
                return stage;
            }
        }
        return 0; // Default stage if not found
    }

    public int getEnhancementLevel(String itemName, int stage) {
        // Extract enhancement level from item name (e.g., "+5")
        // Stages 0~4 can have +1~+7, stage 5 can have +1~+10
        int maxEnhancement = stage == 5 ? 10 : 7;
        for (int enhancement = maxEnhancement; enhancement >= 1; enhancement--) {
            if (itemName.contains("+" + enhancement)) {
                return enhancement;
            }
        }
        return 0; // No enhancement if not found
    }

    public int getWeaponBaseAttack(int stage) {
        // Weapon base attack power based on stage
        switch (stage) {
            case 0:
                return 100;
            case 1:
                return 300;
            case 2:
                return 800;
            case 3:
                return 2200;
            case 4:
                return 6000;
            case 5:
                return 16000;
            default:
                return 0;
        }
    }

    public int getWeaponEnhancementAttack(int enhancementLevel) {
        // Weapon attack power increase based on enhancement level
        switch (enhancementLevel) {
            case 1:
                return 5;
            case 2:
                return 10;
            case 3:
                return 15;
            case 4:
                return 20;
            case 5:
                return 25;
            case 6:
                return 30;
            case 7:
                return 35;
            case 8:
                return 100;
            case 9:
                return 500;
            case 10:
                return 2500;
            default:
                return 0;
        }
    }

    public int getArmorEnhancementHealth(int enhancementLevel) {
        // Armor health increase based on enhancement level
        switch (enhancementLevel) {
            case 1:
            case 2:
            case 3:
            case 4:
                return 150;
            case 5:
            case 6:
            case 7:
                return 500;
            case 8:
                return 1000;
            case 9:
                return 5000;
            case 10:
                return 10000;
            default:
                return 0;
        }
    }
}