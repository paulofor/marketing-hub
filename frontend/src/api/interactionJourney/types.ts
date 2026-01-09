export interface InteractionJourneyElement {
  id?: number;
  label: string;
  type?: string;
  notes?: string;
  orderIndex?: number;
  children: InteractionJourneyElement[];
}

export interface InteractionJourneyStep {
  id?: number;
  title: string;
  description?: string;
  orderIndex?: number;
  elements: InteractionJourneyElement[];
}

export interface InteractionJourney {
  id?: number;
  name: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  steps: InteractionJourneyStep[];
}
