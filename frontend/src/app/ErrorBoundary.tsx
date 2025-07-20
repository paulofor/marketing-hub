import React from 'react';
import { decodeInvariant } from './decodeInvariant';

interface Props {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface State {
  error: Error | null;
  decoded: string | null;
}

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { error: null, decoded: null };

  static getDerivedStateFromError(error: Error): State {
    return { error, decoded: null };
  }

  async componentDidCatch(error: Error, info: React.ErrorInfo) {
    const decoded = await decodeInvariant(error);
    this.setState({ error, decoded });
    if ((window as any).__REACT_DEVTOOLS_GLOBAL_HOOK__) {
      console.error(error);
    }
    if (import.meta.env.VITE_SENTRY_DSN && (window as any).Sentry) {
      (window as any).Sentry.captureException(error);
    }
  }

  render() {
    if (this.state.error) {
      const message = this.state.decoded || this.state.error.message;
      return this.props.fallback ?? <pre>{message}</pre>;
    }
    return this.props.children;
  }
}
