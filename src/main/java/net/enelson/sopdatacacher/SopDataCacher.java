package net.enelson.sopdatacacher;

import net.enelson.sopdatacacher.api.PlaceholderExtention;
import net.enelson.sopdatacacher.configs.ConfigManager;
import net.enelson.sopdatacacher.data.DataManager;
import net.enelson.sopdatacacher.database.SQLManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SopDataCacher extends JavaPlugin implements CommandExecutor {
    private static SopDataCacher instance;

    private ConfigManager configManager;
    private SQLManager sqlManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.sqlManager = new SQLManager(this);
        this.dataManager = new DataManager(this);
        this.dataManager.start();

        if (getCommand("sopdatacacher") != null) {
            getCommand("sopdatacacher").setExecutor(this);
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderExtention("sopdatacacher").register();
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
        }
    }

    @Override
    public void onDisable() {
        if (this.dataManager != null) {
            this.dataManager.shutdown();
        }
        if (this.sqlManager != null) {
            this.sqlManager.disconnect();
        }
    }

    public static SopDataCacher getInstance() {
        return instance;
    }

    public ConfigManager getConfigs() {
        return configManager;
    }

    public SQLManager getSQLManager() {
        return sqlManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        this.configManager.reloadConfigs();
        this.dataManager.reload();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sopdatacacher.admin") && !sender.hasPermission("adatacacher.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "SopDataCacher reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
        return true;
    }
}
