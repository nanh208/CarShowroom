package com.ares.service;

import com.ares.dto.ShopDtos.CartDto;
import com.ares.dto.ShopDtos.CartItemDto;
import com.ares.model.Car;
import com.ares.repository.CarRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "ares_cart";
    private final CarRepository carRepository;

    @Autowired
    public CartService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public CartDto getCart(HttpSession session) {
        CartDto cart = (CartDto) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new CartDto();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public CartDto addToCart(HttpSession session, Long carId, int quantity) {
        CartDto cart = getCart(session);
        if (carId == null || quantity <= 0) return cart;

        Optional<Car> carOpt = carRepository.findById(carId);
        if (carOpt.isEmpty()) return cart;

        Car car = carOpt.get();
        Optional<CartItemDto> existingItem = cart.getItems().stream()
                .filter(item -> item.getCarId().equals(carId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemDto item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            cart.getItems().add(new CartItemDto(car, quantity));
        }

        cart.recalculate();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public CartDto updateItemQuantity(HttpSession session, Long carId, int quantity) {
        CartDto cart = getCart(session);
        if (carId == null) return cart;

        if (quantity <= 0) {
            return removeFromCart(session, carId);
        }

        for (CartItemDto item : cart.getItems()) {
            if (item.getCarId().equals(carId)) {
                item.setQuantity(quantity);
                break;
            }
        }

        cart.recalculate();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public CartDto removeFromCart(HttpSession session, Long carId) {
        CartDto cart = getCart(session);
        if (carId == null) return cart;

        cart.getItems().removeIf(item -> item.getCarId().equals(carId));
        cart.recalculate();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public CartDto clearCart(HttpSession session) {
        CartDto cart = getCart(session);
        cart.getItems().clear();
        cart.recalculate();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }
}
