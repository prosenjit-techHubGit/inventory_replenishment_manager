package com.example.procurement.ai;

/** Pluggable recommendation source; never invoked directly from a Camunda worker (plan section 17). */
public interface RecommendationProvider {

    RecommendationResponse recommend(RecommendationRequest request);
}
