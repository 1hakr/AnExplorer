package dev.dworks.apps.anexplorer.misc;

import android.graphics.Color;

public class Palette {

    public static int[] DEFAULT;

    private Palette() {

    }

    static {

        DEFAULT = new int[]{Color.parseColor("#b8c847"),
                Color.parseColor("#67bb43"), Color.parseColor("#41b691"),
                Color.parseColor("#4182b6"), Color.parseColor("#4149b6"),
                Color.parseColor("#7641b6"), Color.parseColor("#b741a7"),
                Color.parseColor("#c54657"), Color.parseColor("#d1694a"),
                Color.parseColor("#d1904a"), Color.parseColor("#d1c54a")};

    }

}