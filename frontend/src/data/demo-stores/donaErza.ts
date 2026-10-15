import boloDePoteImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/bolo-de-pote.webp";
import brigadeirosImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/kit-brigadeiros.webp";
import cheesecakeImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/cheesecake-morango.webp";
import erzaLogo from "../../assets/demo-stores/restaurante-dona-erza/brand/logo.webp";
import hamburgerImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/hamburguer-gourmet.webp";
import mostOrderedImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/combo-mais-pedido.webp";
import tortaImage from "../../assets/demo-stores/restaurante-dona-erza/catalog/optimized/torta-holandesa.webp";
import type { StorefrontDemoData } from "./types";

const donaErza: StorefrontDemoData = {
  store: {
    slug: "restaurante-dona-erza",
    theme: "fast-food",
    name: "Restaurante Dona Erza",
    descriptor: "Restaurante e confeitaria",
    neighborhood: "Parque Grajaú · Grajaú — SP",
    foundedYear: 2025,
    deliveryTime: "30–45 min",
    openUntil: "Hoje, até 22h",
    statusLabel: "Cozinha aberta para pedidos",
    logoUrl: erzaLogo,
    introduction: {
      eyebrow: "Restaurante e confeitaria",
      titleLineOne: "Sabor de casa.",
      titleLineTwo: "Feito no Grajaú.",
      description: "Hambúrgueres caprichados, bolos e doces preparados pela Dona Erza.",
    },
    menuTitle: "Escolha com calma.",
    deliveryTitle: "Até onde a Dona Erza entrega?",
    deliveryDescription: "Veja a área aproximada no mapa. A confirmação do endereço e da entrega acontece no servidor antes do pedido.",
    footerMessage: "Comida feita por gente, pedida pelo Peditto.",
    bestSellerLabel: "Mais pedido",
    bestSellerProductId: "combo-dona-erza",
    highlight: {
      productId: "cheesecake-morango",
      eyebrow: "Doce da vitrine",
      title: "Um pedaço que pede outro.",
      description: "Cheesecake de morango com cobertura feita na casa.",
      actionLabel: "Ver sobremesa",
    },
  },
  categories: [
    { id: "all", label: "Tudo", icon: "✳" },
    { id: "combos", label: "Combos", icon: "▣" },
    { id: "hamburgueres", label: "Hambúrgueres", icon: "◒" },
    { id: "bolos", label: "Bolos", icon: "◉" },
    { id: "doces", label: "Doces", icon: "✿" },
  ],
  products: [
    {
      id: "combo-dona-erza",
      categoryId: "combos",
      name: "Combo Dona Erza",
      description: "Dois hambúrgueres, batata dourada e molhos da casa.",
      price: 42,
      image: mostOrderedImage,
      imageAlt: "Combo de hambúrgueres com batatas fritas",
      badge: "mais pedido",
      addons: [
        { id: "molho-extra", name: "Molho da casa extra", price: 3 },
        { id: "batata-extra", name: "Porção extra de batata", price: 8 },
      ],
    },
    {
      id: "hamburguer-gourmet",
      categoryId: "hamburgueres",
      name: "Hambúrguer gourmet",
      description: "Hambúrguer artesanal com molho cremoso da casa.",
      price: 32,
      image: hamburgerImage,
      imageAlt: "Hambúrguer artesanal com molho cremoso",
      addons: [
        { id: "cheddar-extra", name: "Cheddar extra", price: 4 },
        { id: "bacon-extra", name: "Bacon crocante", price: 5 },
      ],
    },
    {
      id: "cheesecake-morango",
      categoryId: "doces",
      name: "Cheesecake de morango",
      description: "Cheesecake cremoso com morangos e cobertura artesanal.",
      price: 16,
      image: cheesecakeImage,
      imageAlt: "Cheesecake de morango sendo servido",
      badge: "feito na casa",
      addons: [],
    },
    { id: "torta-holandesa", categoryId: "doces", name: "Torta holandesa", description: "Camadas cremosas com cobertura de chocolate.", price: 15, image: tortaImage, imageAlt: "Fatia de torta holandesa com chocolate", addons: [] },
    { id: "bolo-de-pote", categoryId: "bolos", name: "Bolo de pote de morango", description: "Creme, bolo macio e morangos em camadas.", price: 12, image: boloDePoteImage, imageAlt: "Bolo de pote com morango e creme", addons: [] },
    { id: "kit-brigadeiros", categoryId: "doces", name: "Kit de brigadeiros · 4 unidades", description: "Quatro brigadeiros artesanais com sabores variados.", price: 18, image: brigadeirosImage, imageAlt: "Caixa com quatro brigadeiros artesanais", addons: [] },
  ],
};

export default donaErza;
