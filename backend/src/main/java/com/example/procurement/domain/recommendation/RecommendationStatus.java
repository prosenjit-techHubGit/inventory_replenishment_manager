package com.example.procurement.domain.recommendation;

/** Allowed lifecycle states for a Recommendation (plan section 37.1). */
public enum RecommendationStatus {
    PENDING_REVIEW,
    ACCEPTED,
    REJECTED,
    SUPERSEDED,
    FAILED_MANUAL_REVIEW
}
