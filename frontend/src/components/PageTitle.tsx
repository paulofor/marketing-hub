import type { ReactNode } from "react";
import "./PageTitle.css";

interface PageTitleProps {
  children?: ReactNode;
  title?: ReactNode;
  subtitle?: ReactNode;
  icon?: string;
  iconAlt?: string;
}

export default function PageTitle({
  children,
  title,
  subtitle,
  icon,
  iconAlt = "",
}: PageTitleProps) {
  const mainContent = title ?? children;

  return (
    <h1 className="page-title">
      {icon ? (
        <span className="page-title-icon">
          <img src={icon} alt={iconAlt} loading="lazy" />
        </span>
      ) : null}
      <span className="page-title-text">
        {mainContent}
        {subtitle ? <small className="d-block text-muted fs-6 mt-1">{subtitle}</small> : null}
      </span>
    </h1>
  );
}
