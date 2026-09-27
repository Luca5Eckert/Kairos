package com.kairos.share.docs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the same versioned contract used by CI, only when explicitly enabled. */
@RestController
@ConditionalOnProperty(prefix = "kairos.api-docs", name = "enabled", havingValue = "true")
public class ApiDocumentationController {
    @GetMapping(value = "/api-docs/openapi.yaml", produces = "application/yaml")
    public ClassPathResource specification() {
        return new ClassPathResource("api-contract/openapi.yaml");
    }

    @GetMapping(value = {"/swagger-ui", "/swagger-ui/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> swaggerUi() {
        // UI assets are pinned to a release; the contract itself is always served locally.
        return ResponseEntity.ok("""
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <title>Kairos API</title>
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui.css">
                </head><body><div id="swagger-ui"></div>
                <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
                <script>SwaggerUIBundle({url:'/api-docs/openapi.yaml',dom_id:'#swagger-ui',persistAuthorization:false});</script>
                </body></html>
                """);
    }
}
