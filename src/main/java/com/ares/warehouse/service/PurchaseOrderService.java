package com.ares.warehouse.service;

import com.ares.warehouse.model.InventoryItem;
import com.ares.warehouse.model.PurchaseOrder;
import com.ares.warehouse.model.PurchaseOrderLine;
import com.ares.warehouse.model.PurchaseOrderStatus;
import com.ares.warehouse.model.StockMovement;
import com.ares.warehouse.model.Warehouse;
import com.ares.warehouse.model.WarehouseStock;
import com.ares.warehouse.repository.InventoryItemRepository;
import com.ares.warehouse.repository.PurchaseOrderRepository;
import com.ares.warehouse.repository.StockMovementRepository;
import com.ares.warehouse.repository.WarehouseRepository;
import com.ares.warehouse.repository.WarehouseStockRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {
    private final PurchaseOrderRepository orders;
    private final InventoryItemRepository items;
    private final WarehouseRepository warehouses;
    private final WarehouseStockRepository stocks;
    private final StockMovementRepository movements;

    public PurchaseOrderService(PurchaseOrderRepository orders, InventoryItemRepository items,
                                WarehouseRepository warehouses, WarehouseStockRepository stocks,
                                StockMovementRepository movements) {
        this.orders = orders;
        this.items = items;
        this.warehouses = warehouses;
        this.stocks = stocks;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getOrders() { return orders.findAllByOrderByCreatedAtDesc(); }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems() { return items.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehouses() { return warehouses.findAllByOrderByNameAsc(); }

    @Transactional
    public PurchaseOrder create(String supplier, Long warehouseId, String note,
                                List<Long> itemIds, List<Integer> quantities, List<Long> unitCosts) {
        String cleanSupplier = required(supplier, "Nhà cung cấp");
        if (cleanSupplier.length() > 160) throw new IllegalArgumentException("Tên nhà cung cấp tối đa 160 ký tự.");
        if (itemIds == null || itemIds.isEmpty() || itemIds.size() > 50
                || quantities == null || unitCosts == null
                || itemIds.size() != quantities.size() || itemIds.size() != unitCosts.size()) {
            throw new IllegalArgumentException("Đơn nhập cần từ 1 đến 50 dòng hàng hợp lệ.");
        }
        Warehouse warehouse = warehouses.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận hàng."));
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > 500) {
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        }

        long totalCost = 0;
        Set<Long> distinctItemIds = new HashSet<>();
        try {
            for (int index = 0; index < itemIds.size(); index++) {
                Long itemId = itemIds.get(index);
                Integer quantity = quantities.get(index);
                Long unitCost = unitCosts.get(index);
                if (itemId == null || quantity == null || quantity < 1 || quantity > 1_000_000
                        || unitCost == null || unitCost < 0 || unitCost > 1_000_000_000_000L) {
                    throw new IllegalArgumentException("Số lượng phải từ 1 đến 1.000.000; đơn giá không được âm.");
                }
                if (!distinctItemIds.add(itemId)) {
                    throw new IllegalArgumentException("Mỗi mặt hàng chỉ được xuất hiện một lần trong đơn.");
                }
                totalCost = Math.addExact(totalCost, Math.multiplyExact(quantity.longValue(), unitCost));
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Tổng tiền đơn nhập vượt giới hạn cho phép.");
        }

        PurchaseOrder order = new PurchaseOrder(nextCode(), cleanSupplier, warehouse, totalCost, cleanNote);
        for (int index = 0; index < itemIds.size(); index++) {
            InventoryItem item = items.findById(itemIds.get(index))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mặt hàng trong đơn."));
            order.addLine(new PurchaseOrderLine(item, quantities.get(index), unitCosts.get(index)));
        }
        return orders.save(order);
    }

    @Transactional
    public void markOrdered(Long orderId) {
        PurchaseOrder order = locked(orderId);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ đơn nháp mới có thể chuyển sang đã đặt.");
        }
        order.markOrdered();
    }

    @Transactional
    public void cancel(Long orderId) {
        PurchaseOrder order = locked(orderId);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT && order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new IllegalArgumentException("Chỉ đơn nháp hoặc đã đặt mới có thể hủy.");
        }
        order.cancel();
    }

    @Transactional
    public void receive(Long orderId) {
        PurchaseOrder order = locked(orderId);
        if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new IllegalArgumentException("Chỉ đơn đã đặt mới có thể nhận hàng.");
        }
        Warehouse lockedWarehouse = warehouses.findByIdForUpdate(order.getWarehouse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận hàng."));
        String note = "Nhận theo " + order.getCode() + " · " + order.getSupplier();
        for (PurchaseOrderLine line : order.getLines()) {
            WarehouseStock stock = stocks.findByWarehouseIdAndItemId(lockedWarehouse.getId(), line.getItem().getId())
                    .orElseGet(() -> new WarehouseStock(lockedWarehouse, line.getItem(), 0));
            final int next;
            try {
                next = Math.addExact(stock.getQuantity(), line.getQuantity());
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Số lượng tồn vượt giới hạn cho SKU " + line.getItem().getSku() + ".");
            }
            stock.setQuantity(next);
            stocks.save(stock);
            movements.save(new StockMovement(line.getItem(), null, lockedWarehouse,
                    "PURCHASE_IN", line.getQuantity(), note));
        }
        order.markReceived();
    }

    private PurchaseOrder locked(Long id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn nhập."));
    }

    private String nextCode() {
        String code;
        do {
            code = "PO-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
        } while (orders.existsByCode(code));
        return code;
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được để trống.");
        return value.trim();
    }
}
