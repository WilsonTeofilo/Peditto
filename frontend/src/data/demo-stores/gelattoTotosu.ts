import coneImage from "../../assets/demo-stores/gelatto-totosu/catalog/casquinha.png";
import comboImage from "../../assets/demo-stores/gelatto-totosu/catalog/combo-sorvetes.png";
import milkshakeImage from "../../assets/demo-stores/gelatto-totosu/catalog/milkshakes.png";
import literImage from "../../assets/demo-stores/gelatto-totosu/catalog/sorvete-1-litro.png";
import twoLitersImage from "../../assets/demo-stores/gelatto-totosu/catalog/sorvete-2-litros.png";
import logo from "../../assets/demo-stores/gelatto-totosu/brand/logo.webp";
import type { StorefrontDemoData } from "./types";

const gelattoTotosu: StorefrontDemoData = {
  store: {
    slug: "gelatto-totosu",
    theme: "original",
    name: "Gelatto Totosu",
    descriptor: "Sorveteria artesanal",
    neighborhood: "Faria Lima · São Paulo — SP",
    foundedYear: 1999,
    deliveryTime: "20–35 min",
    openUntil: "Hoje, até 22h",
    statusLabel: "Gelateria aberta",
    logoUrl: logo,
    introduction: {
      eyebrow: "Gelateria da Asai Ayumi",
      titleLineOne: "Uma pausa",
      titleLineTwo: "mais gostosa.",
      description: "Gelatos artesanais em um ambiente tranquilo na Faria Lima. A dona, Asai Ayumi, é japonesa e recebe a vizinhança desde 1999.",
    },
    menuTitle: "Escolha seu sabor.",
    deliveryTitle: "A Gelatto Totosu entrega aí?",
    deliveryDescription: "Confira a área ilustrativa da loja. A disponibilidade real do endereço é confirmada antes do pedido.",
    footerMessage: "Um bom gelato pede uma pausa sem pressa.",
    bestSellerLabel: "Favorito da casa",
    bestSellerProductId: "casquinha-gelatto",
    highlight: {
      productId: "combo-gelatto",
      eyebrow: "Para dividir",
      title: "Um sabor puxa o outro.",
      description: "Uma seleção de gelatos para provar mais de um sabor.",
      actionLabel: "Ver o combo",
    },
  },
  categories: [
    { id: "all", label: "Tudo", icon: "✳" },
    { id: "gelatos", label: "Gelatos", icon: "◉" },
    { id: "milkshakes", label: "Milkshakes", icon: "◒" },
    { id: "potes", label: "Potes", icon: "▣" },
  ],
  products: [
    { id: "casquinha-gelatto", categoryId: "gelatos", name: "Casquinha da casa", description: "Gelato artesanal servido na hora, com dois sabores à escolha.", price: 12, image: coneImage, imageAlt: "Casquinha de gelato artesanal da Gelatto Totosu", badge: "favorito da casa", addons: [] },
    { id: "combo-gelatto", categoryId: "gelatos", name: "Combo de gelatos", description: "Uma seleção da casa para compartilhar e provar sabores diferentes.", price: 28, image: comboImage, imageAlt: "Seleção de gelatos da Gelatto Totosu", badge: "para dividir", addons: [] },
    { id: "milkshake-gelatto", categoryId: "milkshakes", name: "Milkshake cremoso", description: "Gelato batido na hora, com calda e chantilly.", price: 18, image: milkshakeImage, imageAlt: "Milkshakes artesanais da Gelatto Totosu", addons: [] },
    { id: "pote-1l-gelatto", categoryId: "potes", name: "Pote de gelato · 1 litro", description: "Leve para casa um litro do seu sabor preferido.", price: 32, image: literImage, imageAlt: "Pote de um litro de gelato artesanal", addons: [] },
    { id: "pote-2l-gelatto", categoryId: "potes", name: "Pote de gelato · 2 litros", description: "Dois litros para servir a família ou guardar no congelador.", price: 40, image: twoLitersImage, imageAlt: "Pote de dois litros de gelato artesanal", addons: [] },
  ],
};

export default gelattoTotosu;
