import PageTitle from "../../components/PageTitle";
import { useEffect, useState } from "react";
import ReactECharts from "echarts-for-react";
import axios from "axios";

export default function AnalyticsDashboard() {
  const [data, setData] = useState<any[]>([]);
  useEffect(() => {
    axios.get("/api/metrics").then((r) => setData(r.data || []));
  }, []);
  const option = {
    xAxis: { type: "category", data: data.map((d) => d.date) },
    yAxis: { type: "value" },
    series: [{ data: data.map((d) => d.cpa), type: "line" }],
  };
  return (
    <div>
      <PageTitle>Desempenho</PageTitle>
      <ReactECharts option={option} style={{ height: 400 }} />
    </div>
  );
}
