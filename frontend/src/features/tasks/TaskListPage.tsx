import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { claimTask, listTasks } from "../../api/tasksApi";
import { useRole } from "../../app/RoleContext";
import { ageLabel, taskGroup } from "../../app/format";
import { useToast } from "../../app/ToastContext";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { HttpError } from "../../api/httpClient";

const FILTERS = ["All", "Recommendations", "Approvals", "Fulfillment"] as const;

export function TaskListPage() {
  const { role } = useRole();
  const { notifySuccess } = useToast();
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("All");
  const [actionError, setActionError] = useState<string | null>(null);
  const { data, error, loading, reload } = useLoad(() => listTasks(role.id), [role.id]);

  const rows = useMemo(() => {
    const tasks = data ?? [];
    if (filter === "All") {
      return tasks;
    }
    return tasks.filter((task) => taskGroup(task.elementId) === filter);
  }, [data, filter]);

  async function onClaim(taskKey: string) {
    setActionError(null);
    try {
      await claimTask(taskKey);
      notifySuccess("Task claimed successfully.");
      await reload();
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to claim task");
    }
  }

  return (
    <>
      <PageHeader title="Inbox" subtitle="Work assigned to your role. Claim a task to work it." />
      <ErrorBanner message={error ?? actionError} />
      <div className="chips">
        {FILTERS.map((item) => (
          <button
            key={item}
            className={`chip${filter === item ? " active" : ""}`}
            onClick={() => setFilter(item)}
            type="button"
          >
            {item}
          </button>
        ))}
      </div>
      {loading ? (
        <LoadingState />
      ) : (
        <div className="card table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Task</th>
                <th>Document</th>
                <th>Process</th>
                <th>Age</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={5} className="muted">
                    No tasks in this queue.
                  </td>
                </tr>
              ) : (
                rows.map((task) => (
                  <tr key={task.taskKey}>
                    <td>{task.name}</td>
                    <td>{task.businessId ?? "—"}</td>
                    <td>{task.bpmnProcessId}</td>
                    <td>{ageLabel(task.creationDate)}</td>
                    <td>
                      {task.assignee ? (
                        <Link className="btn btn-primary" to={`/tasks/${task.taskKey}`}>
                          Open
                        </Link>
                      ) : (
                        <button className="btn btn-secondary" type="button" onClick={() => void onClaim(task.taskKey)}>
                          Claim
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
