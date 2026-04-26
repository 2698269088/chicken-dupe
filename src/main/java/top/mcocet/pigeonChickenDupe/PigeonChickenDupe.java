package top.mcocet.pigeonChickenDupe;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PigeonChickenDupe extends JavaPlugin implements Listener {
    private DataManager dataManager;
    private ScheduledTask spawnTask;

    @Override
    public void onEnable() {
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // 初始化数据管理器
        dataManager = new DataManager(getDataFolder(), getLogger());

        int intervalSeconds = getConfig().getInt("SpawnInterval");
        long intervalTicks = Math.max(1L, intervalSeconds * 20L); // 确保至少 1 tick

        // 使用 Folia 兼容的调度器（也兼容 Paper）
        spawnTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            (task) -> spawnItemsForChickens(),
            1L,  // Folia 要求初始延迟至少为 1 tick
            intervalTicks
        );

        // 输出插件加载成功信息
        getLogger().info("PigeonChickenDupe");
        getLogger().info("插件加载成功");
        getLogger().info("Folia 兼容模式: " + isFolia());
    }



    @Override
    public void onDisable() {
        // 取消定时任务
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        
        // 异步保存数据
        if (dataManager != null) {
            dataManager.saveDataAsync(() -> {
                getLogger().info("数据已保存");
            });
        }
        
        // 输出插件卸载成功信息
        getLogger().info("PigeonChickenDupe");
        getLogger().info("插件卸载成功");
    }

    // 玩家右键点击实体事件
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Chicken chicken) {
            // 如果鸡已经成年
            if (chicken.isAdult()) {
                Player player = event.getPlayer();
                ItemStack item = player.getInventory().getItemInMainHand();
                Material material = item.getType();
                if (material != Material.AIR) {
                    UUID chickenUuid = chicken.getUniqueId();
                    
                    // 使用 DataManager 存储数据（线程安全）
                    dataManager.setChickenItem(chickenUuid, item);
                    
                    // 异步保存数据到文件
                    dataManager.saveDataAsync(null);
                    
                    // 播放音效，设置鸡的名称为手中物品的名称
                    Location location = entity.getLocation();
                    player.playEffect(location, Effect.CLICK2, null);
                    chicken.setCustomName(ChatColor.GREEN + "[物品] " + ChatColor.GOLD + item.getI18NDisplayName());
                    chicken.setCustomNameVisible(true);
                }
            }
        }
    }
    // 实体死亡事件
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Chicken) {
            UUID chickenUuid = entity.getUniqueId();
            
            // 使用 DataManager 移除数据（线程安全）
            dataManager.removeChickenItem(chickenUuid);
            
            // 异步保存数据到文件
            dataManager.saveDataAsync(null);
        }
    }
    private void spawnItemsForChickens() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        int SpawnNumber = getConfig().getInt("SpawnNumber");
        int maxChickensPerChunk = getConfig().getInt("MaxChickensPerChunk", 10);
        
        // 用于统计每个区块中的刷物品鸡数量
        Map<String, Integer> chunkChickenCount = new HashMap<>();
        
        // 遍历所有世界和已加载的区块
        for (var world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                String chunkKey = world.getName() + "," + chunk.getX() + "," + chunk.getZ();
                int chickenCountInChunk = 0;
                
                // 获取区块中的所有实体
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Chicken chicken) {
                        UUID uuid = chicken.getUniqueId();
                        ItemStack itemStack = dataManager.getChickenItem(uuid);
                        
                        if (itemStack != null) {
                            // 检查是否超过限制
                            if (chickenCountInChunk >= maxChickensPerChunk) {
                                continue; // 跳过这只鸡
                            }
                            
                            chickenCountInChunk++;
                            
                            // 在实体所在的区域线程中执行生成物品的操作
                            getServer().getRegionScheduler().execute(
                                this,
                                chicken.getLocation(),
                                () -> {
                                    // 再次检查鸡是否还存在
                                    if (chicken.isValid()) {
                                        ItemStack itemToDrop = itemStack.clone();
                                        itemToDrop.setAmount(SpawnNumber);
                                        chicken.getWorld().dropItemNaturally(chicken.getLocation(), itemToDrop);
                                    }
                                }
                            );
                        }
                    }
                }
                
                // 更新区块计数
                chunkChickenCount.put(chunkKey, chickenCountInChunk);
            }
        }
    }

    /**
     * 检测是否在 Folia 环境下运行
     */
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
