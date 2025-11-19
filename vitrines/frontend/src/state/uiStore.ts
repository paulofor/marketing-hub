import { create } from 'zustand'

type UiStore = {
  navigationOpen: boolean
  toggleNavigation: () => void
}

export const useUiStore = create<UiStore>((set) => ({
  navigationOpen: true,
  toggleNavigation: () => set((state) => ({ navigationOpen: !state.navigationOpen }))
}))
