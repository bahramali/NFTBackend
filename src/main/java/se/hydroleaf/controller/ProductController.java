package se.hydroleaf.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<Map<String, Object>> listProducts() {
        return List.of(
                Map.of("id", 1, "name", "NFT Lamp", "price", 99.99),
                Map.of("id", 2, "name", "NFT Sensor", "price", 129.99)
        );
    }
}
