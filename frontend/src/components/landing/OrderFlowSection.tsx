const steps = [
  {
    number: "01",
    badge: "CARDÁPIO",
    title: "Compartilhe seu link",
    text: "O cliente acessa pelo navegador do celular, sem precisar baixar aplicativo pesado. Cardápio limpo, rápido e com a sua identidade.",
    mascot: "/link.png",
    alt: "Mascote apresentando o link do cardápio digital",
  },
  {
    number: "02",
    badge: "COZINHA",
    title: "Receba o pedido completo",
    text: "Itens, adicionais, observações e endereço de entrega chegam organizados no painel. Nada fica perdido em conversas soltas.",
    mascot: "/mascot-cooking.png",
    alt: "Mascote preparando o pedido",
  },
  {
    number: "03",
    badge: "ENTREGA",
    title: "Despache e acompanhe",
    text: "Atualize o status até a saída para entrega. O cliente sabe quando o pedido saiu sem precisar ficar perguntando no WhatsApp.",
    mascot: "/mascot-delivery.png",
    alt: "Mascote em rota de entrega",
  },
];

function OrderFlowSection() {
  return (
    <section className="flow-section" id="fluxo" aria-labelledby="flow-title">
      <div className="landing-container">
        <div className="section-heading" data-reveal="up">
          <p className="eyebrow"><span />Um fluxo direto</p>
          <h2 id="flow-title">Cada pedido sabe<br /><em>qual é o próximo passo.</em></h2>
          <p>Do link que você divulga até a entrega na porta do cliente, tudo segue uma linha contínua.</p>
        </div>
        <ol className="flow-list" data-reveal="stagger">
          {steps.map((step) => (
            <li className="flow-step" data-reveal="pop" key={step.number}>
              <div className="flow-step__top">
                <span className="flow-step__number">{step.number}</span>
                <span className="flow-step__badge">{step.badge}</span>
              </div>
              <div className="flow-step__mascot-frame">
                <img
                  className="flow-step__mascot"
                  src={step.mascot}
                  alt={step.alt}
                  loading="lazy"
                />
              </div>
              <div className="flow-step__body">
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

export default OrderFlowSection;
