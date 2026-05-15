package top.mcocet.pigeonChickenDupe;

import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropMultiplierListener implements Listener {
    private final PigeonChickenDupe plugin;

    public DropMultiplierListener(PigeonChickenDupe plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 检查功能是否启用
        if (!plugin.getConfig().getBoolean("EnableDropMultiplier", true)) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        // 只处理带有箱子的生物（驴、骡子、羊驼等）
        boolean isChestedAnimal = false;
        
        if (entity instanceof ChestedHorse) {
            // 驴和骡子
            isChestedAnimal = ((ChestedHorse) entity).isCarryingChest();
        } else if (entity instanceof Llama) {
            // 羊驼
            isChestedAnimal = ((Llama) entity).isCarryingChest();
        }
        
        // 只有带箱子的生物才应用掉落倍数
        if (isChestedAnimal) {
            // 获取配置的掉落倍数
            int dropMultiplier = plugin.getConfig().getInt("DropMultiplier", 3);
            
            // 获取当前的掉落物列表（包括箱子内的物品和生物自然掉落）
            List<ItemStack> drops = event.getDrops();
            
            // 遍历所有掉落物，将数量乘以倍数
            for (ItemStack drop : drops) {
                if (drop != null && drop.getType() != org.bukkit.Material.AIR) {
                    int originalAmount = drop.getAmount();
                    int newAmount = originalAmount * dropMultiplier;
                    drop.setAmount(newAmount);
                }
            }
        }
    }
}
