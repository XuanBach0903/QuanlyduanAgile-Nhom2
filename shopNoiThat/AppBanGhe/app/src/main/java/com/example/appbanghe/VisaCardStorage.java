package com.example.appbanghe;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public final class VisaCardStorage {

    private static final String PREFS = "visa_cards";
    private static final String KEY_CARD_1 = "card_1";
    private static final String KEY_CARD_2 = "card_2";

    private static final Gson GSON = new Gson();

    private VisaCardStorage() {
    }

    public static class VisaCard {
        public String soThe;
        public String tenChuThe;
        public int thangHetHan;
        public int namHetHan;
        public String cvv;

        public VisaCard(String soThe, String tenChuThe, int thangHetHan, int namHetHan, String cvv) {
            this.soThe = soThe;
            this.tenChuThe = tenChuThe;
            this.thangHetHan = thangHetHan;
            this.namHetHan = namHetHan;
            this.cvv = cvv;
        }

        public String masked() {
            String digits = soThe == null ? "" : soThe.replaceAll("\\s+", "");
            if (digits.length() < 4) return "****";
            String last4 = digits.substring(digits.length() - 4);
            return "**** **** **** " + last4;
        }
    }

    public static VisaCard getCard(Context ctx, int slot) {
        if (ctx == null) return null;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(slot == 2 ? KEY_CARD_2 : KEY_CARD_1, null);
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return GSON.fromJson(raw, VisaCard.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveCard(Context ctx, int slot, VisaCard card) {
        if (ctx == null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(slot == 2 ? KEY_CARD_2 : KEY_CARD_1, card == null ? null : GSON.toJson(card)).apply();
    }

    public static void clearCard(Context ctx, int slot) {
        saveCard(ctx, slot, null);
    }
}
