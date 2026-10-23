package com.ares.dto;

import com.ares.model.Car;
import java.util.ArrayList;
import java.util.List;

public class ShopDtos {

    public static class CategoryDto {
        private String code;
        private String name;
        private String icon;
        private int count;

        public CategoryDto() {}
        public CategoryDto(String code, String name, String icon, int count) {
            this.code = code;
            this.name = name;
            this.icon = icon;
            this.count = count;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class ReviewDto {
        private Long id;
        private String userTitle;
        private int rating;
        private String comment;
        private String boughtInfo;
        private String photoUrl;

        public ReviewDto() {}
        public ReviewDto(Long id, String userTitle, int rating, String comment, String boughtInfo, String photoUrl) {
            this.id = id;
            this.userTitle = userTitle;
            this.rating = rating;
            this.comment = comment;
            this.boughtInfo = boughtInfo;
            this.photoUrl = photoUrl;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUserTitle() { return userTitle; }
        public void setUserTitle(String userTitle) { this.userTitle = userTitle; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getBoughtInfo() { return boughtInfo; }
        public void setBoughtInfo(String boughtInfo) { this.boughtInfo = boughtInfo; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    }

    public static class CartItemDto {
        private Long carId;
        private String carName;
        private String carImage;
        private Long unitPrice;
        private String formattedUnitPrice;
        private int quantity;
        private Long subtotal;
        private String formattedSubtotal;

        public CartItemDto() {}
        public CartItemDto(Car car, int quantity) {
            this.carId = car.getId();
            this.carName = car.getName();
            this.carImage = car.getImage();
            this.unitPrice = car.getPrice();
            this.formattedUnitPrice = car.getFormattedPrice();
            this.quantity = quantity;
            this.subtotal = car.getPrice() * quantity;
            this.formattedSubtotal = String.format("%,d₫", this.subtotal).replace(',', '.');
        }

        public Long getCarId() { return carId; }
        public void setCarId(Long carId) { this.carId = carId; }
        public String getCarName() { return carName; }
        public void setCarName(String carName) { this.carName = carName; }
        public String getCarImage() { return carImage; }
        public void setCarImage(String carImage) { this.carImage = carImage; }
        public Long getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
        public String getFormattedUnitPrice() { return formattedUnitPrice; }
        public void setFormattedUnitPrice(String formattedUnitPrice) { this.formattedUnitPrice = formattedUnitPrice; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
            if (this.unitPrice != null) {
                this.subtotal = this.unitPrice * quantity;
                this.formattedSubtotal = String.format("%,d₫", this.subtotal).replace(',', '.');
            }
        }
        public Long getSubtotal() { return subtotal; }
        public void setSubtotal(Long subtotal) { this.subtotal = subtotal; }
        public String getFormattedSubtotal() { return formattedSubtotal; }
        public void setFormattedSubtotal(String formattedSubtotal) { this.formattedSubtotal = formattedSubtotal; }
    }

    public static class CartDto {
        private List<CartItemDto> items = new ArrayList<>();
        private int totalQuantity = 0;
        private Long totalPrice = 0L;
        private String formattedTotalPrice = "0₫";

        public CartDto() {}

        public void recalculate() {
            totalQuantity = 0;
            totalPrice = 0L;
            for (CartItemDto item : items) {
                totalQuantity += item.getQuantity();
                totalPrice += item.getSubtotal();
            }
            formattedTotalPrice = String.format("%,d₫", totalPrice).replace(',', '.');
        }

        public List<CartItemDto> getItems() { return items; }
        public void setItems(List<CartItemDto> items) { this.items = items; }
        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
        public Long getTotalPrice() { return totalPrice; }
        public void setTotalPrice(Long totalPrice) { this.totalPrice = totalPrice; }
        public String getFormattedTotalPrice() { return formattedTotalPrice; }
        public void setFormattedTotalPrice(String formattedTotalPrice) { this.formattedTotalPrice = formattedTotalPrice; }
    }

    public static class AddToCartRequest {
        private Long carId;
        private int quantity = 1;

        public Long getCarId() { return carId; }
        public void setCarId(Long carId) { this.carId = carId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
