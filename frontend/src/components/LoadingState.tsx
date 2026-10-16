export function LoadingState({ label = "Loading…" }: { label?: string }) {
  return <div className="loading">{label}</div>;
}
