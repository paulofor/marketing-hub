import { createContext, useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";

export interface Crumb {
  label: string;
  to?: string;
}

const SetCrumbsContext = createContext<(c: Crumb[]) => void>(() => {});

export function useBreadcrumbs(crumbs: Crumb[]) {
  const set = useContext(SetCrumbsContext);
  useEffect(() => {
    set(crumbs);
  }, [crumbs, set]);
}

export default function BreadcrumbsProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [crumbs, setCrumbs] = useState<Crumb[]>([]);
  return (
    <SetCrumbsContext.Provider value={setCrumbs}>
      <nav aria-label="breadcrumb" className="mb-3">
        <ol className="breadcrumb mb-0">
          {crumbs.map((c, i) => (
            <li key={i} className="breadcrumb-item">
              {c.to ? <Link to={c.to}>{c.label}</Link> : c.label}
            </li>
          ))}
        </ol>
      </nav>
      {children}
    </SetCrumbsContext.Provider>
  );
}
