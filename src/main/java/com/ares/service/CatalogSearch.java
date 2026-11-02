package com.ares.service;

import com.ares.model.Car;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Search rules adapted from the source SearchService to the existing Car model. */
public final class CatalogSearch {
    private CatalogSearch() {}
    private static final Pattern SCALE = Pattern.compile("\\b1\\s*[:/]\\s*(\\d{1,3})\\b");
    private static final Pattern PRICE = Pattern.compile("\\b(duoi|tren|toi da|tu)\\s+(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|k)\\b");
    private static final Set<String> STOP = Set.of("xe", "mo", "hinh", "tim", "mua", "cho", "toi", "ty", "ti", "le");
    private static final Map<String, String> SYNONYMS = Map.of(
        "lambo", "lamborghini", "merc", "mercedes", "benz", "mercedes",
        "dieu khien", "rc", "remote", "rc", "pin", "batteries");

    public record Parsed(String scale, Long minPrice, Long maxPrice, List<String> terms) {}

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').trim().replaceAll("\\s+", " ");
    }

    public static Parsed parse(String query) {
        String text = normalize(query);
        var scaleMatch = SCALE.matcher(text);
        String scale = scaleMatch.find() ? "1:" + scaleMatch.group(1) : null;
        text = scaleMatch.replaceAll(" ");
        var prices = PRICE.matcher(text);
        Long min = null, max = null;
        while (prices.find()) {
            long amount;
            try {
                amount = new java.math.BigDecimal(prices.group(2).replace(',', '.'))
                    .multiply(java.math.BigDecimal.valueOf(prices.group(3).equals("k") ? 1000 : 1000000))
                    .longValueExact();
            } catch (ArithmeticException error) {
                throw new IllegalArgumentException("Mức giá trong từ khóa không hợp lệ");
            }
            if (prices.group(1).equals("duoi") || prices.group(1).equals("toi da")) max = amount;
            else min = amount;
        }
        text = " " + prices.replaceAll(" ").trim() + " ";
        for (var entry : SYNONYMS.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed()).toList()) {
            text = text.replace(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
        }
        List<String> terms = Arrays.stream(text.trim().split("\\s+"))
            .filter(term -> !term.isBlank() && !STOP.contains(term)).distinct().toList();
        return new Parsed(scale, min, max, terms);
    }

    public static boolean matches(Car car, Parsed query) {
        if (query.scale() != null && !query.scale().equals(scale(car.getScale()))) return false;
        if (query.minPrice() != null && (car.getPrice() == null || car.getPrice() < query.minPrice())) return false;
        if (query.maxPrice() != null && (car.getPrice() == null || car.getPrice() > query.maxPrice())) return false;
        String hay = normalize(String.join(" ", Objects.toString(car.getName(), ""),
            Objects.toString(car.getCode(), ""), Objects.toString(car.getCategory(), "").replace('_', ' '),
            Objects.toString(car.getMaterial(), ""), Objects.toString(car.getFeatures(), ""), Objects.toString(car.getDescription(), "")));
        var words = hay.split("[^a-z0-9-]+");
        return query.terms().stream().allMatch(term -> hay.contains(term)
            || (term.length() >= 4 && Arrays.stream(words).anyMatch(word -> oneEdit(term, word))));
    }

    public static String scale(String value) {
        var match = SCALE.matcher(normalize(value));
        return match.find() ? "1:" + match.group(1) : normalize(value);
    }

    static boolean oneEdit(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;
        int i = 0, j = 0, edits = 0;
        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) == b.charAt(j)) { i++; j++; continue; }
            if (++edits > 1) return false;
            if (a.length() >= b.length()) i++;
            if (b.length() >= a.length()) j++;
        }
        return edits + a.length() - i + b.length() - j <= 1;
    }
}
