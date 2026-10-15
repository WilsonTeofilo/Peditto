export const demoStoreSlugs = [
  "gelatto-totosu",
  "restaurante-dona-erza",
  "muru-muru-coffee",
] as const;

export type DemoStoreSlug = (typeof demoStoreSlugs)[number];
