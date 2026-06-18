import { NavLink } from "react-router-dom";

interface OprmModuleNavigationProps {
  occupationSeedRef?: string;
}

const navigationItems = [
  { label: "CNAEs", to: "/oprm", end: true },
  { label: "Públicos Gerais", to: "/oprm/general-audiences" },
  { label: "Jobs", to: "/oprm/jobs" },
];

export default function OprmModuleNavigation({
  occupationSeedRef: _occupationSeedRef,
}: OprmModuleNavigationProps) {
  return (
    <nav aria-label="Navegação interna do OPRM">
      <ul className="nav nav-pills gap-2">
        {navigationItems.map((item) => (
          <li className="nav-item" key={item.to}>
            <NavLink
              to={item.to}
              end={item.end}
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
