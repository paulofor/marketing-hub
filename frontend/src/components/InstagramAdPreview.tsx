import { Creative } from "../api/creative/useCreatives";

interface Props {
  creative: Creative;
}

export default function InstagramAdPreview({ creative }: Props) {
  const cta = creative.cta?.replace(/_/g, " ") || "SAIBA MAIS";
  return (
    <div
      style={{
        maxWidth: 360,
        margin: "0 auto",
        border: "1px solid #dbdbdb",
        borderRadius: 8,
        fontFamily: "Arial, sans-serif",
        backgroundColor: "#fff",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          padding: "8px",
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: "50%",
            background: "#eee",
            marginRight: 8,
          }}
        />
        <div>
          <div style={{ fontWeight: 600 }}>Página</div>
          <div style={{ fontSize: 12, color: "#8e8e8e" }}>Patrocinado</div>
        </div>
      </div>
      <img
        src={creative.imageUrl}
        alt="creative"
        style={{ width: "100%", display: "block" }}
      />
      <div style={{ padding: 8 }}>
        <p style={{ marginBottom: 8 }}>
          <strong>{creative.headline}</strong> {creative.primaryText}
        </p>
        <button
          style={{
            width: "100%",
            padding: "8px",
            background: "#fff",
            border: "1px solid #dbdbdb",
            borderRadius: 4,
            fontWeight: 600,
          }}
        >
          {cta}
        </button>
      </div>
    </div>
  );
}
