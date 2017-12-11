package be.webiteasy.android.location;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;

public class Location extends android.location.Location {

    private FlagEntry[] mFlags;

    public Location(String provider) {
        super(provider);
    }

    public Location(android.location.Location l) {
        super(l);
    }

    @Nullable
    public FlagEntry[] getFlags() {
        return mFlags;
    }

    public void setFlags(@Nullable FlagEntry[] flags) {
        mFlags = flags;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);

        if (mFlags == null) {
            parcel.writeStringList(null);
            parcel.writeStringList(null);
        } else {
            ArrayList<String> flagsFlags = new ArrayList<>(mFlags.length);
            ArrayList<String> flagsLabels = new ArrayList<>(mFlags.length);

            for (FlagEntry mFlag : mFlags) {
                flagsFlags.add(mFlag.flag);
                flagsLabels.add(mFlag.label);
            }

            parcel.writeStringList(flagsFlags);
            parcel.writeStringList(flagsLabels);
        }
    }

    public static final Parcelable.Creator<Location> CREATOR =
            new Parcelable.Creator<Location>() {
                @Override
                public Location createFromParcel(Parcel in) {

                    android.location.Location l = android.location.Location.CREATOR.createFromParcel(in);

                    Location ll = new Location(l);

                    final LinkedList<String> flagsFlags = new LinkedList<>();
                    in.readStringList(flagsFlags);

                    final LinkedList<String> flagsLabels = new LinkedList<>();
                    in.readStringList(flagsLabels);

                    FlagEntry[] out = new FlagEntry[flagsFlags.size()];
                    for (int i = 0; i < flagsFlags.size(); i++) {
                        out[i] = new FlagEntry(flagsFlags.get(i), flagsLabels.get(i));
                    }

                    ll.mFlags = out;

                    return ll;
                }

                @Override
                public Location[] newArray(int size) {
                    return new Location[size];
                }
            };

    public static class FlagEntry {
        public final String flag;
        public final String label;

        public FlagEntry(String flag, String label) {
            this.flag = flag;
            this.label = label;
        }
    }
}
