import { NavLink } from "react-router-dom";

interface OprmModuleNavigationProps {
  occupationSeedRef?: string;
}

interface OprmNavItem {
  label: string;
  to?: string;
}

const baseItems: OprmNavItem[] = [
  { label: "Ocupações", to: "/oprm" },
  { label: "Operações" },
];

export default function OprmModuleNavigation({
  occupationSeedRef,
}: OprmModuleNavigationProps) {
  const scopedItems: OprmNavItem[] = occupationSeedRef
    ? [
        {
          label: "Rotina",
          to: `/oprm/routine/${encodeURIComponent(occupationSeedRef)}`,
        },
        {
          label: "Oferta",
          to: `/oprm/offer/${encodeURIComponent(occupationSeedRef)}`,
        },
        {
          label: "Evidências",
          to: `/oprm/evidence/${encodeURIComponent(occupationSeedRef)}`,
        },
        {
          label: "Feedback",
          to: `/oprm/feedback/${encodeURIComponent(occupationSeedRef)}`,
        },
      ]
    : [{ label: "Rotina" }, { label: "Oferta" }, { label: "Evidências" }, { label: "Feedback" }];

  const items: OprmNavItem[] = [baseItems[0], ...scopedItems, ...baseItems.slice(1)];

  return (
    <nav aria-label="Navegação interna do OPRM">
      <ul className="nav nav-pills gap-2">
        {items.map((item) => (
          <li className="nav-item" key={item.label}>
            {item.to ? (
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  isActive ? "nav-link active" : "nav-link"
                }
              >
                {item.label}
              </NavLink>
            ) : (
              <span className="nav-link disabled">{item.label}</span>
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
}
