import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { ErrorBoundary } from '../app/ErrorBoundary';

function Bomb() {
  throw new Error('Minified React error #310');
  return null;
}

describe('ErrorBoundary', () => {
  it('renders decoded error message', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({
      ok: true,
      text: () => Promise.resolve('full error message'),
    })) as any);
    render(
      <ErrorBoundary>
        <Bomb />
      </ErrorBoundary>
    );
    expect(await screen.findByText('full error message')).toBeTruthy();
  });
});
