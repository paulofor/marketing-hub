import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import MediaListPage from './MediaListPage';
import { vi } from 'vitest';
import axios from 'axios';

vi.mock('axios');

describe('MediaListPage', () => {
  it('renders table', async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <MediaListPage />
        </BrowserRouter>
      </QueryClientProvider>
    );
    expect(await screen.findByText(/New Media/)).toBeTruthy();
  });
});
