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
import se.hydroleaf.store.api.dto.ProductVariantAdminResponse;
import se.hydroleaf.store.api.dto.ProductVariantRequest;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.repository.ProductRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public List<ProductVariantAdminResponse> listVariants(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        return productVariantRepository.findByProductId(product.getId()).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public ProductVariantAdminResponse createVariant(UUID productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        validateVariantRequest(request);

        String normalizedSku = normalizeSku(request.getSku());
        ensureUniqueSku(normalizedSku, null);
        ensureUniqueWeight(productId, request.getWeightGrams(), null);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .label(resolveLabel(request))
                .weightGrams(request.getWeightGrams())
                .priceCents(request.getPriceCents())
                .stockQuantity(request.getStockQuantity())
                .sku(normalizedSku)
                .ean(request.getEan())
                .active(request.isActive())
                .build();
        variant = productVariantRepository.save(variant);
        log.info("Created product variant id={} productId={} weightGrams={}", variant.getId(), productId, variant.getWeightGrams());
        return toAdminResponse(variant);
    }

    @Transactional
    public ProductVariantAdminResponse updateVariant(UUID productId, UUID variantId, ProductVariantRequest request) {
        validateVariantRequest(request);
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));

        String normalizedSku = normalizeSku(request.getSku());
        ensureUniqueSku(normalizedSku, variantId);
        ensureUniqueWeight(productId, request.getWeightGrams(), variantId);

        variant.setLabel(resolveLabel(request));
        variant.setWeightGrams(request.getWeightGrams());
        variant.setPriceCents(request.getPriceCents());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setSku(normalizedSku);
        variant.setEan(request.getEan());
        variant.setActive(request.isActive());
        variant = productVariantRepository.save(variant);
        log.info("Updated product variant id={} productId={}", variant.getId(), productId);
        return toAdminResponse(variant);
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));
        if (!variant.isActive()) {
            return;
        }
        variant.setActive(false);
        productVariantRepository.save(variant);
        log.info("Deactivated product variant id={} productId={}", variantId, productId);
    }

    private void validateVariantRequest(ProductVariantRequest request) {
        if (request.getWeightGrams() <= 0) {
            throw new BadRequestException("INVALID_WEIGHT", "Weight must be greater than zero");
        }
        if (request.getPriceCents() < 0) {
            throw new BadRequestException("INVALID_PRICE", "Price must be zero or positive");
        }
        if (request.getStockQuantity() < 0) {
            throw new BadRequestException("INVALID_STOCK", "Stock quantity must be zero or positive");
        }
    }

    private void ensureUniqueSku(String sku, UUID currentVariantId) {
        if (sku == null) {
            return;
        }
        productVariantRepository.findBySku(sku).ifPresent(existing -> {
            if (currentVariantId == null || !existing.getId().equals(currentVariantId)) {
                throw new ConflictException("DUPLICATE_SKU", "A variant with this SKU already exists");
            }
        });
    }

    private void ensureUniqueWeight(UUID productId, int weightGrams, UUID currentVariantId) {
        productVariantRepository.findByProductIdAndWeightGrams(productId, weightGrams).ifPresent(existing -> {
            if (currentVariantId == null || !existing.getId().equals(currentVariantId)) {
                throw new ConflictException("DUPLICATE_WEIGHT", "A variant with this weight already exists for this product");
            }
        });
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        return sku.trim().toUpperCase();
    }

    private String resolveLabel(ProductVariantRequest request) {
        if (request.getLabel() == null || request.getLabel().isBlank()) {
            return request.getWeightGrams() + "g";
        }
        return request.getLabel().trim();
    }

    private ProductVariantAdminResponse toAdminResponse(ProductVariant variant) {
        return ProductVariantAdminResponse.builder()
                .id(variant.getId())
                .label(variant.getLabel())
                .weightGrams(variant.getWeightGrams())
                .priceCents(variant.getPriceCents())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .ean(variant.getEan())
                .active(variant.isActive())
                .build();
    }
}
