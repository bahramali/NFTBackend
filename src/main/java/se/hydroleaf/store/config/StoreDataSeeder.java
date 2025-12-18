package se.hydroleaf.store.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.repository.ProductRepository;

@Component
@RequiredArgsConstructor
public class StoreDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreDataSeeder.class);

    private final ProductRepository productRepository;
    private final StoreProperties storeProperties;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> seeds = List.of(
                Product.builder()
                        .sku("HYDRO-STARTER")
                        .name("Hydroleaf Starter Kit")
                        .description("Complete hydroponic starter bundle with reservoir, pump, and grow lights")
                        .priceCents(19900)
                        .currency(storeProperties.getCurrency())
                        .active(true)
                        .inventoryQty(25)
                        .imageUrl("https://cdn.hydroleaf.se/images/starter-kit.jpg")
                        .category("kits")
                        .build(),
                Product.builder()
                        .sku("NUTRIENT-A")
                        .name("Hydroleaf Grow Nutrient A")
                        .description("Vegetative stage nutrients optimized for leafy greens")
                        .priceCents(7900)
                        .currency(storeProperties.getCurrency())
                        .active(true)
                        .inventoryQty(80)
                        .imageUrl("https://cdn.hydroleaf.se/images/nutrient-a.jpg")
                        .category("nutrients")
                        .build(),
                Product.builder()
                        .sku("CLIMATE-SENSOR")
                        .name("Climate Sensor Node")
                        .description("Wireless temp/humidity sensor that pairs with the Hydroleaf dashboard")
                        .priceCents(14900)
                        .currency(storeProperties.getCurrency())
                        .active(true)
                        .inventoryQty(40)
                        .imageUrl("https://cdn.hydroleaf.se/images/climate-sensor.jpg")
                        .category("sensors")
                        .build());

        productRepository.saveAll(seeds);
        log.info("Seeded {} store products", seeds.size());
    }
}
