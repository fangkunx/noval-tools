package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.AssetService;
import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import com.noval.workbench.interfaces.rest.dto.AssetRequest;
import com.noval.workbench.interfaces.rest.dto.AssetResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public List<AssetResponse> list(@RequestParam(required = false) String type) {
        AssetType assetType = null;
        if (type != null && !type.isEmpty()) {
            try {
                assetType = AssetType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 非法类型参数，返回全部
            }
        }
        return assetService.listByType(assetType).stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return assetService.getById(id)
                .map(a -> ResponseEntity.ok(AssetResponse.from(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AssetResponse create(@RequestBody AssetRequest request) {
        Asset asset = assetService.create(request.toDomain());
        return AssetResponse.from(asset);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AssetRequest request) {
        if (!assetService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "资产不存在"));
        }
        Asset updated = assetService.update(id, request.toDomain());
        return ResponseEntity.ok(AssetResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
