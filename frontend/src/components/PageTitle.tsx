import type { ReactNode } from "react";
import "./PageTitle.css";

interface PageTitleProps {
  children: ReactNode;
}

export default function PageTitle({ children }: PageTitleProps) {
  return (
    <h1 className="page-title">
      <span className="page-title-text">{children}</span>
    </h1>
  );
}
