package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.ArtStyleService;
import com.noval.workbench.domain.model.ArtStyle;
import com.noval.workbench.interfaces.rest.dto.ArtStyleRequest;
import com.noval.workbench.interfaces.rest.dto.ArtStyleResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/styles")
public class ArtStyleController {

    private final ArtStyleService artStyleService;

    public ArtStyleController(ArtStyleService artStyleService) {
        this.artStyleService = artStyleService;
    }

    @GetMapping
    public List<ArtStyleResponse> list() {
        return artStyleService.listAll().stream()
                .map(ArtStyleResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return artStyleService.getById(id)
                .map(s -> ResponseEntity.ok(ArtStyleResponse.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ArtStyleResponse create(@RequestBody ArtStyleRequest request) {
        ArtStyle style = artStyleService.create(request.toDomain());
        return ArtStyleResponse.from(style);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ArtStyleRequest request) {
        if (!artStyleService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "画风不存在"));
        }
        ArtStyle updated = artStyleService.update(id, request.toDomain());
        return ResponseEntity.ok(ArtStyleResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        artStyleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
