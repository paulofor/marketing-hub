import { FormEvent, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { createLead } from "../api";
import { LeadDetails } from "../types";

interface LeadFormProps {
  onLeadCreated: (lead: LeadDetails) => void;
}

const MIN_IMAGE_WIDTH = 600;

function LeadForm({ onLeadCreated }: LeadFormProps) {
  const formRef = useRef<HTMLFormElement>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const mutation = useMutation({
    mutationFn: createLead,
    onSuccess: (lead) => {
      onLeadCreated(lead);
      setLocalError(null);
      formRef.current?.reset();
    },
    onError: (error: unknown) => {
      if (error instanceof Error) {
        setLocalError(error.message);
      } else {
        setLocalError("Não foi possível enviar o lead.");
      }
    }
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);

    const name = (formData.get("name") as string)?.trim();
    const email = (formData.get("email") as string)?.trim();
    const notes = (formData.get("notes") as string)?.trim();
    const image = formData.get("image");

    if (!image || !(image instanceof File)) {
      setLocalError("Selecione uma imagem para continuar.");
      return;
    }

    const isValid = await validateImageDimensions(image);
    if (!isValid) {
      setLocalError(`A imagem precisa ter pelo menos ${MIN_IMAGE_WIDTH}px de largura.`);
      return;
    }

    mutation.mutate({
      name,
      email,
      notes: notes?.length ? notes : undefined,
      image
    });
  };

  return (
    <form ref={formRef} onSubmit={handleSubmit}>
      <div className="form-field">
        <label className="form-label" htmlFor="name">
          Nome<span className="required">*</span>
        </label>
        <input
          id="name"
          name="name"
          className="form-input"
          type="text"
          placeholder="Ana Silva"
          required
        />
      </div>

      <div className="form-field">
        <label className="form-label" htmlFor="email">
          E-mail<span className="required">*</span>
        </label>
        <input
          id="email"
          name="email"
          className="form-input"
          type="email"
          placeholder="ana@empresa.com"
          required
        />
      </div>

      <div className="form-field">
        <label className="form-label" htmlFor="notes">
          Observações
        </label>
        <textarea
          id="notes"
          name="notes"
          className="form-textarea"
          placeholder="Conte um pouco sobre o contexto do lead"
        />
      </div>

      <div className="form-field">
        <label className="form-label" htmlFor="image">
          Imagem de referência<span className="required">*</span>
        </label>
        <input id="image" name="image" className="form-file" type="file" accept="image/*" required />
        <span className="upload-hint">Envie arquivos JPG ou PNG com pelo menos {MIN_IMAGE_WIDTH}px de largura.</span>
      </div>

      {localError && <p className="upload-hint" style={{ color: "#dc2626" }}>{localError}</p>}

      <button className="button-primary" type="submit" disabled={mutation.isPending}>
        {mutation.isPending && <span className="spinner" aria-hidden="true" />}
        {mutation.isPending ? "Enviando..." : "Enviar lead"}
      </button>
    </form>
  );
}

async function validateImageDimensions(file: File): Promise<boolean> {
  if (!file.type.startsWith("image/")) {
    return false;
  }

  const dataUrl = URL.createObjectURL(file);
  try {
    const image = await loadImage(dataUrl);
    return image.width >= MIN_IMAGE_WIDTH;
  } finally {
    URL.revokeObjectURL(dataUrl);
  }
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = url;
  });
}

export default LeadForm;
