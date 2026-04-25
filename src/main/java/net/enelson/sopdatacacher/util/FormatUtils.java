package net.enelson.sopdatacacher.util;

import net.enelson.sopdatacacher.model.TopLineSettings;
import org.bukkit.ChatColor;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FormatUtils {
    private FormatUtils() {
    }

    public static String applyConfiguredFormat(String value, String configuredFormat) {
        return applyFormat(value, configuredFormat, null, false);
    }

    public static String applyExplicitFormat(String value, String format, Integer precision) {
        return applyFormat(value, format, precision, true);
    }

    public static String resolveFormat(String explicitFormat, String configuredFormat) {
        if (explicitFormat != null && !explicitFormat.isBlank()) {
            return explicitFormat.toLowerCase();
        }
        if (configuredFormat != null && !configuredFormat.isBlank()) {
            return configuredFormat.toLowerCase();
        }
        return "none";
    }

    public static String applyFormat(String value, String format, Integer precision, boolean explicitFormat) {
        if (value == null) {
            return "";
        }

        String mode = format == null ? "none" : format.toLowerCase();

        try {
            if (mode.startsWith("divide_")) {
                return applyDivideFormat(value, mode, precision, explicitFormat);
            }

            switch (mode) {
                case "int":
                    return String.valueOf((long) Double.parseDouble(value));
                case "floor":
                    return formatDecimalWithMode(Double.parseDouble(value), precision, RoundingMode.FLOOR, explicitFormat);
                case "ceil":
                    return formatDecimalWithMode(Double.parseDouble(value), precision, RoundingMode.CEILING, explicitFormat);
                case "round":
                    return formatDecimalWithMode(Double.parseDouble(value), precision, RoundingMode.HALF_UP, explicitFormat);
                case "duration":
                    return formatDurationSeconds((long) Math.floor(Double.parseDouble(value)));
                case "durationticks":
                case "duration_ticks":
                    return formatDurationSeconds((long) Math.floor(Double.parseDouble(value)) / 20L);
                case "none":
                default:
                    if (precision != null) {
                        return scaleToString(BigDecimal.valueOf(Double.parseDouble(value)), precision, RoundingMode.HALF_UP, explicitFormat);
                    }
                    return value;
            }
        } catch (Exception ignored) {
            return value;
        }
    }

    public static String formatNumericScore(double score, String format, Integer precision, boolean explicitFormat) {
        return applyFormat(BigDecimal.valueOf(score).toPlainString(), format, precision, explicitFormat);
    }

    private static String applyDivideFormat(String value, String format, Integer precision, boolean explicitFormat) {
        try {
            String[] parts = format.split("_");
            if (parts.length < 2) {
                return value;
            }

            double original = Double.parseDouble(value);
            double divisor = Double.parseDouble(parts[1]);

            if (divisor == 0.0D) {
                return value;
            }

            double divided = original / divisor;

            if (parts.length == 2) {
                if (precision != null) {
                    return scaleToString(
                            BigDecimal.valueOf(divided),
                            precision,
                            RoundingMode.HALF_UP,
                            explicitFormat
                    );
                }
                return BigDecimal.valueOf(divided).stripTrailingZeros().toPlainString();
            }

            String nestedMode = parts[2].toLowerCase();

            if (parts.length == 3) {
                switch (nestedMode) {
                    case "floor":
                        return formatDecimalWithMode(divided, precision, RoundingMode.FLOOR, explicitFormat);
                    case "ceil":
                        return formatDecimalWithMode(divided, precision, RoundingMode.CEILING, explicitFormat);
                    case "round":
                        return formatDecimalWithMode(divided, precision, RoundingMode.HALF_UP, explicitFormat);
                    case "int":
                        return String.valueOf((long) divided);
                    default:
                        if (precision != null) {
                            return scaleToString(
                                    BigDecimal.valueOf(divided),
                                    precision,
                                    RoundingMode.HALF_UP,
                                    explicitFormat
                            );
                        }
                        return BigDecimal.valueOf(divided).stripTrailingZeros().toPlainString();
                }
            }

            if (parts.length >= 4) {
                Integer nestedPrecision = tryParseInt(parts[3]);
                if (nestedPrecision == null) {
                    nestedPrecision = precision;
                }

                switch (nestedMode) {
                    case "floor":
                        return formatDecimalWithMode(divided, nestedPrecision, RoundingMode.FLOOR, explicitFormat);
                    case "ceil":
                        return formatDecimalWithMode(divided, nestedPrecision, RoundingMode.CEILING, explicitFormat);
                    case "round":
                        return formatDecimalWithMode(divided, nestedPrecision, RoundingMode.HALF_UP, explicitFormat);
                    case "int":
                        return String.valueOf((long) divided);
                    default:
                        if (nestedPrecision != null) {
                            return scaleToString(
                                    BigDecimal.valueOf(divided),
                                    nestedPrecision,
                                    RoundingMode.HALF_UP,
                                    explicitFormat
                            );
                        }
                        return BigDecimal.valueOf(divided).stripTrailingZeros().toPlainString();
                }
            }

            return BigDecimal.valueOf(divided).stripTrailingZeros().toPlainString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatDecimalWithMode(double value, Integer precision, RoundingMode mode, boolean explicitFormat) {
        if (precision == null) {
            switch (mode) {
                case FLOOR:
                    return String.valueOf((long) Math.floor(value));
                case CEILING:
                    return String.valueOf((long) Math.ceil(value));
                default:
                    return String.valueOf(Math.round(value));
            }
        }

        BigDecimal bd = BigDecimal.valueOf(value).setScale(precision, mode);
        return scaleToString(bd, precision, mode, explicitFormat);
    }

    private static String scaleToString(BigDecimal bd, int precision, RoundingMode mode, boolean explicitFormat) {
        BigDecimal scaled = bd.setScale(precision, mode);
        if (explicitFormat) {
            return scaled.toPlainString();
        }
        return scaled.stripTrailingZeros().toPlainString();
    }

    public static String formatDurationSeconds(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        long weeks = seconds / 604800L;
        seconds %= 604800L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String formatTopLine(String name, String value, int position, TopLineSettings settings) {
        if (settings == null) {
            return position + ". " + (name == null ? "-" : name) + " ... " + (value == null ? "-" : value);
        }

        String safeName = name == null || name.isEmpty() ? "-" : name;
        String safeValue = value == null || value.isEmpty() ? "-" : value;
        String template = settings.getTemplateForPosition(position);
        String fill = buildFill(safeName, safeValue, position, settings, template);

        return template
                .replace("{name}", safeName)
                .replace("{value}", safeValue)
                .replace("{fill}", fill)
                .replace("{pos}", String.valueOf(position));
    }

    private static String buildFill(String name, String value, int position, TopLineSettings settings, String template) {
        int targetWidth = settings.getWidth();
        int minFill = Math.max(1, settings.getMinFill());
        String filler = settings.getFiller();

        if (filler == null || filler.isEmpty()) {
            filler = ".";
        }

        String templateWithoutFill = template
                .replace("{name}", name)
                .replace("{value}", value)
                .replace("{position}", String.valueOf(position))
                .replace("{pos}", String.valueOf(position))
                .replace("{fill}", "");

        int currentWidth = settings.isUseMinecraftWidth()
                ? getMinecraftTextWidth(templateWithoutFill)
                : visibleLength(templateWithoutFill);

        int fillerUnitWidth = settings.isUseMinecraftWidth()
                ? getMinecraftTextWidth(filler)
                : visibleLength(filler);

        if (fillerUnitWidth <= 0) {
            fillerUnitWidth = 1;
        }

        int required = targetWidth - currentWidth;
        int fillerCount = (int) Math.ceil((double) required / fillerUnitWidth);

        if (fillerCount < minFill) {
            fillerCount = minFill;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fillerCount; i++) {
            sb.append(filler);
        }
        return sb.toString();
    }

    public static int visibleLength(String text) {
        if (text == null) {
            return 0;
        }
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
        return stripped == null ? 0 : stripped.length();
    }

    public static int getMinecraftTextWidth(String text) {
        if (text == null) {
            return 0;
        }

        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
        if (stripped == null) {
            return 0;
        }

        int width = 0;
        for (char c : stripped.toCharArray()) {
            width += getCharWidth(c);
        }
        return width;
    }

    private static int getCharWidth(char c) {
        switch (c) {
            case 'i':
            case 'l':
            case '!':
            case '.':
            case ',':
            case ':':
            case ';':
            case '|':
            case '\'':
                return 2;
            case ' ':
                return 4;
            case 't':
            case 'I':
            case '[':
            case ']':
            case '(':
            case ')':
                return 4;
            case 'f':
            case 'k':
            case '{':
            case '}':
            case '<':
            case '>':
            case '*':
            case '"':
                return 5;
            default:
                return 6;
        }
    }
}
