import { NavLink } from "react-router-dom";

interface OprmModuleNavigationProps {
  occupationSeedRef?: string;
}

const cnaeItems = [
  { label: "CNAEs", to: "/oprm" },
  { label: "Nichos enriquecidos", to: "/oprm/cnaes-enriched" },
];

export default function OprmModuleNavigation({
  occupationSeedRef: _occupationSeedRef,
}: OprmModuleNavigationProps) {
  return (
    <nav aria-label="Navegação interna do OPRM">
      <ul className="nav nav-pills gap-2">
        {cnaeItems.map((item) => (
          <li className="nav-item" key={item.to}>
            <NavLink
              to={item.to}
              end={item.to === "/oprm"}
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
