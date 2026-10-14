import { MessageSquareX, Percent, Clock } from "lucide-react";

const pains = [
  {
    icon: <MessageSquareX size={20} aria-hidden="true" />,
    title: "Mensagens caóticas",
    text: "Pedidos misturados em conversas de WhatsApp, comprovantes perdidos e nenhum histórico confiável do que foi vendido no dia.",
  },
  {
    icon: <Percent size={20} aria-hidden="true" />,
    title: "Custos difíceis de prever",
    text: "Comissões e planos variam de serviço para serviço. Sem olhar a taxa real, fica difícil saber quanto sobra no fim do mês.",
  },
  {
    icon: <Clock size={20} aria-hidden="true" />,
    title: "Anotação no improviso",
    text: "Caderno com endereço incompleto, taxa de entrega cobrada no achismo e cliente cobrando status da comida a cada 5 minutos.",
  },
];

function PainSection() {
  return (
    <section className="pain-section" id="problemas" aria-labelledby="pain-title">
      <div className="landing-container">
        <div className="section-heading" data-reveal="up">
          <p className="eyebrow"><span />O problema real</p>
          <h2 id="pain-title">O delivery cresce,<br /><em>o controle não acompanha.</em></h2>
          <p>Três gargalos que travam o lucro e a tranquilidade de quem opera delivery independente.</p>
        </div>
        <div className="pain-grid" data-reveal="stagger">
          {pains.map((p) => (
            <article className="pain-card" key={p.title} data-reveal="pop">
              <div className="pain-card__icon">{p.icon}</div>
              <h3>{p.title}</h3>
              <p>{p.text}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default PainSection;
