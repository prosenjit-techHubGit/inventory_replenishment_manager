import type { Recommendation } from "../models/types";
import { request } from "./httpClient";

export function listRecommendations(): Promise<Recommendation[]> {
  return request("/api/v1/recommendations");
}

export function getRecommendation(id: string): Promise<Recommendation> {
  return request(`/api/v1/recommendations/${id}`);
}
