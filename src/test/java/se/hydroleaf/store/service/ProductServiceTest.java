package se.hydroleaf.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.store.api.dto.ProductRequest;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.repository.CartItemRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.ProductRepository;

@DataJpaTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, new StoreMapper(), new StoreProperties(),
                cartItemRepository, orderRepository);
    }

    @Test
    void createProduct_generatesIdAndPersistsEntity() {
        ProductRequest request = new ProductRequest();
        request.setSku("NEW-ITEM");
        request.setName("New Product");
        request.setDescription("Created via test");
        request.setPriceCents(12_345);
        request.setCurrency("SEK");
        request.setActive(true);
        request.setInventoryQty(10);
        request.setImageUrl("https://example.com/product.jpg");
        request.setCategory("test");

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getId()).isNotNull();
        assertThat(productRepository.findById(response.getId())).isPresent();
    }

    @Test
    void createProduct_generatesSkuWhenMissing() {
        ProductRequest request = new ProductRequest();
        request.setName("Unnamed Product");
        request.setPriceCents(500);
        request.setCurrency("SEK");
        request.setActive(true);
        request.setInventoryQty(1);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getSku()).isNotBlank();
        assertThat(response.getSku()).hasSizeBetween(7, 64);
    }
}
