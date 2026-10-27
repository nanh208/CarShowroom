package com.ares.transport.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Reference GHN/GHTK rates adapted from the source project's sandbox estimator. */
@Service
public class CarrierQuoteService {
    private static final List<String> INNER_CITIES = List.of("TP. Hồ Chí Minh", "Hà Nội");
    private static final List<String> NEAR_CITIES = List.of("Bình Dương", "Đồng Nai", "Long An",
            "Bà Rịa - Vũng Tàu", "Hải Phòng", "Bắc Ninh", "Hưng Yên", "Cần Thơ", "Đà Nẵng");

    public record Quote(String carrier, String service, String label, long fee, long codFee,
                        int weightGrams, LocalDate etaFrom, LocalDate etaTo) {}

    public List<Quote> quoteAll(String city, int weightGrams, long codAmount) {
        if (weightGrams < 50 || weightGrams > 30_000) {
            throw new IllegalArgumentException("Khối lượng kiện phải từ 50 g đến 30 kg.");
        }
        if (codAmount < 0 || codAmount > 1_000_000_000L) {
            throw new IllegalArgumentException("Tiền thu hộ không hợp lệ.");
        }
        List<Quote> quotes = new ArrayList<>();
        for (String carrier : List.of("GHN", "GHTK")) {
            quotes.add(quote(carrier, "STANDARD", city, weightGrams, codAmount));
            quotes.add(quote(carrier, "EXPRESS", city, weightGrams, codAmount));
        }
        quotes.sort(Comparator.comparingLong(Quote::fee));
        return quotes;
    }

    private Quote quote(String carrier, String service, String city, int weightGrams, long codAmount) {
        int zone = zone(city);
        boolean express = "EXPRESS".equals(service);
        boolean ghtk = "GHTK".equals(carrier);
        long base = ghtk ? new long[]{16_500, 25_000, 32_000}[zone]
                : new long[]{18_500, 27_000, 34_000}[zone];
        long step = ghtk ? new long[]{2_500, 5_000, 5_000}[zone]
                : new long[]{2_500, 4_000, 5_000}[zone];
        int days = ghtk ? new int[]{1, 2, 3}[zone] : new int[]{1, 2, 2}[zone];
        long weightSurcharge = weightGrams > 500
                ? (long) Math.ceil((weightGrams - 500) / 500.0) * step : 0;
        long fee = base + weightSurcharge;
        if (express) {
            fee = Math.round(fee * 1.6 / 500) * 500 + 10_000;
            days = Math.max(1, days - 1);
        }
        fee = Math.round(fee / 500.0) * 500;

        long codFee = codAmount > 0
                ? Math.round(codAmount * (ghtk ? 0.005 : 0.004) / 500) * 500 : 0;
        if (ghtk && codAmount <= 3_000_000) codFee = 0;

        LocalDate etaFrom = LocalDate.now().plusDays(days);
        String carrierName = ghtk ? "Giao Hàng Tiết Kiệm" : "Giao Hàng Nhanh";
        String label = carrierName + (express ? " · Hoả tốc" : " · Tiêu chuẩn");
        return new Quote(carrier, service, label, fee, codFee, weightGrams,
                etaFrom, etaFrom.plusDays(express ? 1 : 2));
    }

    private static int zone(String city) {
        if (city == null || city.isBlank()) return 2;
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        if (INNER_CITIES.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).equals(normalized))) return 0;
        if (NEAR_CITIES.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).equals(normalized))) return 1;
        return 2;
    }
}
