import donaErza from "./donaErza";
import gelattoTotosu from "./gelattoTotosu";
import muruMuruCoffee from "./muruMuruCoffee";
import type { DemoStoreSlug } from "./slugs";
import type { StorefrontDemoData } from "./types";

const demoStores: Record<DemoStoreSlug, StorefrontDemoData> = {
  "gelatto-totosu": gelattoTotosu,
  "restaurante-dona-erza": donaErza,
  "muru-muru-coffee": muruMuruCoffee,
};

export function getDemoStore(slug: string | undefined): StorefrontDemoData | undefined {
  return slug && Object.hasOwn(demoStores, slug) ? demoStores[slug as DemoStoreSlug] : undefined;
}

export type { StoreCategory, StoreProduct, StorefrontDemo, StorefrontDemoData } from "./types";
