import { useParams } from "react-router-dom";
import { ReviewRecommendationPage } from "./ReviewRecommendationPage";

export function RecommendationDetailPage() {
  const { id = "" } = useParams();
  return <ReviewRecommendationPage recommendationId={id} />;
}
