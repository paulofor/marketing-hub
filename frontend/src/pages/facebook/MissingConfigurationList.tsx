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

  const normalizedItems = items
    .filter((item) => !isPlannedStatusEntry(item))
    .filter((item, index, list) => list.indexOf(item) === index);

  const uniqueEntriesByLabel = normalizedItems.reduce<
    Array<{ key: string; label: string; helperText?: string }>
  >((acc, item) => {
    const info = getMissingConfigurationInfo(item);
    const alreadyAdded = acc.some((entry) => entry.label === info.label);
    if (!alreadyAdded) {
      acc.push({ key: item, ...info });
    }
    return acc;
  }, []);

  if (uniqueEntriesByLabel.length === 0) {
    return null;
  }

  return (
    <ul className={className}>
      {uniqueEntriesByLabel.map((entry) => (
        <li key={entry.key} className={itemClassName}>
          <div>{entry.label}</div>
          {entry.helperText ? (
            <p className="text-body-secondary small mb-0">{entry.helperText}</p>
          ) : null}
        </li>
      ))}
    </ul>
  );
}

function isPlannedStatusEntry(item: string): boolean {
  const normalized = item.trim().toLowerCase();
  return normalized === "planned" || normalized === "planejado";
}
