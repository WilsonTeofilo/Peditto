import brownieImage from "../../assets/demo-stores/muru-muru-coffee/catalog/brownie.png";
import cafeComLeiteImage from "../../assets/demo-stores/muru-muru-coffee/catalog/cafe-com-leite.png";
import cafeChocolateImage from "../../assets/demo-stores/muru-muru-coffee/catalog/cafe-sabor-chocolate.png";
import caramelMacchiatoImage from "../../assets/demo-stores/muru-muru-coffee/catalog/caramel-macchiato-card.webp";
import coxinhaImage from "../../assets/demo-stores/muru-muru-coffee/catalog/coxinha.png";
import croissantImage from "../../assets/demo-stores/muru-muru-coffee/catalog/croissant.png";
import icedCoffeeImage from "../../assets/demo-stores/muru-muru-coffee/catalog/iced-coffee-card.webp";
import logo from "../../assets/demo-stores/muru-muru-coffee/brand/wordmark.webp";
import type { StorefrontDemoData } from "./types";

const muruMuruCoffee: StorefrontDemoData = {
  store: {
    slug: "muru-muru-coffee",
    theme: "natural",
    name: "Muru Muru Coffee",
    descriptor: "Cafeteria e cozinha natural",
    neighborhood: "Bairro Pilar · Ouro Preto — MG",
    foundedYear: 1988,
    deliveryTime: "15–30 min",
    openUntil: "Hoje, até 20h",
    statusLabel: "Aberto para pedidos",
    logoUrl: logo,
    logoPresentation: "wordmark",
    introduction: {
      eyebrow: "Desde 1988, no bairro Pilar",
      titleLineOne: "Receitas de família",
      titleLineTwo: "feitas com cuidado.",
      description: "Fundada por Alberto José, a Muru Muru é cuidada hoje por Alberto Júnior. Na cozinha, Wilson Teófilo aprimora as receitas antigas da família.",
    },
    menuTitle: "Passe para escolher.",
    deliveryTitle: "A Muru Muru atende seu endereço?",
    deliveryDescription: "Confira a área ilustrativa da cafeteria. A disponibilidade real do endereço é confirmada antes do pedido.",
    footerMessage: "Uma história de família, café e receitas aprimoradas desde 1988.",
    bestSellerLabel: "Queridinho do café",
    bestSellerProductId: "caramel-macchiato",
    highlight: {
      productId: "croissant-muru",
      eyebrow: "Saiu do forno",
      title: "Café passado. Croissant quentinho.",
      description: "Uma dupla simples para acompanhar a sua pausa.",
      actionLabel: "Ver acompanhamento",
    },
  },
  categories: [
    { id: "all", label: "Tudo", icon: "✳" },
    { id: "cafes", label: "Cafés", icon: "◉" },
    { id: "gelados", label: "Gelados", icon: "◒" },
    { id: "acompanhamentos", label: "Acompanhamentos", icon: "▣" },
    { id: "salgados", label: "Salgados", icon: "✿" },
  ],
  products: [
    { id: "caramel-macchiato", categoryId: "cafes", name: "Caramel macchiato", description: "Café espresso, leite vaporizado e caramelo da casa.", price: 16, image: caramelMacchiatoImage, imageAlt: "Caramel macchiato da Muru Muru Coffee", imagePresentation: "portrait", badge: "queridinho", addons: [] },
    { id: "cafe-chocolate", categoryId: "cafes", name: "Café com chocolate", description: "Espresso com chocolate cremoso e leite vaporizado.", price: 14, image: cafeChocolateImage, imageAlt: "Café com chocolate da Muru Muru Coffee", addons: [] },
    { id: "cafe-com-leite", categoryId: "cafes", name: "Café com leite", description: "Espresso equilibrado com leite vaporizado.", price: 10, image: cafeComLeiteImage, imageAlt: "Café com leite da Muru Muru Coffee", addons: [] },
    { id: "iced-coffee", categoryId: "gelados", name: "Iced coffee", description: "Café gelado com leite e gelo, preparado na hora.", price: 15, image: icedCoffeeImage, imageAlt: "Café gelado da Muru Muru Coffee", imagePresentation: "portrait", addons: [] },
    { id: "croissant-muru", categoryId: "acompanhamentos", name: "Croissant da casa", description: "Massa folhada, dourada por fora e macia por dentro.", price: 13, image: croissantImage, imageAlt: "Croissant da Muru Muru Coffee", badge: "feito hoje", addons: [] },
    { id: "brownie-muru", categoryId: "acompanhamentos", name: "Brownie de chocolate", description: "Brownie intenso com casquinha fina e interior macio.", price: 12, image: brownieImage, imageAlt: "Brownie de chocolate da Muru Muru Coffee", addons: [] },
    { id: "coxinha-muru", categoryId: "salgados", name: "Coxinha de frango", description: "Massa macia, recheio de frango e tempero da casa.", price: 11, image: coxinhaImage, imageAlt: "Coxinha de frango da Muru Muru Coffee", addons: [] },
  ],
};

export default muruMuruCoffee;
