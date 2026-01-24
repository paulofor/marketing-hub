import { getMissingConfigurationInfo } from "./missingConfigurationLabels";

interface MissingConfigurationListProps {
  items: string[];
  className?: string;
  itemClassName?: string;
}

export function MissingConfigurationList({
  items,
  className,
  itemClassName,
}: MissingConfigurationListProps) {
  if (!items || items.length === 0) {
    return null;
  }

  return (
    <ul className={className}>
      {items.map((item) => {
        const info = getMissingConfigurationInfo(item);
        return (
          <li key={item} className={itemClassName}>
            <div>{info.label}</div>
            {info.helperText ? (
              <p className="text-body-secondary small mb-0">{info.helperText}</p>
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}
