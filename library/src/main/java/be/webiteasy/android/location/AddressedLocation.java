package be.webiteasy.android.location;

import android.location.Location;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class AddressedLocation extends Location {

    /**
     * Used to store
     */
    private Map<String, Component> mComponents;

    private String[] mTypes;

    public AddressedLocation(String provider, @Nullable String[] types) {
        super(provider);

        init(types);
    }

    public AddressedLocation(Location l, @Nullable String[] types) {
        super(l);

        init(types);
    }

    private void init(String[] types) {
        mTypes = types;

        if (mTypes != null) {
            mComponents = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    // alphabetical ordering
                    return lhs.compareTo(rhs);
                }
            });
        } else {
            mComponents = new LinkedHashMap<>();
        }
    }

    public boolean hasTypes() {
        return mTypes != null;
    }

    /**
     * Ensure the value and the code for a given component type
     *
     * @param type  the type of the component to update
     * @param code  the new code associated to the component type
     * @param value the new value to be associated to the component type
     */
    public void setComponent(String type, String code, String value) {
        mComponents.put(type, new Component(code, value));
    }

    public void setComponents(Map<String, Component> components) {
        LinkedHashMap<String, Component> tmp = new LinkedHashMap<>();
        tmp.putAll(components);
        mComponents.putAll(components);
    }

    public Iterator<Map.Entry<String, Component>> componentsIt() {
        return mComponents.entrySet().iterator();
    }

    public String getFlag(final int depth) {
        StringBuilder sb = new StringBuilder();

        Iterator<Map.Entry<String, Component>> it = componentsIt();
        int i = 0;
        while (it.hasNext()) {
            if (i < depth) {
                break;
            }

            Map.Entry<String, Component> component = it.next();

            sb.append(component.getValue().getCode());
            sb.append("/");

            i++;
        }

        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    public String toString() {
        return super.toString() + " Address : " + mComponents;
    }

    public static class Component {
        private String mCode;

        private String mValue;

        private Component(String code, String value) {
            mCode = code;
            mValue = value;
        }

        public String getCode() {
            return mCode;
        }

        public String getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return mValue + "(" + mCode + ")";
        }
    }
}
