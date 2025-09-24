import { createContext, useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import "./breadcrumbs.css";

export interface Crumb {
  label: string;
  to?: string;
  icon?: string;
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
      {crumbs.length > 0 ? (
        <nav aria-label="breadcrumb" className="app-breadcrumbs">
          <ol className="app-breadcrumbs__list">
            {crumbs.map((c, i) => {
              const isActive = i === crumbs.length - 1;
              const label = (
                <>
                  <span className="app-breadcrumbs__separator" aria-hidden="true">
                    /
                  </span>
                  <span className="app-breadcrumbs__item-label">
                    {c.icon ? (
                      <span className="app-breadcrumbs__item-icon" aria-hidden="true">
                        <img src={c.icon} alt="" />
                      </span>
                    ) : null}
                    <span className="app-breadcrumbs__item-text">{c.label}</span>
                  </span>
                </>
              );
              return (
                <li
                  key={i}
                  className={`app-breadcrumbs__item${
                    isActive ? " is-active" : ""
                  }`}
                >
                  {c.to && !isActive ? (
                    <Link to={c.to}>{label}</Link>
                  ) : (
                    <span aria-current={isActive ? "page" : undefined}>{label}</span>
                  )}
                </li>
              );
            })}
          </ol>
        </nav>
      ) : null}
      {children}
    </SetCrumbsContext.Provider>
  );
}
