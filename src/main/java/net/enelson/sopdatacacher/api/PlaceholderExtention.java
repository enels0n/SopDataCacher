package net.enelson.sopdatacacher.api;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.sopdatacacher.SopDataCacher;
import net.enelson.sopdatacacher.data.DataManager;
import net.enelson.sopdatacacher.util.StringUtils;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderExtention extends PlaceholderExpansion {
    private static final Pattern DIRECT_PATTERN = Pattern.compile("\\{(?<player>[^}]+)\\}_\\{(?<placeholder>[^}]+)\\}(?:_(?<format>[a-z_]+?)(?:_(?<precision>\\d+))?)?$");
    private static final Pattern NESTED_PLAYER_PATTERN = Pattern.compile("\\{##(?<playerExpr>.+?)##\\}_\\{(?<placeholder>[^}]+)\\}(?:_(?<format>[a-z_]+?)(?:_(?<precision>\\d+))?)?$");
    private static final Pattern TOP_PATTERN = Pattern.compile("top_\\{(?<placeholder>[^}]+)\\}_(?<pos>\\d+)_(?<field>name|value)(?:_(?<format>[a-z_]+?)(?:_(?<precision>\\d+))?)?$");
    private static final Pattern TOP_LINE_DUAL_PATTERN = Pattern.compile("topline_\\{(?<sortAlias>[^}]+)\\}_\\{(?<valueAlias>[^}]+)\\}_(?<pos>\\d+)(?:_(?<format>[a-z_]+?)(?:_(?<precision>\\d+))?)?$");
    private static final Pattern TOP_LINE_PATTERN = Pattern.compile("topline_\\{(?<placeholder>[^}]+)\\}_(?<pos>\\d+)(?:_(?<format>[a-z_]+?)(?:_(?<precision>\\d+))?)?$");
    private final String identifier;

    public PlaceholderExtention(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "E.NeLsOn";
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getPlugin() {
        return SopDataCacher.getInstance().getName();
    }

    @Override
    public String getVersion() {
        return "2.2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        DataManager dataManager = SopDataCacher.getInstance().getDataManager();

        Matcher topLineDualMatcher = TOP_LINE_DUAL_PATTERN.matcher(identifier);
        if (topLineDualMatcher.matches()) {
            String sortAlias = topLineDualMatcher.group("sortAlias").trim().toLowerCase();
            String valueAlias = topLineDualMatcher.group("valueAlias").trim().toLowerCase();
            int position = Integer.parseInt(topLineDualMatcher.group("pos"));
            String format = groupOrNull(topLineDualMatcher, "format");
            Integer precision = parsePrecision(topLineDualMatcher.group("precision"));
            return dataManager.getFormattedTopLine(sortAlias, valueAlias, position, format, precision);
        }

        Matcher topLineMatcher = TOP_LINE_PATTERN.matcher(identifier);
        if (topLineMatcher.matches()) {
            String alias = topLineMatcher.group("placeholder").trim().toLowerCase();
            int position = Integer.parseInt(topLineMatcher.group("pos"));
            String format = groupOrNull(topLineMatcher, "format");
            Integer precision = parsePrecision(topLineMatcher.group("precision"));
            return dataManager.getFormattedTopLine(alias, position, format, precision);
        }

        Matcher topMatcher = TOP_PATTERN.matcher(identifier);
        if (topMatcher.matches()) {
            String alias = topMatcher.group("placeholder").trim().toLowerCase();
            int pos = Integer.parseInt(topMatcher.group("pos"));
            String field = topMatcher.group("field");
            String format = groupOrNull(topMatcher, "format");
            Integer precision = parsePrecision(topMatcher.group("precision"));
            return resolveTopField(dataManager, alias, pos, field, format, precision);
        }

        Matcher directMatcher = DIRECT_PATTERN.matcher(identifier);
        if (directMatcher.matches()) {
            String playerExpr = directMatcher.group("player").trim();
            String alias = directMatcher.group("placeholder").trim().toLowerCase();
            String format = groupOrNull(directMatcher, "format");
            Integer precision = parsePrecision(directMatcher.group("precision"));
            String resolvedPlayer = PlaceholderAPI.setPlaceholders(null, "%" + playerExpr + "%");
            return resolveValue(dataManager, resolvedPlayer, alias, format, precision);
        }

        Matcher nestedMatcher = NESTED_PLAYER_PATTERN.matcher(identifier);
        if (nestedMatcher.matches()) {
            String playerExpr = nestedMatcher.group("playerExpr").trim();
            String resolvedPlayer = PlaceholderAPI.setPlaceholders(null, "%" + playerExpr + "%");
            String alias = nestedMatcher.group("placeholder").trim().toLowerCase();
            String format = groupOrNull(nestedMatcher, "format");
            Integer precision = parsePrecision(nestedMatcher.group("precision"));
            return resolveValue(dataManager, resolvedPlayer, alias, format, precision);
        }

        return "null";
    }

    private String resolveTopField(DataManager dataManager, String alias, int pos, String field, String explicitFormat, Integer precision) {
        if ("name".equalsIgnoreCase(field)) {
            String name = dataManager.getTopName(alias, pos);
            return name == null ? "-" : name;
        }

        if ("value".equalsIgnoreCase(field)) {
            String value = dataManager.getTopValueFormatted(alias, pos, explicitFormat, precision);
            return value == null ? "-" : value;
        }

        return "-";
    }

    private String resolveValue(DataManager dataManager, String player, String alias, String explicitFormat, Integer precision) {
        if (StringUtils.isBlank(player)) {
            return "null";
        }

        if (dataManager.getPlaceholder(alias) == null) {
            return "null";
        }

        String value = dataManager.getCachedValueFormatted(player, alias, explicitFormat, precision);
        if (value != null) {
            return value;
        }

        String def = dataManager.getDefault(alias);
        return def == null ? "null" : def;
    }

    private static String groupOrNull(Matcher matcher, String group) {
        String value = matcher.group(group);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.toLowerCase();
    }

    private static Integer parsePrecision(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
