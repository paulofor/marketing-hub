import { ButtonHTMLAttributes } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: string;
}

export default function Button({ className = "btn btn-primary", variant, ...props }: ButtonProps) {
  return <button className={className} {...props} />;
}
