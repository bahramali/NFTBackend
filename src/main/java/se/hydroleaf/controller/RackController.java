package se.hydroleaf.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.shelly.model.Rack;
import se.hydroleaf.shelly.registry.ShellyRegistry;

@RestController
@RequestMapping("/api")
public class RackController {

    private final ShellyRegistry registry;

    public RackController(ShellyRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/racks")
    public List<Rack> getRacks() {
        return registry.getRacks();
    }
}
