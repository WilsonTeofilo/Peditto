import { ArrowRight } from "lucide-react";
import gelattoLogo from "../../assets/demo-stores/gelatto-totosu/brand/logo.webp";
import gelattoMascotIdle from "../../assets/demo-stores/gelatto-totosu/brand/mascot/idle.png";
import gelattoMascotHappy from "../../assets/demo-stores/gelatto-totosu/brand/mascot/happy.png";
import erzaLogo from "../../assets/demo-stores/restaurante-dona-erza/brand/logo.webp";
import erzaMascotIdle from "../../assets/demo-stores/restaurante-dona-erza/brand/mascot/idle.png";
import erzaMascotHappy from "../../assets/demo-stores/restaurante-dona-erza/brand/mascot/happy.png";
import muruLogo from "../../assets/demo-stores/muru-muru-coffee/brand/logo.webp";
import muruMascotIdle from "../../assets/demo-stores/muru-muru-coffee/brand/mascot/idle.png";
import muruMascotHappy from "../../assets/demo-stores/muru-muru-coffee/brand/mascot/happy.png";

type ThemeOption = {
  id: string;
  slug: string;
  theme: string;
  storeName: string;
  category: string;
  description: string;
  logo: string;
  logoAlt: string;
  mascotIdle: string;
  mascotHappy: string;
};

const options: ThemeOption[] = [
  {
    id: "original",
    slug: "gelatto-totosu",
    theme: "Original Peditto",
    storeName: "Gelatto Totosu",
    category: "Sorvetes artesanais",
    description: "Roxo ameixa, amarelo quente e creme — a identidade original da Peditto.",
    logo: gelattoLogo,
    logoAlt: "Logo da sorveteria fictícia Gelatto Totosu",
    mascotIdle: gelattoMascotIdle,
    mascotHappy: gelattoMascotHappy,
  },
  {
    id: "fast-food",
    slug: "restaurante-dona-erza",
    theme: "Fast food",
    storeName: "Restaurante Dona Erza",
    category: "Restaurante e confeitaria",
    description: "Vermelho, amarelo e creme para uma vitrine marcante de delivery.",
    logo: erzaLogo,
    logoAlt: "Logo do Restaurante Dona Erza",
    mascotIdle: erzaMascotIdle,
    mascotHappy: erzaMascotHappy,
  },
  {
    id: "natural",
    slug: "muru-muru-coffee",
    theme: "Café e natural",
    storeName: "Muru Muru Coffee",
    category: "Cafeteria e cozinha natural",
    description: "Verde-floresta, creme natural e detalhes de café.",
    logo: muruLogo,
    logoAlt: "Logo da cafeteria fictícia Muru Muru Coffee",
    mascotIdle: muruMascotIdle,
    mascotHappy: muruMascotHappy,
  },
];

function ThemeGallerySection() {
  return (
    <section className="theme-gallery-section" id="temas" aria-labelledby="theme-gallery-title">
      <div className="landing-container">
        <div className="section-heading" data-reveal="up">
          <p className="eyebrow"><span /> Escolha um tema para visualizar</p>
          <h2 id="theme-gallery-title">Qual vitrine<br /><em>combina com você?</em></h2>
          <p>Escolha uma vitrine para abrir a loja e explorar o tema por dentro.</p>
        </div>

        <div className="theme-gallery-grid" data-reveal="stagger">
          {options.map((option) => (
            <a
              className={`theme-option theme-option--${option.id}`}
              data-reveal="pop3d"
              href={`/loja/${option.slug}`}
              key={option.id}
              aria-label={`${option.theme}: abrir a loja fictícia ${option.storeName}.`}
            >
              <span className="theme-option__surface">
                <span className="theme-option__preview">
                  <span className="theme-option__window-bar"><i /><i /><i /><span>VITRINE.PEDITTO</span></span>
                  <img className="theme-option__logo" src={option.logo} alt={option.logoAlt} loading="lazy" decoding="async" />
                  <span className="theme-option__caption">{option.theme}</span>
                </span>
                <span className="theme-option__body">
                  <span className="theme-option__store">{option.category}</span>
                  <span className="theme-option__title">{option.storeName}</span>
                  <span className="theme-option__description">{option.description}</span>
                  <span className="theme-option__link">Clique para explorar <ArrowRight size={15} aria-hidden="true" /></span>
                </span>
              </span>
              <span className="theme-option__mascot" aria-hidden="true">
                <img className="theme-option__mascot-idle" src={option.mascotIdle} alt="" loading="lazy" decoding="async" />
                <img className="theme-option__mascot-happy" src={option.mascotHappy} alt="" loading="lazy" decoding="async" />
              </span>
            </a>
          ))}
        </div>

        <p className="theme-gallery-note">
          Cada opção abre uma loja fictícia completa, com cardápio e identidade próprios. Volte para explorar os outros estilos.
        </p>
      </div>
    </section>
  );
}

export default ThemeGallerySection;
