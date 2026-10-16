import { humanStatus, statusTone } from "../app/format";

export function StatusBadge({ status }: { status: string }) {
  return <span className={`badge badge-${statusTone(status)}`}>{humanStatus(status)}</span>;
}
