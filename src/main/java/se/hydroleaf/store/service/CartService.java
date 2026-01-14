package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.api.dto.CartCreateRequest;
import se.hydroleaf.store.api.dto.CartItemRequest;
import se.hydroleaf.store.api.dto.CartResponse;
import se.hydroleaf.store.api.dto.MoneySummary;
import se.hydroleaf.store.api.dto.UpdateCartItemRequest;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.CartStatus;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.repository.CartItemRepository;
import se.hydroleaf.store.repository.CartRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreMapper storeMapper;
    private final StoreProperties storeProperties;
    private final ProductService productService;

    @Transactional
    public CartResponse createCart(CartCreateRequest request) {
        String sessionId = Optional.ofNullable(request.getSessionId()).filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        Cart cart = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.OPEN)
                .map(existing -> {
                    if (existing.getUserId() == null && request.getUserId() != null) {
                        existing.setUserId(request.getUserId());
                    }
                    return existing;
                })
                .orElseGet(() -> Cart.builder()
                        .sessionId(sessionId)
                        .userId(request.getUserId())
                        .status(CartStatus.OPEN)
                        .build());

        cart = cartRepository.save(cart);
        MoneySummary totals = refreshPricing(cart);
        log.info("Cart created/retrieved cartId={} sessionId={}", cart.getId(), cart.getSessionId());
        return storeMapper.toCartResponse(cart, totals);
    }

    @Transactional
    public CartResponse addItem(UUID cartId, CartItemRequest request) {
        Cart cart = requireOpenCart(cartId);
        ProductVariant variant = productService.requireActiveVariant(request.getVariantId());
        Product product = variant.getProduct();
        int qtyToAdd = request.getQty();

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getVariant() != null && i.getVariant().getId().equals(variant.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setProduct(product);
                    created.setVariant(variant);
                    cart.getItems().add(created);
                    return created;
                });

        int newQty = item.getId() == null ? qtyToAdd : item.getQty() + qtyToAdd;
        applyQuantity(item, variant, newQty);
        cartRepository.save(cart);

        MoneySummary totals = refreshPricing(cart);
        log.info("Added item to cart cartId={} variantId={} qty={}", cart.getId(), variant.getId(), newQty);
        return storeMapper.toCartResponse(cart, totals);
    }

    @Transactional
    public CartResponse updateItem(UUID cartId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = requireOpenCart(cartId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));

        ProductVariant variant = productService.requireActiveVariant(item.getVariant().getId());
        applyQuantity(item, variant, request.getQty());
        cartRepository.save(cart);

        MoneySummary totals = refreshPricing(cart);
        log.info("Updated cart item cartId={} itemId={} qty={}", cart.getId(), item.getId(), request.getQty());
        return storeMapper.toCartResponse(cart, totals);
    }

    @Transactional
    public CartResponse removeItem(UUID cartId, UUID itemId) {
        Cart cart = requireOpenCart(cartId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));
        cart.getItems().remove(item);
        cartRepository.save(cart);

        MoneySummary totals = refreshPricing(cart);
        log.info("Removed cart item cartId={} itemId={}", cart.getId(), item.getId());
        return storeMapper.toCartResponse(cart, totals);
    }

    @Transactional
    public CartResponse getCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        MoneySummary totals = refreshPricing(cart);
        return storeMapper.toCartResponse(cart, totals);
    }

    @Transactional
    public CartCheckoutSnapshot loadCartForCheckout(UUID cartId) {
        Cart cart = requireOpenCart(cartId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("EMPTY_CART", "Cart has no items");
        }
        MoneySummary totals = refreshPricing(cart);
        return new CartCheckoutSnapshot(cart, totals);
    }

    private void applyQuantity(CartItem item, ProductVariant variant, int qty) {
        if (qty < 1) {
            throw new BadRequestException("INVALID_QTY", "Quantity must be at least 1");
        }
        int availableQty = variant.getStockQuantity();
        if (availableQty < 1) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Variant is out of stock");
        }
        if (qty > availableQty) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Not enough stock for selected variant");
        }
        item.setProduct(variant.getProduct());
        item.setVariant(variant);
        item.setQty(qty);
        item.setUnitPriceCents(variant.getPriceCents());
        item.setLineTotalCents(Math.multiplyExact(variant.getPriceCents(), (long) qty));
    }

    private Cart requireOpenCart(UUID cartId) {
        Cart cart = cartRepository.findLockedWithItems(cartId)
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new ConflictException("CART_CLOSED", "Cart is no longer open");
        }
        return cart;
    }

    private MoneySummary refreshPricing(Cart cart) {
        long subtotal = 0L;
        for (CartItem item : cart.getItems()) {
            if (item.getVariant() == null) {
                throw new NotFoundException("VARIANT_NOT_FOUND", "Product variant not available");
            }
            ProductVariant current = productService.requireActiveVariant(item.getVariant().getId());
            int availableQty = current.getStockQuantity();
            if (availableQty < 1) {
                throw new ConflictException("INSUFFICIENT_STOCK", "Item " + current.getLabel() + " is out of stock");
            }
            if (item.getQty() > availableQty) {
                throw new ConflictException("INSUFFICIENT_STOCK", "Not enough stock for " + current.getLabel());
            }
            item.setProduct(current.getProduct());
            item.setVariant(current);
            item.setUnitPriceCents(current.getPriceCents());
            item.setLineTotalCents(Math.multiplyExact(current.getPriceCents(), (long) item.getQty()));
            subtotal += item.getLineTotalCents();
        }

        long shipping = storeProperties.getShippingFlatCents();
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;
        cart.setUpdatedAt(Instant.now());
        return MoneySummary.builder()
                .subtotalCents(subtotal)
                .shippingCents(shipping)
                .taxCents(tax)
                .totalCents(total)
                .currency(storeProperties.getCurrency())
                .build();
    }

    private long calculateTax(long subtotal) {
        BigDecimal rate = storeProperties.getTaxRatePercent();
        if (rate == null || rate.signum() == 0) {
            return 0L;
        }
        return rate.multiply(BigDecimal.valueOf(subtotal))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                .longValue();
    }

    public record CartCheckoutSnapshot(Cart cart, MoneySummary totals) {}
}
