import React from "react";
import { createRoot } from "react-dom/client";
import { MiraCandidateApp } from "./MiraCandidateApp";
import "./mira-candidate.css";

/** Inicializa a candidata privada sem carregar a aplicação ou os estilos comerciais históricos. */
createRoot(document.getElementById("root") as HTMLElement).render(
  <MiraCandidateApp />,
);
