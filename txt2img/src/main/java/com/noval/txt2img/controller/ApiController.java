package com.noval.txt2img.controller;

import com.noval.txt2img.dto.GenerateRequest;
import com.noval.txt2img.dto.TaskStatus;
import com.noval.txt2img.service.Lk888Service;
import com.noval.txt2img.service.TokenPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ApiController {

    @Autowired
    private Lk888Service lk888Service;

    @Autowired
    private TokenPlanService tokenPlanService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Submit a generation request. For lk888 returns task_id; for tokenplan waits for result.
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            TaskStatus result;
            if ("tokenplan".equals(req.getBackend())) {
                result = tokenPlanService.generate(req);
            } else {
                // lk888 default
                result = lk888Service.submit(req);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("state", "error");
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Poll task status (lk888 async only). Pass ?wait=true to block until done.
     */
    @GetMapping("/api/status/{taskId}")
    @ResponseBody
    public ResponseEntity<?> pollStatus(
            @PathVariable long taskId,
            @RequestParam(defaultValue = "false") boolean wait) {
        try {
            TaskStatus status = lk888Service.poll(taskId, wait);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("state", "error");
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Return available models and their parameter specs.
     */
    @GetMapping("/api/models")
    @ResponseBody
    public ResponseEntity<?> models(@RequestParam(defaultValue = "image") String type) {
        // Return hardcoded model configs (can be extended to fetch dynamically)
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("models", new Object[]{
            modelSpec("tt-image-2.6", "TT Image 2.6", "lk888",
                new String[][]{
                    {"version", "select", "flare", "flare|sunburst"},
                    {"aspect_ratio", "select", "1:1", "1:1|16:9|9:16|4:3|3:4|3:2|2:3|5:4|4:5|2:1|1:2|21:9|9:21"},
                    {"resolution", "select", "1K", "1K|2K|4K"},
                    {"quality", "select", "auto", "auto|low|medium|high|xhigh|max"},
                    {"complexity", "select", "medium", "low|medium|high"},
                    {"output_format", "select", "png", "png|jpeg|webp|svg|gif"},
                    {"background", "select", "auto", "auto|opaque|transparent"},
                    {"size", "text", "", ""},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("tt-image-2.5", "TT Image 2.5", "lk888",
                new String[][]{
                    {"aspect_ratio", "select", "1:1", "1:1|16:9|9:16|4:3|3:4|3:2|2:3|5:4|4:5|2:1|1:2|21:9|9:21"},
                    {"resolution", "select", "1K", "1K|2K|4K"},
                    {"quality", "select", "auto", "auto|low|medium|high|xhigh|max"},
                    {"output_format", "select", "png", "png|jpeg|webp|svg|gif"},
                    {"background", "select", "auto", "auto|opaque|transparent"},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("banana-2.1", "Banana 2.1", "lk888",
                new String[][]{
                    {"aspect_ratio", "select", "1:1", "1:1|16:9|9:16|4:3|3:4"},
                    {"resolution", "select", "1K", "1K|2K|4K"},
                    {"quality", "select", "auto", "auto|low|medium|high"},
                    {"output_format", "select", "png", "png|jpeg|webp"},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("gk-image-2.0", "GK Image 2.0", "lk888",
                new String[][]{
                    {"aspect_ratio", "select", "1:1", "1:1|16:9|9:16|4:3|3:4"},
                    {"resolution", "select", "1K", "1K|2K|4K"},
                    {"quality", "select", "auto", "auto|low|medium|high|xhigh|max"},
                    {"output_format", "select", "png", "png|jpeg|webp"},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("qwen-image-3.0-pro", "通义万相 3.0 Pro", "tokenplan",
                new String[][]{
                    {"size", "select", "1024*1024", "1024*1024|1280*720|720*1280|1024*768|768*1024|1792*768"},
                    {"numImages", "number", "1", ""},
                    {"negativePrompt", "text", "", ""},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("qwen-image-2.0-pro", "通义万相 2.0 Pro", "tokenplan",
                new String[][]{
                    {"size", "select", "1024*1024", "1024*1024|1280*720|720*1280|1024*768|768*1024|1792*768"},
                    {"numImages", "number", "1", ""},
                    {"negativePrompt", "text", "", ""},
                    {"referenceImages", "images", "", ""},
                }),
            modelSpec("wan2.7-image", "万相 2.7", "tokenplan",
                new String[][]{
                    {"size", "select", "1024*1024", "1024*1024|1280*720|720*1280|1024*768|768*1024|1792*768"},
                    {"numImages", "number", "1", ""},
                    {"negativePrompt", "text", "", ""},
                    {"referenceImages", "images", "", ""},
                }),
        });
        return ResponseEntity.ok(data);
    }

    private Map<String, Object> modelSpec(String name, String displayName, String backend, String[][] params) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("displayName", displayName);
        m.put("backend", backend);
        Map<String, Object>[] paramMaps = new HashMap[params.length];
        for (int i = 0; i < params.length; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("key", params[i][0]);
            p.put("type", params[i][1]);
            p.put("defaultValue", params[i][2]);
            p.put("options", params[i][3]);
            paramMaps[i] = p;
        }
        m.put("params", paramMaps);
        return m;
    }
}