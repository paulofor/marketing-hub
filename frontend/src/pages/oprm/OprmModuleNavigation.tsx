import { NavLink } from "react-router-dom";

interface OprmModuleNavigationProps {
  occupationSeedRef?: string;
}

const cnaeItem = { label: "CNAEs", to: "/oprm" };

export default function OprmModuleNavigation({
  occupationSeedRef: _occupationSeedRef,
}: OprmModuleNavigationProps) {
  return (
    <nav aria-label="Navegação interna do OPRM">
      <ul className="nav nav-pills gap-2">
        <li className="nav-item">
          <NavLink
            to={cnaeItem.to}
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            {cnaeItem.label}
          </NavLink>
        </li>
      </ul>
    </nav>
  );
}
