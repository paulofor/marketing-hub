import { useSyncExternalStore } from "react";

export type TenantContextValue = {
  tenantId: string;
  userEmail: string;
};

const STORAGE_KEY = "marketinghub.tenantContext";
const DEFAULT_CONTEXT: TenantContextValue = {
  tenantId: "default",
  userEmail: "time@marketinghub.io",
};

const listeners = new Set<() => void>();

function readFromStorage(): TenantContextValue {
  if (typeof window === "undefined") {
    return DEFAULT_CONTEXT;
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return DEFAULT_CONTEXT;
    }
    const parsed = JSON.parse(raw);
    return {
      tenantId: parsed.tenantId || DEFAULT_CONTEXT.tenantId,
      userEmail: parsed.userEmail || DEFAULT_CONTEXT.userEmail,
    };
  } catch (error) {
    return DEFAULT_CONTEXT;
  }
}

function writeToStorage(value: TenantContextValue) {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function setTenantContext(value: TenantContextValue) {
  writeToStorage(value);
  listeners.forEach((listener) => listener());
}

export function getTenantContextSnapshot(): TenantContextValue {
  return readFromStorage();
}

export function useTenantContext() {
  return useSyncExternalStore(subscribe, readFromStorage, () => DEFAULT_CONTEXT);
}
