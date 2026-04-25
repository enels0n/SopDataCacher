package net.enelson.sopdatacacher.model;

import java.util.Map;

public class TopLineSettings {
    private final int width;
    private final String filler;
    private final String template;
    private final boolean useMinecraftWidth;
    private final int minFill;
    private final Map<Integer, String> positionTemplates;

    public TopLineSettings(
            int width,
            String filler,
            String template,
            boolean useMinecraftWidth,
            int minFill,
            Map<Integer, String> positionTemplates
    ) {
        this.width = width;
        this.filler = filler;
        this.template = template;
        this.useMinecraftWidth = useMinecraftWidth;
        this.minFill = minFill;
        this.positionTemplates = positionTemplates;
    }

    public int getWidth() {
        return width;
    }

    public String getFiller() {
        return filler;
    }

    public String getTemplate() {
        return template;
    }

    public boolean isUseMinecraftWidth() {
        return useMinecraftWidth;
    }

    public int getMinFill() {
        return minFill;
    }

    public Map<Integer, String> getPositionTemplates() {
        return positionTemplates;
    }

    public String getTemplateForPosition(int position) {
        if (positionTemplates == null) {
            return template;
        }
        return positionTemplates.getOrDefault(position, template);
    }
}
