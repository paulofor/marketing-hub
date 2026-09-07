import React from "react";
import { createRoot } from "react-dom/client";
import { MiraPrivatePrototype } from "./MiraPrivatePrototype";
import "./mira.css";

/** Inicializa somente a superfície privada do produto Mira. */
const root = createRoot(document.getElementById("root") as HTMLElement);
root.render(<MiraPrivatePrototype />);
