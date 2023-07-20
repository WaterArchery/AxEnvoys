package com.artillexstudios.axenvoy.utils;

import com.artillexstudios.axenvoy.AxEnvoyPlugin;
import com.artillexstudios.axenvoy.envoy.Crate;
import com.artillexstudios.axenvoy.envoy.Envoy;
import com.artillexstudios.axenvoy.rewards.CommandReward;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    @NotNull
    public static Location deserializeLocation(@NotNull YamlDocument file, String path) {
        String locString = file.getString(path, "world;0;0;0");
        String[] split = locString.split(";");
        World world = Bukkit.getWorld(split[0]);
        return new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), 0, 0);
    }

    public static String serializeLocation(@NotNull Location location) {
        return "%s;%s;%s;%s".formatted(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Crate randomCrate(@NotNull Object2ObjectArrayMap<Crate, Double> map) {
        List<Pair<Crate, Double>> list = new ArrayList<>();
        map.forEach((key, value) -> list.add(new Pair<>(key, value)));

        EnumeratedDistribution<Crate> e = new EnumeratedDistribution<>(list);

        return e.sample();
    }

    public static CommandReward randomReward(@NotNull List<CommandReward> rewards) {
        List<Pair<CommandReward, Double>> list = new ArrayList<>();
        for (CommandReward reward : rewards) {
            list.add(new Pair<>(reward, reward.chance()));
        }

        EnumeratedDistribution<CommandReward> e = new EnumeratedDistribution<>(list);

        return e.sample();
    }

    @Nullable
    public static Location getNextLocation(@NotNull Envoy envoy, @NotNull Location loc) {
        Location center = loc.clone();
        loc.setX(loc.getBlockX() + ThreadLocalRandom.current().nextInt(envoy.getMaxDistance() * -1, envoy.getMaxDistance()));
        loc.setZ(loc.getBlockZ() + ThreadLocalRandom.current().nextInt(envoy.getMaxDistance() * -1, envoy.getMaxDistance()));

        Location loc2 = topBlock(loc);
        Location tempLoc = loc2.clone();
        if (envoy.getNotOnMaterials().contains(tempLoc.add(0, -1, 0).getBlock().getType())) return null;
        if (loc2.getY() < envoy.getMinHeight()) return null;
        if (loc2.getY() > envoy.getMaxHeight()) return null;
        if (loc.distanceSquared(center) < envoy.getMinDistance() * envoy.getMinDistance()) return null;

        return loc2;
    }

    @NotNull
    public static Location topBlock(@NotNull Location loc) {
        loc.getWorld().getChunkAtAsync(loc, false).thenAccept(chunk -> loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1));
        return loc;
    }

    @NotNull
    public static String fancyTime(long time) {
        Duration remainingTime = Duration.ofMillis(time);
        long total = remainingTime.getSeconds();
        long days = total / 84600;
        long hours = (total % 84600) / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;

        if (days > 0) return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @NotNull
    public static ItemStack createItem(final Material material, final int amount, final String name, @NotNull final List<String> lore, final String id, final boolean glow, final boolean addTag) {
        final ItemStack item = new ItemStack(material, amount);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(StringUtils.format(name).applyFallbackStyle(TextDecoration.ITALIC.withState(false)));
        ArrayList<Component> ar = new ArrayList<>();

        for (String s : lore) {
            ar.add(StringUtils.format(s).applyFallbackStyle(TextDecoration.ITALIC.withState(false)));
        }
        meta.lore(ar);

        if (glow) {
            item.addUnsafeEnchantment(Enchantment.WATER_WORKER, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (addTag) {
            NamespacedKey key = new NamespacedKey(AxEnvoyPlugin.getInstance(), "rivalsenvoy");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        }

        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    public static ItemStack createItem(Section section, String id) {
        return createItem(Material.matchMaterial(section.getString("material", "stone").toLowerCase(Locale.ENGLISH)), section.getInt("amount", 1), section.getString("name", ""), section.getStringList("lore", new ArrayList<>()), id, section.getBoolean("glow", false), true);
    }
}
