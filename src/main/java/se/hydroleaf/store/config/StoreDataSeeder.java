package se.hydroleaf.store.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.repository.ProductRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;

@Component
@RequiredArgsConstructor
public class StoreDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreDataSeeder.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StoreProperties storeProperties;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        Product starterKit = Product.builder()
                .sku("HYDRO-STARTER")
                .name("Hydroleaf Starter Kit")
                .description("Complete hydroponic starter bundle with reservoir, pump, and grow lights")
                .priceCents(249900)
                .currency(storeProperties.getCurrency())
                .active(true)
                .inventoryQty(25)
                .imageUrl("https://cdn.hydroleaf.se/images/starter-kit.jpg")
                .category("kits")
                .build();
        Product nutrient = Product.builder()
                .sku("NUTRIENT-A")
                .name("Hydroleaf Grow Nutrient A")
                .description("Vegetative stage nutrients optimized for leafy greens")
                .priceCents(15900)
                .currency(storeProperties.getCurrency())
                .active(true)
                .inventoryQty(80)
                .imageUrl("https://cdn.hydroleaf.se/images/nutrient-a.jpg")
                .category("nutrients")
                .build();
        Product climateSensor = Product.builder()
                .sku("CLIMATE-SENSOR")
                .name("Climate Sensor Node")
                .description("Wireless temp/humidity sensor that pairs with the Hydroleaf dashboard")
                .priceCents(99900)
                .currency(storeProperties.getCurrency())
                .active(true)
                .inventoryQty(40)
                .imageUrl("https://cdn.hydroleaf.se/images/climate-sensor.jpg")
                .category("sensors")
                .build();
        Product basil = Product.builder()
                .sku("FRESH-BASIL")
                .name("Fresh Basil")
                .description("Locally grown basil harvested for maximum aroma")
                .priceCents(0)
                .currency(storeProperties.getCurrency())
                .active(true)
                .inventoryQty(0)
                .imageUrl("https://cdn.hydroleaf.se/images/basil.jpg")
                .category("greens")
                .build();

        productRepository.saveAll(List.of(starterKit, nutrient, climateSensor, basil));

        List<ProductVariant> variants = List.of(
                ProductVariant.builder()
                        .product(starterKit)
                        .label("Standard")
                        .weightGrams(0)
                        .priceCents(249900)
                        .stockQuantity(25)
                        .sku("HYDRO-STARTER-STD")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(nutrient)
                        .label("Standard")
                        .weightGrams(0)
                        .priceCents(15900)
                        .stockQuantity(80)
                        .sku("NUTRIENT-A-STD")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(climateSensor)
                        .label("Standard")
                        .weightGrams(0)
                        .priceCents(99900)
                        .stockQuantity(40)
                        .sku("CLIMATE-SENSOR-STD")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(basil)
                        .label("50g")
                        .weightGrams(50)
                        .priceCents(2900)
                        .stockQuantity(100)
                        .sku("BASIL-50G")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(basil)
                        .label("70g")
                        .weightGrams(70)
                        .priceCents(3500)
                        .stockQuantity(40)
                        .sku("BASIL-70G")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(basil)
                        .label("100g")
                        .weightGrams(100)
                        .priceCents(4500)
                        .stockQuantity(0)
                        .sku("BASIL-100G")
                        .active(true)
                        .build());

        productVariantRepository.saveAll(variants);
        log.info("Seeded {} store products and {} variants", 4, variants.size());
    }
}
