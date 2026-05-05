package net.enelson.sopdatacacher.data;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopdatacacher.SopDataCacher;
import net.enelson.sopdatacacher.model.CachedPlaceholder;
import net.enelson.sopdatacacher.model.TopLineSettings;
import net.enelson.sopdatacacher.tops.Top;
import net.enelson.sopdatacacher.tops.TopElement;
import net.enelson.sopdatacacher.util.FormatUtils;
import net.enelson.sopdatacacher.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final SopDataCacher plugin;
    private final Map<String, CachedPlaceholder> placeholders = new ConcurrentHashMap<>();
    private final Map<String, Top> tops = new ConcurrentHashMap<>();
    private BukkitTask task;

    public DataManager(SopDataCacher plugin) {
        this.plugin = plugin;
        loadPlaceholders();
    }

    public void start() {
        shutdownTaskOnly();

        int periodSeconds = plugin.getConfigs().getConfigs().getInt("placeholders-update-time", 10);
        if (periodSeconds < 1) {
            periodSeconds = 1;
        }

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateData, 0L, periodSeconds * 20L);
    }

    public void reload() {
        shutdown();
        loadPlaceholders();
        start();
    }

    public void shutdown() {
        shutdownTaskOnly();
        placeholders.clear();
        tops.clear();
    }

    private void shutdownTaskOnly() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void loadPlaceholders() {
        placeholders.clear();
        tops.clear();

        ConfigurationSection section = plugin.getConfigs().getConfigs().getConfigurationSection("named-placeholders");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String alias = key.toLowerCase();
            String base = key + ".";
            String placeholder = section.getString(base + "placeholder");
            String def = section.getString(base + "default", "");
            String format = section.getString(base + "format", "none");
            TopLineSettings topLine = loadTopLine(section, key);

            if (placeholder == null || placeholder.trim().isEmpty()) {
                continue;
            }

            placeholders.put(alias, new CachedPlaceholder(alias, placeholder.trim(), def, format, topLine));
        }
    }

    private TopLineSettings loadTopLine(ConfigurationSection root, String key) {
        ConfigurationSection topLineSection = root.getConfigurationSection(key + ".top-line");
        if (topLineSection == null) {
            return null;
        }

        int width = topLineSection.getInt("width", 32);
        String filler = topLineSection.getString("filler", ".");
        String template = topLineSection.getString("template", "{name} {fill} {value}");
        boolean useMinecraftWidth = topLineSection.getBoolean("use-minecraft-width", true);
        int minFill = topLineSection.getInt("min-fill", 3);

        Map<Integer, String> positionTemplates = new HashMap<>();

        ConfigurationSection posSection = topLineSection.getConfigurationSection("position-templates");
        if (posSection != null) {
            for (String keyStr : posSection.getKeys(false)) {
                try {
                    int pos = Integer.parseInt(keyStr);
                    String posTemplate = posSection.getString(keyStr);

                    if (!StringUtils.isBlank(posTemplate)) {
                        positionTemplates.put(pos, posTemplate);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new TopLineSettings(width, filler, template, useMinecraftWidth, minFill, positionTemplates);
    }

    private void updateData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (CachedPlaceholder cached : placeholders.values()) {
                    String rawValue = PlaceholderAPI.setPlaceholders(player, cached.getPlaceholder());
                    if (StringUtils.isBlank(rawValue)) {
                        rawValue = cached.getDefaultValue();
                    }
                    plugin.getSQLManager().updatePlayerDataField(player.getName(), cached.getAlias(), rawValue);
                }
            }
            updateTops();
        });
    }

    private void updateTops() {
        Map<String, Top> refreshed = new ConcurrentHashMap<>();

        for (CachedPlaceholder cached : placeholders.values()) {
            if (!cached.hasNumericDefault()) {
                continue;
            }

            Top top = new Top(cached.getAlias());
            try (ResultSet rs = plugin.getSQLManager().getTop10ByField(cached.getAlias())) {
                int position = 1;
                while (rs != null && rs.next() && position <= 10) {
                    String player = rs.getString("player");
                    double score = rs.getDouble("value");
                    top.addElement(new TopElement(player, score));
                    position++;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            refreshed.put(cached.getAlias(), top);
        }

        tops.clear();
        tops.putAll(refreshed);
    }

    public String resolvePlaceholder(String alias) {
        CachedPlaceholder cached = getPlaceholder(alias);
        return cached == null ? null : cached.getPlaceholder();
    }

    public String getDefault(String alias) {
        CachedPlaceholder cached = getPlaceholder(alias);
        return cached == null ? null : cached.getDefaultValue();
    }

    public String getConfiguredFormat(String alias) {
        CachedPlaceholder cached = getPlaceholder(alias);
        return cached == null ? "none" : cached.getFormat();
    }

    public CachedPlaceholder getPlaceholder(String alias) {
        if (alias == null) {
            return null;
        }
        return placeholders.get(alias.toLowerCase());
    }

    public Top getTop(String alias) {
        if (alias == null) {
            return null;
        }
        return tops.get(alias.toLowerCase());
    }

    public String getCachedValueRaw(String player, String alias) {
        if (StringUtils.isBlank(player)) {
            return null;
        }
        CachedPlaceholder cached = getPlaceholder(alias);
        if (cached == null) {
            return null;
        }

        String value = plugin.getSQLManager().getPlayerDataField(player, cached.getAlias());
        if (StringUtils.isBlank(value)) {
            return cached.getDefaultValue();
        }
        return value;
    }

    public String getCachedValueFormatted(String player, String alias, String explicitFormat, Integer precision) {
        String raw = getCachedValueRaw(player, alias);
        if (StringUtils.isBlank(raw)) {
            raw = getDefault(alias);
        }
        String format = FormatUtils.resolveFormat(explicitFormat, getConfiguredFormat(alias));
        return FormatUtils.applyFormat(raw, format, precision, !StringUtils.isBlank(explicitFormat));
    }

    public String getTopName(String alias, int position) {
        Top top = getTop(alias);
        if (top == null) {
            return null;
        }
        TopElement element = top.getTopElement(position);
        return element == null ? null : element.getPlayer();
    }

    public String getTopValueRaw(String alias, int position) {
        Top top = getTop(alias);
        if (top == null) {
            return null;
        }
        TopElement element = top.getTopElement(position);
        if (element == null) {
            return null;
        }
        return Double.toString(element.getScore());
    }

    public String getTopValueFormatted(String alias, int position, String explicitFormat, Integer precision) {
        String raw = getTopValueRaw(alias, position);
        if (raw == null) {
            return null;
        }
        String format = FormatUtils.resolveFormat(explicitFormat, getConfiguredFormat(alias));
        return FormatUtils.applyFormat(raw, format, precision, !StringUtils.isBlank(explicitFormat));
    }

    public String getFormattedTopLine(String sortAlias, int position, String explicitFormat, Integer precision) {
        return getFormattedTopLine(sortAlias, sortAlias, position, explicitFormat, precision);
    }

    public String getFormattedTopLine(String sortAlias, String valueAlias, int position, String explicitFormat, Integer precision) {
        String name = getTopName(sortAlias, position);
        if (StringUtils.isBlank(name)) {
            name = "---";
        }

        String value;
        if (sortAlias.equalsIgnoreCase(valueAlias)) {
            value = getTopValueFormatted(sortAlias, position, explicitFormat, precision);
        } else {
            value = getCachedValueFormatted(name, valueAlias, explicitFormat, precision);
        }

        if (StringUtils.isBlank(value)) {
            value = "0";
        }

        CachedPlaceholder placeholder = getPlaceholder(sortAlias);
        return FormatUtils.formatTopLine(name, value, position, placeholder == null ? null : placeholder.getTopLine());
    }
}
