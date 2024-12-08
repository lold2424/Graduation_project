package com.example.Graduation_project.controller;

import com.example.Graduation_project.dto.VtuberRequest;
import com.example.Graduation_project.entity.VtuberEntity;
import com.example.Graduation_project.service.VtuberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vtubers")
public class VtuberController {

    private final VtuberService vtuberService;

    public VtuberController(VtuberService vtuberService) {
        this.vtuberService = vtuberService;
    }


    @PostMapping
    public ResponseEntity<VtuberEntity> createVtuber(@RequestBody VtuberRequest vtuberRequest) {
        VtuberEntity vtuberEntity = vtuberService.createVtuber(vtuberRequest.getDescription(), vtuberRequest.getGender());
        return new ResponseEntity<>(vtuberEntity, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVtubersAndSongs(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "channelId", required = false) String channelId) {

        if ((query == null || query.trim().isEmpty()) && (channelId == null || channelId.trim().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> searchResults = vtuberService.searchVtubersAndSongs(query, channelId);
        return ResponseEntity.ok(searchResults);
    }

}