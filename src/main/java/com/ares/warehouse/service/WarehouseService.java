package com.ares.warehouse.service;

import com.ares.warehouse.model.InventoryItem;
import com.ares.warehouse.model.StockMovement;
import com.ares.warehouse.model.Warehouse;
import com.ares.warehouse.model.WarehouseStock;
import com.ares.warehouse.repository.InventoryItemRepository;
import com.ares.warehouse.repository.StockMovementRepository;
import com.ares.warehouse.repository.WarehouseRepository;
import com.ares.warehouse.repository.WarehouseStockRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {
    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final WarehouseStockRepository stocks;
    private final StockMovementRepository movements;

    public WarehouseService(WarehouseRepository warehouses, InventoryItemRepository items,
                            WarehouseStockRepository stocks, StockMovementRepository movements) {
        this.warehouses = warehouses;
        this.items = items;
        this.stocks = stocks;
        this.movements = movements;
    }

    @Bean
    CommandLineRunner seedWarehouses(WarehouseRepository repository) {
        return args -> {
            seed(repository, "HCM", "Kho Tân Bình", "TP. Hồ Chí Minh");
            seed(repository, "HN", "Kho Cầu Giấy", "Hà Nội");
            seed(repository, "DN", "Kho Hải Châu", "Đà Nẵng");
        };
    }

    private void seed(WarehouseRepository repository, String code, String name, String location) {
        if (repository.findByCode(code).isEmpty()) repository.save(new Warehouse(code, name, location));
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehouses() { return warehouses.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems() { return items.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public List<WarehouseStock> getStocks() { return stocks.findAllByOrderByItem_NameAsc(); }

    @Transactional(readOnly = true)
    public List<StockMovement> getRecentMovements() { return movements.findTop100ByOrderByCreatedAtDesc(); }

    @Transactional
    public void addItem(String sku, String name, String category) {
        String cleanSku = required(sku, "SKU").toUpperCase();
        String cleanName = required(name, "Tên mặt hàng");
        if (items.findBySkuIgnoreCase(cleanSku).isPresent()) {
            throw new IllegalArgumentException("SKU này đã tồn tại.");
        }
        items.save(new InventoryItem(cleanSku, cleanName, clean(category)));
    }

    @Transactional
    public void adjust(Long itemId, Long warehouseId, int delta, String note) {
        if (delta == 0) throw new IllegalArgumentException("Số lượng điều chỉnh không được bằng 0.");
        InventoryItem item = items.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mặt hàng."));
        Warehouse warehouse = warehouses.findById(warehouseId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho."));
        WarehouseStock stock = stocks.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElseGet(() -> new WarehouseStock(warehouse, item, 0));
        int next = stock.getQuantity() + delta;
        if (next < 0) throw new IllegalArgumentException("Tồn kho không đủ để giảm số lượng này.");
        stock.setQuantity(next);
        stocks.save(stock);
        movements.save(new StockMovement(item, delta < 0 ? warehouse : null,
                delta > 0 ? warehouse : null, delta < 0 ? "ADJUSTMENT_OUT" : "ADJUSTMENT_IN",
                Math.abs(delta), clean(note)));
    }

    @Transactional
    public void transfer(Long itemId, Long fromId, Long toId, int quantity, String note) {
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng điều chuyển phải lớn hơn 0.");
        if (fromId.equals(toId)) throw new IllegalArgumentException("Kho đi và kho đến phải khác nhau.");
        InventoryItem item = items.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mặt hàng."));
        Warehouse from = warehouses.findById(fromId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho xuất."));
        Warehouse to = warehouses.findById(toId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận."));
        WarehouseStock fromStock = stocks.findByWarehouseIdAndItemId(fromId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Kho xuất chưa có tồn mặt hàng này."));
        if (fromStock.getQuantity() < quantity) throw new IllegalArgumentException("Tồn kho tại kho xuất không đủ.");
        WarehouseStock toStock = stocks.findByWarehouseIdAndItemId(toId, itemId)
                .orElseGet(() -> new WarehouseStock(to, item, 0));
        fromStock.setQuantity(fromStock.getQuantity() - quantity);
        toStock.setQuantity(toStock.getQuantity() + quantity);
        stocks.save(fromStock);
        stocks.save(toStock);
        movements.save(new StockMovement(item, from, to, "TRANSFER", quantity, clean(note)));
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được để trống.");
        return value.trim();
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
