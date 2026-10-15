import gelattoLogo from "../../assets/demo-stores/gelatto-totosu/brand/logo.webp";
import gelattoMascotIdle from "../../assets/demo-stores/gelatto-totosu/brand/mascot/idle.png";
import gelattoMascotHappy from "../../assets/demo-stores/gelatto-totosu/brand/mascot/happy.png";
import erzaLogo from "../../assets/demo-stores/restaurante-dona-erza/brand/logo.webp";
import erzaMascotIdle from "../../assets/demo-stores/restaurante-dona-erza/brand/mascot/idle.png";
import erzaMascotHappy from "../../assets/demo-stores/restaurante-dona-erza/brand/mascot/happy.png";
import muruLogo from "../../assets/demo-stores/muru-muru-coffee/brand/logo.webp";
import muruMascotIdle from "../../assets/demo-stores/muru-muru-coffee/brand/mascot/idle.png";
import muruMascotHappy from "../../assets/demo-stores/muru-muru-coffee/brand/mascot/happy.png";
import type { StorefrontDemo } from "./types";
import type { DemoStoreSlug } from "./slugs";

export type DemoThemePreview = {
  themeId: StorefrontDemo["theme"];
  storeSlug: DemoStoreSlug;
  themeName: string;
  storeName: string;
  category: string;
  description: string;
  logo: string;
  logoAlt: string;
  mascotIdle: string;
  mascotHappy: string;
};

export const demoThemePreviews: readonly DemoThemePreview[] = [
  {
    themeId: "original",
    storeSlug: "gelatto-totosu",
    themeName: "Original Peditto",
    storeName: "Gelatto Totosu",
    category: "Sorvetes artesanais",
    description: "Roxo ameixa, amarelo quente e creme — a identidade original da Peditto.",
    logo: gelattoLogo,
    logoAlt: "Logo da sorveteria fictícia Gelatto Totosu",
    mascotIdle: gelattoMascotIdle,
    mascotHappy: gelattoMascotHappy,
  },
  {
    themeId: "fast-food",
    storeSlug: "restaurante-dona-erza",
    themeName: "Fast food",
    storeName: "Restaurante Dona Erza",
    category: "Restaurante e confeitaria",
    description: "Vermelho, amarelo e creme para uma vitrine marcante de delivery.",
    logo: erzaLogo,
    logoAlt: "Logo do Restaurante Dona Erza",
    mascotIdle: erzaMascotIdle,
    mascotHappy: erzaMascotHappy,
  },
  {
    themeId: "natural",
    storeSlug: "muru-muru-coffee",
    themeName: "Café e natural",
    storeName: "Muru Muru Coffee",
    category: "Cafeteria e cozinha natural",
    description: "Verde-floresta, creme natural e detalhes de café.",
    logo: muruLogo,
    logoAlt: "Logo da cafeteria fictícia Muru Muru Coffee",
    mascotIdle: muruMascotIdle,
    mascotHappy: muruMascotHappy,
  },
];
