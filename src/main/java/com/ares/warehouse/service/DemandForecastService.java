package com.ares.warehouse.service;

import com.ares.model.Car;
import com.ares.model.CustomerOrder;
import com.ares.model.OrderLine;
import com.ares.repository.CarRepository;
import com.ares.repository.CustomerOrderRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemandForecastService {
    private static final Set<String> EXCLUDED_STATUSES = Set.of("CANCELLED", "RETURNED");
    private static final Map<String, Integer> RISK_ORDER = Map.of("OUT", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 3, "NONE", 4);
    private final CustomerOrderRepository orders;
    private final CarRepository cars;
    private final WarehouseService warehouseService;

    public DemandForecastService(CustomerOrderRepository orders, CarRepository cars,
                                 WarehouseService warehouseService) {
        this.orders = orders;
        this.cars = cars;
        this.warehouseService = warehouseService;
    }

    public record ForecastItem(Long itemId, String sku, String name, int available,
                               int soldInHistory, double avgDailyDemand, double trendPerDay,
                               int leadDemand, int horizonDemand, int safetyStock,
                               int demandReorderPoint, int thresholdReorderPoint,
                               int suggestedOrder, Double daysOfCover, LocalDate stockoutDate,
                               String risk, List<Integer> weeklySales,
                               Map<String, Integer> byWarehouse) {}

    public record ForecastReport(int days, int horizon, int leadTime, double serviceLevel,
                                 int consideredOrders, int matchedOrderLines, int unmatchedOrderLines,
                                 List<ForecastItem> items,
                                 String method) {}

    @Transactional(readOnly = true)
    public ForecastReport run(int requestedDays, int requestedHorizon, int requestedLead, double requestedServiceLevel) {
        int days = clamp(requestedDays, 14, 365, 90);
        int horizon = clamp(requestedHorizon, 7, 120, 30);
        int lead = clamp(requestedLead, 1, 60, 7);
        double serviceLevel = normalizeServiceLevel(requestedServiceLevel);
        double z = zScore(serviceLevel);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        List<WarehouseService.RestockSuggestion> restockRows = warehouseService.getRestockSuggestions();
        Map<String, WarehouseService.RestockSuggestion> inventoryBySku = new HashMap<>();
        for (WarehouseService.RestockSuggestion row : restockRows) {
            inventoryBySku.put(normalize(row.sku()), row);
        }
        Map<Long, Car> catalogCars = new HashMap<>();
        for (Car car : cars.findAll()) catalogCars.put(car.getId(), car);

        Map<Long, double[]> salesByItem = new HashMap<>();
        for (WarehouseService.RestockSuggestion row : restockRows) {
            salesByItem.put(row.itemId(), new double[days]);
        }
        LocalDate untilDate = today.plusDays(1);
        List<CustomerOrder> history = orders.findForecastableOrders(start.atStartOfDay(), untilDate.atStartOfDay(), EXCLUDED_STATUSES);
        int matchedLines = 0;
        int unmatchedLines = 0;
        for (CustomerOrder order : history) {
            int dayIndex = (int) (order.getCreatedAt().toLocalDate().toEpochDay() - start.toEpochDay());
            if (dayIndex < 0 || dayIndex >= days) continue;
            for (OrderLine line : order.getItems()) {
                Car car = catalogCars.get(line.getCarId());
                WarehouseService.RestockSuggestion inventory = car == null ? null : inventoryBySku.get(normalize(car.getCode()));
                if (inventory == null) {
                    unmatchedLines++;
                    continue;
                }
                salesByItem.get(inventory.itemId())[dayIndex] += line.getQuantity();
                matchedLines++;
            }
        }

        List<ForecastItem> forecasts = new ArrayList<>();
        for (WarehouseService.RestockSuggestion row : restockRows) {
            forecasts.add(forecast(row, salesByItem.get(row.itemId()), horizon, lead, z, today));
        }
        forecasts.sort(Comparator.comparingInt((ForecastItem row) -> RISK_ORDER.get(row.risk()))
                .thenComparing(Comparator.comparingInt(ForecastItem::suggestedOrder).reversed())
                .thenComparing(ForecastItem::name, String.CASE_INSENSITIVE_ORDER));
        return new ForecastReport(days, horizon, lead, serviceLevel, history.size(), matchedLines,
                unmatchedLines, List.copyOf(forecasts),
                "Holt có giảm chấn (α=0,3, β=0,1, φ=0,9); tồn an toàn = z·σ·√thời gian chờ. Chỉ ghép đơn hàng với SKU trùng mã xe.");
    }

    private static ForecastItem forecast(WarehouseService.RestockSuggestion inventory, double[] sales,
                                         int horizon, int lead, double z, LocalDate today) {
        double[] holt = holt(sales, 0.3, 0.1);
        double level = Math.max(0, holt[0]);
        double trend = holt[1];
        double demandLead = 0;
        double totalDemand = 0;
        double dampedTrend = 0;
        for (int day = 1; day <= horizon + lead; day++) {
            dampedTrend += Math.pow(0.9, day);
            double prediction = Math.max(0, level + trend * dampedTrend);
            if (day <= lead) demandLead += prediction;
            totalDemand += prediction;
        }
        double averageDaily = totalDemand / (horizon + lead);
        int safetyStock = (int) Math.ceil(z * holt[2] * Math.sqrt(lead));
        int demandReorderPoint = (int) Math.ceil(demandLead + safetyStock);
        int demandSuggested = Math.max(0, (int) Math.ceil(totalDemand + safetyStock - inventory.available()));
        int thresholdSuggested = Math.max(0, inventory.reorderPoint() - inventory.available());
        int suggestedOrder = Math.max(demandSuggested, thresholdSuggested);
        Double cover = averageDaily > 0.001 ? inventory.available() / averageDaily : null;
        String risk = inventory.available() <= 0 && averageDaily > 0 ? "OUT"
                : cover == null ? "NONE"
                : cover < lead ? "HIGH"
                : cover < lead + horizon / 2.0 ? "MEDIUM" : "LOW";
        LocalDate stockoutDate = cover == null ? null : today.plusDays((long) Math.floor(cover));
        return new ForecastItem(inventory.itemId(), inventory.sku(), inventory.name(), inventory.available(),
                (int) Arrays.stream(sales).sum(), round2(averageDaily), round2(trend),
                (int) Math.round(demandLead), (int) Math.round(totalDemand - demandLead), safetyStock,
                demandReorderPoint, inventory.reorderPoint(), suggestedOrder, cover, stockoutDate, risk,
                weeklySales(sales), inventory.byWarehouse());
    }

    private static double[] holt(double[] values, double alpha, double beta) {
        if (values.length == 0) return new double[]{0, 0, 0};
        double level = Arrays.stream(values, 0, Math.min(7, values.length)).average().orElse(0);
        double trend = 0;
        double squaredError = 0;
        for (double value : values) {
            double prediction = level + trend;
            squaredError += (value - prediction) * (value - prediction);
            double nextLevel = alpha * value + (1 - alpha) * (level + trend);
            trend = beta * (nextLevel - level) + (1 - beta) * trend;
            level = nextLevel;
        }
        return new double[]{level, trend, Math.sqrt(squaredError / Math.max(1, values.length))};
    }

    private static List<Integer> weeklySales(double[] dailySales) {
        int weeks = Math.min(12, Math.max(1, dailySales.length / 7));
        List<Integer> weekly = new ArrayList<>(weeks);
        int firstDay = dailySales.length - weeks * 7;
        for (int week = 0; week < weeks; week++) {
            int from = Math.max(0, firstDay + week * 7);
            int to = Math.min(dailySales.length, from + 7);
            weekly.add((int) Arrays.stream(dailySales, from, to).sum());
        }
        return List.copyOf(weekly);
    }

    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    private static double normalizeServiceLevel(double value) {
        return value == 0.9 || value == 0.95 || value == 0.99 ? value : 0.95;
    }

    private static double zScore(double serviceLevel) {
        if (serviceLevel >= 0.99) return 2.33;
        if (serviceLevel >= 0.95) return 1.65;
        return 1.28;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static double round2(double value) { return Math.round(value * 100) / 100.0; }
}
