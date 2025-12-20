package se.hydroleaf.store.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store")
public class StoreFallbackController {

    @RequestMapping(
            value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
