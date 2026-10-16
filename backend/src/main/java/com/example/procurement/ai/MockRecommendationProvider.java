package com.example.procurement.ai;

import com.example.procurement.ai.RecommendationRequest.EligibleSupplier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a real AI adapter (plan sections 7, 17). Picks the supplier with the
 * shortest lead time (tie-broken by lowest unit price) and sizes the order to cover the reorder
 * deficit plus a 14-day consumption buffer. Swap/extend via a real provider bean later.
 */
@Component
public class MockRecommendationProvider implements RecommendationProvider {

    private static final int CONSUMPTION_BUFFER_DAYS = 14;
    private static final int DELIVERY_SAFETY_DAYS = 2;

    @Override
    public RecommendationResponse recommend(RecommendationRequest request) {
        if (request.eligibleSuppliers().isEmpty()) {
            throw new IllegalStateException("No eligible suppliers for item " + request.sku());
        }
        EligibleSupplier chosen = request.eligibleSuppliers().stream()
                .min(Comparator.comparingInt(EligibleSupplier::leadTimeDays)
                        .thenComparing(EligibleSupplier::unitPrice))
                .orElseThrow();

        BigDecimal deficit = request.reorderLevel().subtract(request.onHandQuantity()).max(BigDecimal.ZERO);
        BigDecimal consumption = request.averageConsumption() == null ? BigDecimal.ZERO : request.averageConsumption();
        BigDecimal buffer = consumption.multiply(BigDecimal.valueOf(CONSUMPTION_BUFFER_DAYS));
        BigDecimal recommendedQuantity = deficit.add(buffer).setScale(2, RoundingMode.HALF_UP);
        if (recommendedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            recommendedQuantity = request.reorderLevel().setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate targetDeliveryDate = LocalDate.now().plusDays(chosen.leadTimeDays() + DELIVERY_SAFETY_DAYS);
        String rationale = "Deterministic recommendation: restock to cover the reorder deficit plus "
                + CONSUMPTION_BUFFER_DAYS + " days of average consumption via supplier " + chosen.code()
                + " (lead time " + chosen.leadTimeDays() + " days).";
        return new RecommendationResponse(recommendedQuantity, chosen.supplierId(), targetDeliveryDate, chosen.riskLevel(), rationale);
    }
}
