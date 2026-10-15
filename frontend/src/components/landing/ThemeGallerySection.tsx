import { ArrowRight } from "lucide-react";
import { demoThemePreviews } from "../../data/demo-stores/themePreviews";

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
          {demoThemePreviews.map((option) => (
            <a
              className={`theme-option theme-option--${option.themeId}`}
              data-reveal="pop3d"
              href={`/loja/${option.storeSlug}`}
              key={option.themeId}
              aria-label={`${option.themeName}: abrir a loja fictícia ${option.storeName}.`}
            >
              <span className="theme-option__surface">
                <span className="theme-option__preview">
                  <span className="theme-option__window-bar"><i /><i /><i /><span>VITRINE.PEDITTO</span></span>
                  <img className="theme-option__logo" src={option.logo} alt={option.logoAlt} loading="lazy" decoding="async" />
                  <span className="theme-option__caption">{option.themeName}</span>
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
