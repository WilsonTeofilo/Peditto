export type StoreCategory = {
  id: string;
  label: string;
  icon: string;
};

export type ProductAddon = {
  id: string;
  name: string;
  price: number;
};

export type StoreProduct = {
  id: string;
  categoryId: string;
  name: string;
  description: string;
  price: number;
  image: string;
  imageAlt: string;
  imagePresentation?: "portrait";
  badge?: string;
  addons: ProductAddon[];
};

export type StorefrontDemo = {
  slug: string;
  theme: "original" | "fast-food" | "natural";
  name: string;
  descriptor: string;
  neighborhood: string;
  foundedYear: number | null;
  deliveryTime: string;
  openUntil: string;
  statusLabel: string;
  logoUrl: string;
  logoPresentation?: "badge" | "wordmark";
  introduction: {
    eyebrow: string;
    titleLineOne: string;
    titleLineTwo: string;
    description: string;
  };
  menuTitle: string;
  deliveryTitle: string;
  deliveryDescription: string;
  footerMessage: string;
  bestSellerLabel: string;
  bestSellerProductId: string;
  highlight: {
    productId: string;
    eyebrow: string;
    title: string;
    description: string;
    actionLabel: string;
  };
};

export type StorefrontDemoData = {
  store: StorefrontDemo;
  categories: StoreCategory[];
  products: StoreProduct[];
};
