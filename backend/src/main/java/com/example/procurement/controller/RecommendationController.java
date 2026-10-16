package com.example.procurement.controller;

import com.example.procurement.application.RecommendationApplicationService;
import com.example.procurement.application.dto.RecommendationDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only recommendation endpoints (plan section 18); Accept/Modify/Reject are authoritative
 * workflow actions and go through the Review Recommendation user task via {@code TaskController}.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationApplicationService recommendationApplicationService;

    public RecommendationController(RecommendationApplicationService recommendationApplicationService) {
        this.recommendationApplicationService = recommendationApplicationService;
    }

    @GetMapping
    public List<RecommendationDto> list() {
        return recommendationApplicationService.findAll();
    }

    @GetMapping("/{id}")
    public RecommendationDto getById(@PathVariable UUID id) {
        return recommendationApplicationService.getById(id);
    }
}
