package be.webiteasy.android.location;

public class Address implements Cloneable {

    /*@IntDef({COMPONENT_MS, COMPONENT_SEC, COMPONENT_MIN, COMPONENT_HOURS, COMPONENT_DAYS,
            COMPONENT_WEEKS, COMPONENT_MONTHS, COMPONENT_YEARS})
    public @interface Component { int SIZE = 8; }*/

    public enum Component {
        /**
         * Belgium (BE), USA (US), ...
         */
        COUNTRY(0),

        /**
         *
         */
        SUB_COUNTRY(1),

        /**
         * Illinois (IL), Saxe (SX)
         */
        ADMIN(2),

        /**
         * Bruxelles (BXL),
         */
        AGGLOMERATION(3),

        /**
         * Rixensart
         */
        LOCALITY(4),

        /**
         * Bourgeois
         */
        NEIGHBORHOOD(5),

        /**
         * Route du moulin
         */
        ROUTE(6),

        /**
         * 3b
         */
        NUMBER(7);

        private int index;

        Component(int index) {
            this.index = index;
        }
    }

    private String[] comp = new String[COMPONENTS.length];
    private String[] code = new String[COMPONENTS.length];
    private double latitude;
    private double longitude;

    public Address(double lat, double lon, String... comp) {
        latitude = lat;
        longitude = lon;
        int i = 0;
        for (String s : comp) {
            comp[i] = s;
            i++;
        }
    }

    public static Component[] COMPONENTS = {
            Component.COUNTRY,
            Component.SUB_COUNTRY,
            Component.ADMIN,
            Component.AGGLOMERATION,
            Component.LOCALITY,
            Component.NEIGHBORHOOD,
            Component.ROUTE,
            Component.NUMBER
    };


    public Address(Address address) {
        this(address.getLatitude(), address.getLongitude());

        for (Component c : Component.values()) {
            set(c, address.get(c), address.getCode(c));
        }
    }

    public String get(Component c) {
        return comp[c.index];
    }

    public String getCode(Component c) {
        return code[c.index];
    }

    public void set(Component cid, String s, String c) {
        comp[cid.index] = s;
        code[cid.index] = c;
    }


    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public Object clone() {
        Address clone = new Address(latitude, longitude);

        for (Component c : Component.values()) {
            clone.set(c, get(c), getCode(c));
        }

        return clone;
    }

    @Override
    public String toString() {
        String r = "";
        for (Component c : Component.values()) {
            r += get(c) + " (" + getCode(c) + "), ";
        }
        return r;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Address) {
            boolean equals = true;
            for (Component c : COMPONENTS) {
                if (get(c) == null && ((Address) o).get(c) != null) {
                    equals = false;
                    break;
                }

                if (!get(c).equals(((Address) o).get(c))) {
                    equals = false;
                    break;
                }
            }
            return equals;
        } else
            return super.equals(o);
    }
}
