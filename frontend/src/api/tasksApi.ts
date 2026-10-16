import type { TaskDetail, TaskSummary } from "../models/types";
import { request } from "./httpClient";

export function listTasks(candidateGroup?: string): Promise<TaskSummary[]> {
  const params = new URLSearchParams({ activeOnly: "true" });
  if (candidateGroup) {
    params.set("candidateGroup", candidateGroup);
  }
  return request(`/api/v1/tasks?${params.toString()}`);
}

export function getTask(taskKey: string): Promise<TaskDetail> {
  return request(`/api/v1/tasks/${taskKey}`);
}

export function claimTask(taskKey: string): Promise<void> {
  return request(`/api/v1/tasks/${taskKey}/claim`, { method: "POST" });
}

export function completeTask(
  taskKey: string,
  action: string,
  variables: Record<string, unknown> = {},
  idempotencyKey?: string,
): Promise<void> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }
  return request(`/api/v1/tasks/${taskKey}/complete`, {
    method: "POST",
    headers,
    body: JSON.stringify({ action, variables }),
  });
}
