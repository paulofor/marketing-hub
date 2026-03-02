import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { submitFlowSubmission } from "../api";
import {
  FlowQuestion,
  FlowQuestionType,
  FlowSubmissionResponse,
  LeadPortalFlow,
} from "../types";

type AnswerValue = string | string[] | File | null;

interface FlowFormProps {
  flow: LeadPortalFlow;
  campaignCode?: string | null;
}

export default function FlowForm({ flow, campaignCode }: FlowFormProps) {
  const {
    questions: displayQuestions,
    contactEmailKey,
    contactNameKey,
  } = useMemo(() => enhanceQuestions(flow.questions), [flow.questions]);
  const contactFollowUpConfig = useMemo(
    () => resolveContactFollowUpConfig(displayQuestions),
    [displayQuestions],
  );

  const initialAnswers = useMemo(() => {
    const entries = displayQuestions.map<[string, AnswerValue]>((question) => {
      if (question.type === "MULTIPLE_CHOICE") {
        return [question.dataKey, []];
      }
      return [question.dataKey, ""];
    });
    return Object.fromEntries(entries) as Record<string, AnswerValue>;
  }, [displayQuestions]);

  const [answers, setAnswers] =
    useState<Record<string, AnswerValue>>(initialAnswers);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submissionResult, setSubmissionResult] =
    useState<FlowSubmissionResponse | null>(null);

  const visibleQuestions = useMemo(() => {
    if (!contactFollowUpConfig) {
      return displayQuestions;
    }

    const selectedContact = answers[contactFollowUpConfig.contactQuestionKey];
    const selectedValue =
      typeof selectedContact === "string" ? selectedContact : "";
    const activeFollowUpKey =
      contactFollowUpConfig.followUpByOption[selectedValue];

    return displayQuestions.filter((question) => {
      const match = Object.values(
        contactFollowUpConfig.followUpByOption,
      ).includes(question.dataKey);
      if (!match) {
        return true;
      }
      return question.dataKey === activeFollowUpKey;
    });
  }, [answers, contactFollowUpConfig, displayQuestions]);

  useEffect(() => {
    setAnswers(initialAnswers);
    setErrors({});
    setSubmitError(null);
    setSubmissionResult(null);
  }, [initialAnswers]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }
    const validation: Record<string, string> = {};

    visibleQuestions.forEach((question) => {
      const value = answers[question.dataKey];
      if (!question.required && question.type !== "EMAIL") {
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
    if (Object.keys(validation).length > 0) {
      return;
    }

    const nameAnswer = answers[contactNameKey];
    const emailAnswer = answers[contactEmailKey];
    const preparedAnswers: Record<string, string | string[]> = {};

    visibleQuestions.forEach((question) => {
      const value = answers[question.dataKey];
      if (question.type === "IMAGE_UPLOAD") {
        return;
      }
      if (typeof value === "string") {
        preparedAnswers[question.dataKey] = value.trim();
        return;
      }
      if (Array.isArray(value)) {
        preparedAnswers[question.dataKey] = value;
      }
    });

    const imageQuestion = visibleQuestions.find(
      (question) =>
        question.type === "IMAGE_UPLOAD" &&
        answers[question.dataKey] instanceof File,
    );
    const imageFile = imageQuestion
      ? (answers[imageQuestion.dataKey] as File)
      : null;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const response = await submitFlowSubmission(
        flow.slug,
        {
          name: typeof nameAnswer === "string" ? nameAnswer.trim() : "",
          email: typeof emailAnswer === "string" ? emailAnswer.trim() : "",
          answers: preparedAnswers,
          imageKey: imageQuestion?.dataKey,
          campaignCode: campaignCode ?? undefined,
        },
        imageFile,
      );
      setSubmissionResult(response);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Não foi possível enviar suas respostas.";
      setSubmitError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateAnswer = (question: FlowQuestion, value: AnswerValue) => {
    setAnswers((current) => {
      const next = {
        ...current,
        [question.dataKey]: value,
      };

      if (
        contactFollowUpConfig?.contactQuestionKey === question.dataKey &&
        typeof value === "string"
      ) {
        Object.entries(contactFollowUpConfig.followUpByOption).forEach(
          ([option, key]) => {
            if (option !== value) {
              next[key] = "";
            }
          },
        );
      }

      return next;
    });
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

  if (submissionResult) {
    return (
      <ThankYouPanel
        name={submissionResult.name}
        email={submissionResult.email}
      />
    );
  }

  return (
    <form className="flow-form" onSubmit={handleSubmit} noValidate>
      {submitError ? <div className="error-banner">{submitError}</div> : null}

      <div className="flow-form-card">
        <ol className="flow-question-list">
          {visibleQuestions.map((question, index) => (
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

        <button type="submit" className="submit-button" disabled={isSubmitting}>
          {isSubmitting ? "Enviando respostas..." : "Enviar respostas"}
        </button>
      </div>
    </form>
  );
}

function enhanceQuestions(questions: FlowQuestion[]) {
  const normalized = [...questions];

  const emailIndex = normalized.findIndex(
    (question) => question.type === "EMAIL",
  );
  let contactEmailKey: string;
  if (emailIndex === -1) {
    const emailQuestion: FlowQuestion = {
      title: "Qual é o seu e-mail?",
      dataKey: "email",
      type: "EMAIL",
      required: true,
      description: "Precisamos do seu e-mail para retornar o resultado.",
      placeholder: "voce@email.com",
      options: [],
    };
    normalized.unshift(emailQuestion);
    contactEmailKey = emailQuestion.dataKey;
  } else {
    const question = normalized[emailIndex];
    contactEmailKey = question.dataKey;
    if (!question.required) {
      normalized[emailIndex] = { ...question, required: true };
    }
  }

  const nameIndex = normalized.findIndex(isNameQuestion);
  let contactNameKey: string;
  if (nameIndex === -1) {
    const nameQuestion: FlowQuestion = {
      title: "Qual é o seu nome?",
      dataKey: "nome",
      type: "TEXT",
      required: true,
      description: "Assim conseguimos personalizar nossa resposta.",
      placeholder: "Seu nome",
      options: [],
    };
    normalized.unshift(nameQuestion);
    contactNameKey = nameQuestion.dataKey;
  } else {
    const question = normalized[nameIndex];
    contactNameKey = question.dataKey;
    if (!question.required) {
      normalized[nameIndex] = { ...question, required: true };
    }
  }

  return { questions: normalized, contactEmailKey, contactNameKey };
}

function isNameQuestion(question: FlowQuestion) {
  if (question.type !== "TEXT" && question.type !== "TEXTAREA") {
    return false;
  }
  const key = question.dataKey.toLowerCase();
  const title = question.title.toLowerCase();
  return (
    key.includes("nome") ||
    key.includes("name") ||
    title.includes("nome") ||
    title.includes("name")
  );
}

interface ContactFollowUpConfig {
  contactQuestionKey: string;
  followUpByOption: Record<string, string>;
}

function resolveContactFollowUpConfig(
  questions: FlowQuestion[],
): ContactFollowUpConfig | null {
  const contactQuestion = questions.find(
    (question) =>
      question.type === "SINGLE_CHOICE" &&
      question.dataKey.toLowerCase() === "forma_contato",
  );

  if (!contactQuestion) {
    return null;
  }

  const keyByOption = new Map<string, string>([
    ["instagram", "instagram"],
    ["whatsapp", "whatsapp"],
    ["telefone", "telefone"],
    ["phone", "telefone"],
  ]);

  const followUpByOption: Record<string, string> = {};
  contactQuestion.options.forEach((option) => {
    const normalizedOption = option.trim().toLowerCase();
    const expectedKey = keyByOption.get(normalizedOption);
    if (!expectedKey) {
      return;
    }

    const followUpQuestion = questions.find(
      (question) => question.dataKey.toLowerCase() === expectedKey,
    );
    if (followUpQuestion) {
      followUpByOption[option] = followUpQuestion.dataKey;
    }
  });

  return Object.keys(followUpByOption).length > 0
    ? { contactQuestionKey: contactQuestion.dataKey, followUpByOption }
    : null;
}

function ThankYouPanel({ name, email }: { name: string; email: string }) {
  return (
    <div className="thank-you-card">
      <h2>Respostas enviadas!</h2>
      <p>
        Obrigado, {name || "cliente"}. Recebemos suas respostas e em breve
        entraremos em contato pelo e-mail
        <strong> {email}</strong>.
      </p>
      <p>Você pode fechar esta página com segurança.</p>
    </div>
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

function QuestionField({
  index,
  question,
  value,
  error,
  onChange,
  onToggleOption,
}: QuestionFieldProps) {
  return (
    <div className={`flow-question ${error ? "flow-question-error" : ""}`}>
      <label className="flow-question-label">
        <span className="flow-question-number">{index}.</span>
        <span>
          {question.title}
          {question.required ? (
            <span className="required-indicator">*</span>
          ) : null}
        </span>
      </label>
      {question.description ? (
        <p className="flow-question-description">{question.description}</p>
      ) : null}

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

function InputByType({
  question,
  value,
  onChange,
  onToggleOption,
}: InputByTypeProps) {
  const commonProps = {
    name: question.dataKey,
    required: question.required,
    placeholder: question.placeholder ?? undefined,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      onChange(question, event.target.value),
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
            const selected = Array.isArray(value)
              ? value.includes(option)
              : false;
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
