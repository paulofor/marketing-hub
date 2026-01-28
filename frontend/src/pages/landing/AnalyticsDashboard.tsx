import PageTitle from "../../components/PageTitle";
import { useEffect, useMemo, useState } from "react";
import ReactECharts from "echarts-for-react";
import axios from "axios";

interface PerformanceSnapshot {
  experimentId: number;
  experimentName: string;
  campaignId: string;
  capturedAt: string;
  dateStart?: string;
  dateStop?: string;
  spend?: number;
  currency?: string;
  impressions?: number;
  reach?: number;
  clicks?: number;
  leads?: number;
  ctr?: number;
  cpc?: number;
  cpm?: number;
  cpl?: number;
}

export default function AnalyticsDashboard() {
  const [data, setData] = useState<PerformanceSnapshot[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    axios
      .get("/api/facebook-campaigns/performance")
      .then((r) => setData(r.data || []))
      .catch(() => setError("Não foi possível carregar os dados de desempenho."))
      .finally(() => setLoading(false));
  }, []);

  const currency = useMemo(() => data[0]?.currency || "BRL", [data]);

  const chartOption = useMemo(() => {
    return {
      tooltip: {
        trigger: "axis",
        formatter: (params: any[]) => {
          if (!params?.length) return "";
          const item = data[params[0].dataIndex];
          if (!item) return "";
          const spendValue = formatCurrency(item.spend || 0, item.currency || currency);
          const leadsValue = item.leads ?? 0;
          return `${item.experimentName}<br/>Gasto: ${spendValue}<br/>Leads: ${leadsValue}`;
        },
      },
      xAxis: {
        type: "category",
        data: data.map((d) => d.experimentName),
        axisLabel: { interval: 0, rotate: 20 },
      },
      yAxis: {
        type: "value",
        name: "Gasto",
      },
      series: [
        {
          data: data.map((d) => Number(d.spend || 0)),
          type: "bar",
          name: "Gasto",
          itemStyle: { color: "#2563eb" },
        },
      ],
    };
  }, [currency, data]);

  return (
    <div className="space-y-6">
      <PageTitle>Desempenho das Campanhas</PageTitle>
      {error && <div className="alert alert-error">{error}</div>}
      <ReactECharts option={chartOption} showLoading={loading} style={{ height: 360 }} />
      <div className="overflow-x-auto border rounded-md">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50 text-left">
            <tr>
              <th className="px-4 py-2">Experimento</th>
              <th className="px-4 py-2">Atualizado</th>
              <th className="px-4 py-2">Gasto</th>
              <th className="px-4 py-2">Impressões</th>
              <th className="px-4 py-2">Cliques</th>
              <th className="px-4 py-2">Leads</th>
              <th className="px-4 py-2">CPC</th>
              <th className="px-4 py-2">CPM</th>
              <th className="px-4 py-2">CPL</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr key={`${item.experimentId}-${item.campaignId}`} className="border-t">
                <td className="px-4 py-2">{item.experimentName}</td>
                <td className="px-4 py-2">{formatDate(item.capturedAt)}</td>
                <td className="px-4 py-2">{formatCurrency(item.spend || 0, item.currency || currency)}</td>
                <td className="px-4 py-2">{formatNumber(item.impressions)}</td>
                <td className="px-4 py-2">{formatNumber(item.clicks)}</td>
                <td className="px-4 py-2">{formatNumber(item.leads)}</td>
                <td className="px-4 py-2">{formatCurrency(item.cpc ?? 0, item.currency || currency)}</td>
                <td className="px-4 py-2">{formatCurrency(item.cpm ?? 0, item.currency || currency)}</td>
                <td className="px-4 py-2">{formatCurrency(item.cpl ?? 0, item.currency || currency)}</td>
              </tr>
            ))}
            {!data.length && !loading && (
              <tr>
                <td colSpan={9} className="px-4 py-6 text-center text-gray-500">
                  Nenhum experimento em execução.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function formatCurrency(value: number, currency: string) {
  try {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(value);
  } catch (err) {
    return value.toFixed(2);
  }
}

function formatNumber(value?: number) {
  if (value === undefined || value === null) {
    return "-";
  }
  return value.toLocaleString("pt-BR");
}

function formatDate(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR");
}
