import { useEffect, useState } from "react";

/**
 * Table with funnel metrics and ROI badge.
 */
export default function FunnelMetrics() {
  const [metrics, setMetrics] = useState<any[]>([]);

  useEffect(() => {
    setMetrics([]);
  }, []);

  return (
    <table>
      <thead>
        <tr>
          <th>Step</th>
          <th>CTR</th>
          <th>CVR</th>
          <th>Revenue</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {metrics.map((m) => (
          <tr key={m.id}>
            <td>{m.name}</td>
            <td>{m.ctr}%</td>
            <td>{m.cvr}%</td>
            <td>{m.revenue}</td>
            <td>{m.roi > 2 && <span>ROI TOP</span>}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
