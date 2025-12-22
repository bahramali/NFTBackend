package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.api.dto.ProductRequest;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.repository.CartItemRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final StoreMapper storeMapper;
    private final StoreProperties storeProperties;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;

    public List<ProductResponse> listProducts(Boolean active) {
        List<Product> products = Boolean.TRUE.equals(active)
                ? productRepository.findByActiveTrue()
                : productRepository.findAll();
        return products.stream().map(storeMapper::toProductResponse).toList();
    }

    public ProductResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        return storeMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String normalizedSku = normalizeOrGenerateSku(request.getSku(), request.getName());
        ensureUniqueSku(normalizedSku, null);
        validatePrice(request.getPriceCents());

        Product product = new Product();
        applyDetails(product, request, normalizedSku);
        product = productRepository.save(product);
        log.info("Created product id={} sku={}", product.getId(), product.getSku());
        return storeMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        String normalizedSku = normalizeOrGenerateSku(request.getSku(), request.getName());
        ensureUniqueSku(normalizedSku, productId);
        validatePrice(request.getPriceCents());
        applyDetails(product, request, normalizedSku);
        product = productRepository.save(product);
        log.info("Updated product id={} sku={}", product.getId(), product.getSku());
        return storeMapper.toProductResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        if (cartItemRepository.existsByProductId(productId)) {
            throw new ConflictException("PRODUCT_IN_CART", "Cannot delete a product that exists in a cart. Deactivate it instead.");
        }
        if (orderRepository.existsByItemsProductId(productId)) {
            throw new ConflictException("PRODUCT_IN_ORDER", "Cannot delete a product that has been part of an order.");
        }

        productRepository.delete(product);
        log.info("Deleted product id={} sku={}", product.getId(), product.getSku());
    }

    public Product requireActiveProduct(UUID productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not available"));
        validateCurrency(product);
        return product;
    }

    private void applyDetails(Product product, ProductRequest request, String normalizedSku) {
        product.setSku(normalizedSku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPriceCents(request.getPriceCents());
        product.setCurrency(resolveCurrency(request.getCurrency()));
        product.setActive(request.isActive());
        product.setInventoryQty(request.getInventoryQty());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
    }

    private void ensureUniqueSku(String sku, UUID currentId) {
        productRepository.findBySku(sku).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ConflictException("DUPLICATE_SKU", "A product with this SKU already exists");
            }
        });
    }

    private String normalizeOrGenerateSku(String sku, String name) {
        if (sku == null || sku.isBlank()) {
            return generateSku(name);
        }
        return normalizeSku(sku);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private String generateSku(String name) {
        String base = name == null ? "" : name.replaceAll("[^A-Za-z0-9]", "");
        base = base.isBlank() ? "SKU" : base.substring(0, Math.min(base.length(), 12));
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return (base + "-" + suffix).toUpperCase();
    }

    private String resolveCurrency(String currency) {
        String resolved = currency == null || currency.isBlank()
                ? storeProperties.getCurrency()
                : currency.trim().toUpperCase();
        if (!storeProperties.getCurrency().equalsIgnoreCase(resolved)) {
            throw new BadRequestException("UNSUPPORTED_CURRENCY", "Currency must be " + storeProperties.getCurrency());
        }
        return resolved;
    }

    private void validatePrice(long priceCents) {
        if (priceCents <= 0) {
            throw new BadRequestException("INVALID_PRICE", "Price must be greater than zero");
        }
    }

    private void validateCurrency(Product product) {
        if (!storeProperties.getCurrency().equalsIgnoreCase(product.getCurrency())) {
            log.warn("Currency mismatch for product {} expected {} but was {}", product.getId(), storeProperties.getCurrency(), product.getCurrency());
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not available in this currency");
        }
    }
}
