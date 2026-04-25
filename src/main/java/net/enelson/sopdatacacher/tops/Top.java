package net.enelson.sopdatacacher.tops;

import java.util.HashMap;
import java.util.Map;

public class Top {
    private final String topType;
    private final Map<Integer, TopElement> elements;

    public Top(String topType) {
        this.topType = topType;
        this.elements = new HashMap<>();
    }

    public String getTopType() {
        return topType;
    }

    public TopElement getTopElement(int position) {
        if (position >= 1 && position <= 10 && elements.containsKey(position)) {
            return elements.get(position);
        }
        return null;
    }

    public void addElement(TopElement element) {
        this.elements.put(this.elements.size() + 1, element);
    }
}
