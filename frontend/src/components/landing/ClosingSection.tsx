import { ArrowRight } from "lucide-react";

function ClosingSection() {
  return (
    <section className="closing-section">
      <div className="landing-container closing-panel" data-reveal="pop">
        <div className="closing-panel__content">
          <p className="eyebrow"><span /> Se fizer sentido para sua loja</p>
          <h2>Conheça o Peditto<br />no seu tempo.</h2>
          <p>Monte seu cardápio, configure sua área de entrega e teste o fluxo por 30 dias. Uma loja por CPF ou CNPJ.</p>
        </div>
        <div className="closing-panel__action">
          <img className="closing-panel__mascot" src="/peditto-mascot-avatar.png" alt="" aria-hidden="true" />
          <a className="landing-button landing-button--gold" href="/criar-conta">
            Começar meu teste <ArrowRight size={17} aria-hidden="true" />
          </a>
        </div>
      </div>
    </section>
  );
}

export default ClosingSection;
