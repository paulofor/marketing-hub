import type { ReactNode } from "react";
import "./PageTitle.css";

type PageTitleProps = {
  icon?: string;
  iconAlt?: string;
  subtitle?: ReactNode;
} &
  (
    | {
        children: ReactNode;
        title?: never;
      }
    | {
        title: ReactNode;
        children?: never;
      }
  );

export default function PageTitle({
  children,
  title,
  icon,
  iconAlt = "",
  subtitle,
}: PageTitleProps) {
  const titleContent = title ?? children;

  return (
    <div className="page-title-wrapper">
      <h1 className="page-title">
        {icon ? (
          <span className="page-title-icon">
            <img src={icon} alt={iconAlt} loading="lazy" />
          </span>
        ) : null}
        <span className="page-title-text">{titleContent}</span>
      </h1>
      {subtitle ? <p className="page-title-subtitle">{subtitle}</p> : null}
    </div>
  );
}
