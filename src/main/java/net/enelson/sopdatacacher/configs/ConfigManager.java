package net.enelson.sopdatacacher.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigManager {
    private final Plugin plugin;
    private YamlConfiguration config;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        reloadConfigs();
    }

    public void reloadConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getConfigs() {
        return config;
    }
}
