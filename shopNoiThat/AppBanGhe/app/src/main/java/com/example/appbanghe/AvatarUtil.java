package com.example.appbanghe;

import android.content.Context;

import androidx.annotation.DrawableRes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class AvatarUtil {

    private AvatarUtil() {
    }

    public static Object resolveModel(Context context, String imgUrlOrName, String seed) {
        String v = imgUrlOrName == null ? "" : imgUrlOrName.trim();
        if (isHttpUrl(v)) {
            return v;
        }

        @DrawableRes int resId = getDrawableIdByName(context, v);
        if (resId != 0) {
            return resId;
        }

        String picked = pickFallbackAvatarName(context, seed);
        @DrawableRes int fallbackRes = getDrawableIdByName(context, picked);
        if (fallbackRes != 0) {
            return fallbackRes;
        }
        return R.drawable.avatar_default;
    }

    public static List<String> listAvatarNames(Context context) {
        if (context == null) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        try {
            Field[] fields = R.drawable.class.getFields();
            for (Field f : fields) {
                String name = f.getName();
                if (name != null && name.startsWith("avatar_")) {
                    out.add(name);
                }
            }
        } catch (Exception ignored) {
        }

        Collections.sort(out);
        return out;
    }

    public static String pickFallbackAvatarName(Context context, String seed) {
        List<String> list = listAvatarNames(context);
        if (list.isEmpty()) {
            return "avatar_default";
        }

        int idx;
        if (seed == null || seed.trim().isEmpty()) {
            idx = new Random().nextInt(list.size());
        } else {
            idx = Math.abs(seed.trim().hashCode()) % list.size();
        }
        return list.get(idx);
    }

    private static boolean isHttpUrl(String v) {
        if (v == null) return false;
        String s = v.trim().toLowerCase();
        return s.startsWith("http://") || s.startsWith("https://");
    }

    @DrawableRes
    private static int getDrawableIdByName(Context context, String name) {
        if (context == null) return 0;
        if (name == null) return 0;
        String n = name.trim();
        if (n.isEmpty()) return 0;
        return context.getResources().getIdentifier(n, "drawable", context.getPackageName());
    }
}
