package top.mcocet.pigeonChickenDupe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataManager {
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, ItemStack> chickenData;

    public DataManager(File dataFolder, Logger logger) {
        this.logger = logger;
        this.dataFile = new File(dataFolder, "data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.chickenData = new ConcurrentHashMap<>();
        
        // 确保数据文件存在
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "无法创建数据文件", e);
            }
        }
        
        // 加载现有数据到内存
        loadData();
    }

    /**
     * 从文件加载数据到内存
     */
    public void loadData() {
        try {
            dataConfig.load(dataFile);
            chickenData.clear();
            
            for (String key : dataConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ItemStack item = dataConfig.getItemStack(key);
                    if (item != null) {
                        chickenData.put(uuid, item);
                    }
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "无效的数据键: " + key, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载数据文件失败", e);
        }
    }

    /**
     * 保存内存中的数据到文件
     */
    public void saveData() {
        try {
            // 创建新的配置对象
            YamlConfiguration newConfig = new YamlConfiguration();
            
            // 将数据写入新配置
            for (java.util.Map.Entry<UUID, ItemStack> entry : chickenData.entrySet()) {
                newConfig.set(entry.getKey().toString(), entry.getValue());
            }
            
            // 保存到文件
            newConfig.save(dataFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "保存数据文件失败", e);
        }
    }

    /**
     * 设置鸡的物品数据（线程安全）
     */
    public void setChickenItem(UUID chickenUuid, ItemStack item) {
        chickenData.put(chickenUuid, item.clone());
    }

    /**
     * 移除鸡的物品数据（线程安全）
     */
    public void removeChickenItem(UUID chickenUuid) {
        chickenData.remove(chickenUuid);
    }

    /**
     * 获取鸡的物品数据（线程安全）
     */
    public ItemStack getChickenItem(UUID chickenUuid) {
        ItemStack item = chickenData.get(chickenUuid);
        return item != null ? item.clone() : null;
    }

    /**
     * 获取所有鸡的数据（线程安全）
     */
    public ConcurrentHashMap<UUID, ItemStack> getAllChickenData() {
        return chickenData;
    }

    /**
     * 异步保存数据
     */
    public void saveDataAsync(Runnable callback) {
        Thread.ofVirtual().name("PigeonChickenDupe-DataSaver").start(() -> {
            saveData();
            if (callback != null) {
                callback.run();
            }
        });
    }
}
