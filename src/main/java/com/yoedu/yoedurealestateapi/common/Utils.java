package com.yoedu.yoedurealestateapi.common;

import java.text.Normalizer;

public final class Utils {

    private Utils() {}

    public static String generateSlug(String text) {
        if (text == null || text.isBlank()) return "";

        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "") // Remove accent marks
            .replace("đ", "d") // Vietnamese đ
            .replace("Đ", "D") // Vietnamese Đ
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-") // Replace non-alphanumeric with -
            .replaceAll("^-+|-+$", ""); // Trim leading/trailing -
    }
}
