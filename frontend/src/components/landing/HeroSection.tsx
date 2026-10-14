import { Check } from "lucide-react";
import RegistrationFlow from "../RegistrationFlow";

function HeroSection() {
  return (
    <section className="landing-hero" aria-labelledby="hero-title">
      <div className="landing-container landing-hero__grid">
        <div className="landing-hero__copy" data-reveal="left">
          <p className="eyebrow"><span /> Mais controle para quem vive da própria loja</p>
          <h1 id="hero-title">Seu delivery pode <em>render mais.</em></h1>
          <p className="landing-hero__lead">
            Divulgue seu cardápio com um link próprio, receba cada pedido organizado e escolha até onde entrega. Você cuida da comida; o Peditto põe o resto em ordem.
          </p>
          <ul className="hero-benefits" data-reveal="stagger">
            <li><Check size={16} aria-hidden="true" /> Sem comissão por pedido</li>
            <li><Check size={16} aria-hidden="true" /> Seu link, sua loja</li>
            <li><Check size={16} aria-hidden="true" /> Entrega na área que você define</li>
          </ul>
          <a className="hero-link" href="#fluxo">Veja como funciona <span aria-hidden="true">↓</span></a>
          <img className="hero-mascot" src="/mascot-chef.png" alt="" aria-hidden="true" />
        </div>

        <div className="hero-form-column" data-reveal="pop3d">
          <div className="hero-signup-card">
            <div className="hero-signup-card__bar">
              <span className="window-dots" aria-hidden="true"><i /><i /><i /></span>
              <span className="hero-signup-card__title">SUA_LOJA.EXE</span>
              <span className="hero-signup-card__badge">30 DIAS GRÁTIS</span>
            </div>
            <div className="hero-signup-card__body">
              <RegistrationFlow appearance="hero" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
