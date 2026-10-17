import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";

interface Toast {
  id: number;
  message: string;
  leaving: boolean;
}

interface ToastContextValue {
  notifySuccess: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);
const TOAST_VISIBLE_MS = 2000;
const TOAST_FADE_MS = 250;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<Toast | null>(null);
  const hideTimer = useRef<number | null>(null);
  const fadeTimer = useRef<number | null>(null);
  const nextId = useRef(0);

  const clearTimers = useCallback(() => {
    if (hideTimer.current !== null) {
      window.clearTimeout(hideTimer.current);
      hideTimer.current = null;
    }
    if (fadeTimer.current !== null) {
      window.clearTimeout(fadeTimer.current);
      fadeTimer.current = null;
    }
  }, []);

  const dismiss = useCallback(() => {
    clearTimers();
    setToast(null);
  }, [clearTimers]);

  const notifySuccess = useCallback(
    (message: string) => {
      clearTimers();
      nextId.current += 1;
      const id = nextId.current;
      setToast({ id, message, leaving: false });
      hideTimer.current = window.setTimeout(() => {
        setToast((current) => (current?.id === id ? { ...current, leaving: true } : current));
        fadeTimer.current = window.setTimeout(() => {
          setToast((current) => (current?.id === id ? null : current));
        }, TOAST_FADE_MS);
      }, TOAST_VISIBLE_MS);
    },
    [clearTimers],
  );

  const value = useMemo(() => ({ notifySuccess }), [notifySuccess]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast ? (
        <div className="toast-host" role="status" aria-live="polite">
          <div className={`toast toast-success${toast.leaving ? " toast-leaving" : ""}`} key={toast.id}>
            <span className="toast-dot" aria-hidden="true" />
            <span className="toast-message">{toast.message}</span>
            <button className="toast-dismiss" type="button" onClick={dismiss} aria-label="Dismiss">
              ×
            </button>
          </div>
        </div>
      ) : null}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within ToastProvider");
  }
  return context;
}
