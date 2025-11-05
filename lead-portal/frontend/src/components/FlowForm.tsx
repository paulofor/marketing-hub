import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { FlowQuestion, FlowQuestionType, LeadPortalFlow } from "../types";

type AnswerValue = string | string[] | File | null;

interface FlowFormProps {
  flow: LeadPortalFlow;
}

export default function FlowForm({ flow }: FlowFormProps) {
  const initialAnswers = useMemo(() => {
    const entries = flow.questions.map<[string, AnswerValue]>((question) => {
      if (question.type === "MULTIPLE_CHOICE") {
        return [question.dataKey, []];
      }
      return [question.dataKey, ""];
    });
    return Object.fromEntries(entries) as Record<string, AnswerValue>;
  }, [flow.questions]);

  const [answers, setAnswers] = useState<Record<string, AnswerValue>>(initialAnswers);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    setAnswers(initialAnswers);
    setErrors({});
    setSubmitted(false);
  }, [initialAnswers]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const validation: Record<string, string> = {};

    flow.questions.forEach((question) => {
      const value = answers[question.dataKey];
      if (!question.required) {
        return;
      }

      if (question.type === "MULTIPLE_CHOICE") {
        if (!Array.isArray(value) || value.length === 0) {
          validation[question.dataKey] = "Selecione ao menos uma opção.";
        }
        return;
      }

      if (question.type === "IMAGE_UPLOAD") {
        if (!(value instanceof File)) {
          validation[question.dataKey] = "Envie um arquivo.";
        }
        return;
      }

      if (typeof value !== "string" || value.trim() === "") {
        validation[question.dataKey] = "Preencha este campo.";
      }
    });

    setErrors(validation);

    if (Object.keys(validation).length === 0) {
      setSubmitted(true);
    } else {
      setSubmitted(false);
    }
  };

  const updateAnswer = (question: FlowQuestion, value: AnswerValue) => {
    setAnswers((current) => ({
      ...current,
      [question.dataKey]: value
    }));
  };

  const handleOptionToggle = (question: FlowQuestion, option: string) => {
    const currentValue = answers[question.dataKey];
    const selected = Array.isArray(currentValue) ? [...currentValue] : [];
    const index = selected.indexOf(option);
    if (index >= 0) {
      selected.splice(index, 1);
    } else {
      selected.push(option);
    }
    updateAnswer(question, selected);
  };

  return (
    <form className="flow-form" onSubmit={handleSubmit} noValidate>
      {submitted ? (
        <div className="success-banner">Suas respostas foram registradas localmente.</div>
      ) : null}

      <ol className="flow-question-list">
        {flow.questions.map((question, index) => (
          <li key={question.dataKey} className="flow-question-item">
            <QuestionField
              index={index + 1}
              question={question}
              value={answers[question.dataKey]}
              error={errors[question.dataKey]}
              onChange={updateAnswer}
              onToggleOption={handleOptionToggle}
            />
          </li>
        ))}
      </ol>

      <button type="submit" className="submit-button">
        Enviar respostas
      </button>
    </form>
  );
}

interface QuestionFieldProps {
  index: number;
  question: FlowQuestion;
  value: AnswerValue;
  error?: string;
  onChange: (question: FlowQuestion, value: AnswerValue) => void;
  onToggleOption: (question: FlowQuestion, option: string) => void;
}

function QuestionField({ index, question, value, error, onChange, onToggleOption }: QuestionFieldProps) {
  return (
    <div className={`flow-question ${error ? "flow-question-error" : ""}`}>
      <label className="flow-question-label">
        <span className="flow-question-number">{index}.</span>
        <span>
          {question.title}
          {question.required ? <span className="required-indicator">*</span> : null}
        </span>
      </label>
      {question.description ? <p className="flow-question-description">{question.description}</p> : null}

      <InputByType
        question={question}
        value={value}
        onChange={onChange}
        onToggleOption={onToggleOption}
      />

      {error ? <p className="flow-question-error-message">{error}</p> : null}
    </div>
  );
}

interface InputByTypeProps {
  question: FlowQuestion;
  value: AnswerValue;
  onChange: (question: FlowQuestion, value: AnswerValue) => void;
  onToggleOption: (question: FlowQuestion, option: string) => void;
}

function InputByType({ question, value, onChange, onToggleOption }: InputByTypeProps) {
  const commonProps = {
    name: question.dataKey,
    required: question.required,
    placeholder: question.placeholder ?? undefined,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      onChange(question, event.target.value)
  };

  switch (question.type) {
    case "TEXTAREA":
      return (
        <textarea
          {...commonProps}
          value={typeof value === "string" ? value : ""}
          rows={4}
          className="flow-input"
        />
      );
    case "NUMBER":
    case "EMAIL":
    case "PHONE":
    case "DATE":
    case "TEXT":
      return (
        <input
          {...commonProps}
          type={mapInputType(question.type)}
          value={typeof value === "string" ? value : ""}
          className="flow-input"
        />
      );
    case "SINGLE_CHOICE":
      return (
        <div className="flow-options">
          {question.options.map((option) => (
            <label key={option} className="flow-option">
              <input
                type="radio"
                name={question.dataKey}
                value={option}
                checked={value === option}
                onChange={() => onChange(question, option)}
              />
              <span>{option}</span>
            </label>
          ))}
        </div>
      );
    case "MULTIPLE_CHOICE":
      return (
        <div className="flow-options">
          {question.options.map((option) => {
            const selected = Array.isArray(value) ? value.includes(option) : false;
            return (
              <label key={option} className="flow-option">
                <input
                  type="checkbox"
                  name={`${question.dataKey}-${option}`}
                  value={option}
                  checked={selected}
                  onChange={() => onToggleOption(question, option)}
                />
                <span>{option}</span>
              </label>
            );
          })}
        </div>
      );
    case "IMAGE_UPLOAD":
      return (
        <input
          className="flow-input"
          type="file"
          accept="image/*"
          name={question.dataKey}
          onChange={(event) => {
            const file = event.target.files?.[0] ?? null;
            onChange(question, file ?? null);
          }}
        />
      );
    default:
      return <p>Tipo de pergunta não suportado.</p>;
  }
}

function mapInputType(type: FlowQuestionType) {
  switch (type) {
    case "NUMBER":
      return "number";
    case "EMAIL":
      return "email";
    case "PHONE":
      return "tel";
    case "DATE":
      return "date";
    default:
      return "text";
  }
}
