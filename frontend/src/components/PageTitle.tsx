import type { ReactNode } from "react";
import "./PageTitle.css";

interface PageTitleProps {
  children: ReactNode;
  icon?: string;
  iconAlt?: string;
}

export default function PageTitle({
  children,
  icon,
  iconAlt = "",
}: PageTitleProps) {
  return (
    <h1 className="page-title">
      {icon ? (
        <span className="page-title-icon">
          <img src={icon} alt={iconAlt} loading="lazy" />
        </span>
      ) : null}
      <span className="page-title-text">{children}</span>
    </h1>
  );
}
