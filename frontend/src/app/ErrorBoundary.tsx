import React from "react";
import { decodeInvariant } from "./decodeInvariant";

interface State {
  hasError: boolean;
  message?: string;
}

export default class ErrorBoundary extends React.Component<React.PropsWithChildren<{}>, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  async componentDidCatch(error: Error) {
    const decoded = await decodeInvariant(error);
    if ((window as any).__REACT_DEVTOOLS_GLOBAL_HOOK__) {
      console.error(error);
    }
    this.setState({ hasError: true, message: decoded || error.message });
  }

  render() {
    if (this.state.hasError) {
      return <div>{this.state.message}</div>;
    }
    return this.props.children;
  }
}
