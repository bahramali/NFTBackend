package se.hydroleaf.store.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final StoreMapper storeMapper;
    private final StoreProperties storeProperties;

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

    public Product requireActiveProduct(UUID productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not available"));
        validateCurrency(product);
        return product;
    }

    private void validateCurrency(Product product) {
        if (!storeProperties.getCurrency().equalsIgnoreCase(product.getCurrency())) {
            log.warn("Currency mismatch for product {} expected {} but was {}", product.getId(), storeProperties.getCurrency(), product.getCurrency());
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not available in this currency");
        }
    }
}
