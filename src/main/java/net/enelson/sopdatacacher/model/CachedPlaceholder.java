package net.enelson.sopdatacacher.model;

public class CachedPlaceholder {
    private final String alias;
    private final String placeholder;
    private final String defaultValue;
    private final String format;
    private final TopLineSettings topLine;

    public CachedPlaceholder(String alias, String placeholder, String defaultValue, String format, TopLineSettings topLine) {
        this.alias = alias;
        this.placeholder = placeholder;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.format = format == null ? "none" : format.toLowerCase();
        this.topLine = topLine;
    }

    public String getAlias() {
        return alias;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getFormat() {
        return format;
    }

    public TopLineSettings getTopLine() {
        return topLine;
    }

    public boolean hasNumericDefault() {
        return defaultValue != null && defaultValue.matches("^-?\\d+(\\.\\d+)?$");
    }
}
