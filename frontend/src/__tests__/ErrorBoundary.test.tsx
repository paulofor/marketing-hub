import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import * as matchers from '@testing-library/jest-dom/matchers';
expect.extend(matchers);
import React from 'react';
import ErrorBoundary from '../app/ErrorBoundary';

function Boom() {
  throw new Error('#310');
  return null;
}

globalThis.fetch = vi.fn(() => Promise.resolve({
  ok: true,
  text: () => Promise.resolve('Rendered more hooks than during the previous render.')
})) as any;

describe('ErrorBoundary', () => {
  it('shows decoded message', async () => {
    render(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>
    );
    expect(await screen.findByText(/Rendered more hooks/)).toBeInTheDocument();
  });
});
