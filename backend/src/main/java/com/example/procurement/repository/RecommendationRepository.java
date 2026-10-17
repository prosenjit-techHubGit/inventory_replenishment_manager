package com.example.procurement.repository;

import com.example.procurement.domain.recommendation.Recommendation;
import com.example.procurement.domain.recommendation.RecommendationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    boolean existsByItemIdAndStatusIn(UUID itemId, List<RecommendationStatus> statuses);

    Optional<Recommendation> findTopByItemIdAndStatusOrderByCreatedAtDesc(
            UUID itemId, RecommendationStatus status);

    Optional<Recommendation> findTopByItemIdOrderByCreatedAtDesc(UUID itemId);
}
