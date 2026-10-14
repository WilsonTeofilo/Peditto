import { ArrowDown, ClipboardList, MapPin } from "lucide-react";

function OrderPreview() {
  return (
    <div className="hero-preview-float" data-reveal="pop">
      <div className="order-preview" aria-label="Prévia ilustrativa do painel de pedidos">
        <div className="order-preview__bar">
          <span className="window-dots" aria-hidden="true"><i /><i /><i /></span>
          <span>PEDITTO / BALCAO.EXE</span>
          <span className="order-preview__sample">PRÉVIA</span>
        </div>
        <div className="order-preview__heading">
          <div>
            <span className="micro-label">Fluxo de pedidos</span>
            <h2>Pedidos da loja</h2>
          </div>
          <span className="order-preview__date">EXEMPLO</span>
        </div>
        <article className="order-ticket">
          <div className="order-ticket__icon"><ClipboardList size={17} aria-hidden="true" /></div>
          <div className="order-ticket__copy">
            <strong>Pedido recebido</strong>
            <span>2 itens · observação: sem cebola</span>
          </div>
          <span className="order-ticket__status">NOVO</span>
        </article>
        <article className="order-ticket order-ticket--warm">
          <div className="order-ticket__icon"><MapPin size={17} aria-hidden="true" /></div>
          <div className="order-ticket__copy">
            <strong>Área de entrega</strong>
            <span>Endereço de exemplo · dentro do raio</span>
          </div>
          <span className="order-ticket__status order-ticket__status--muted">OK</span>
        </article>
        <div className="order-preview__footer">
          <span>Etapa do pedido</span><span>Em preparo</span>
        </div>
      </div>
    </div>
  );
}

function HeroSection() {
  return (
    <section className="landing-hero-wrap" aria-labelledby="hero-title">
      <div className="landing-container landing-hero">
        <div className="landing-hero__copy" data-reveal="left">
          <p className="eyebrow"><span /> Delivery independente</p>
          <h1 id="hero-title">Do cardápio à entrega,<br /><em>no ritmo da sua loja.</em></h1>
          <p className="landing-hero__lead">
            Um link para mostrar seu cardápio, organizar pedidos e acompanhar a entrega.
            Você monta a loja e define até onde atende.
          </p>
          <div className="landing-hero__actions">
            <a className="landing-button" href="#fluxo">
              Veja o fluxo de pedidos <ArrowDown size={16} aria-hidden="true" />
            </a>
            <a className="text-link" href="#entrega">Conheça a área de entrega</a>
          </div>
          <p className="trial-note">30 dias para testar <span>·</span> uma loja por CPF ou CNPJ</p>
        </div>
        <div className="hero-scene" data-reveal="right">
          <div className="hero-mascot" aria-hidden="true">
            <img src="/mascot-badge.png" alt="" />
          </div>
          <OrderPreview />
          <span className="hero-scene__caption">UMA PRÉVIA DO FLUXO NO BALCÃO</span>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
