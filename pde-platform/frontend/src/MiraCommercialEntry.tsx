import React from "react";
import { createRoot } from "react-dom/client";
import { MiraCommercialApp } from "./MiraCommercialApp";
import "./mira-commercial.css";

/** Inicializa somente a superfície comercial independente de Mira. */
const root = createRoot(document.getElementById("root") as HTMLElement);
root.render(<MiraCommercialApp />);
