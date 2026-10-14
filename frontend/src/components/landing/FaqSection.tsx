const questions = [
  {
    question: "O Peditto cobra alguma porcentagem por pedido?",
    answer: "Não. Zero comissão. Todo o valor do prato e da entrega vai direto para você. O Peditto funciona com mensalidade fixa após o período de teste.",
  },
  {
    question: "O cliente final precisa baixar algum aplicativo?",
    answer: "Não. O cliente abre o cardápio direto pelo navegador do celular clicando no link da sua bio do Instagram, WhatsApp ou onde você divulgar.",
  },
  {
    question: "Como funciona a definição da taxa e raio de entrega?",
    answer: "Você marca o endereço da sua loja e configura o raio máximo em km e as taxas correspondentes. Endereços fora do raio recebem um aviso claro e o sistema impede pedidos fora da rota.",
  },
  {
    question: "Como funciona o teste gratuito de 30 dias?",
    answer: "Você ganha 30 dias completos para montar o cardápio, testar na rotina real e receber pedidos. Não pedimos cartão de crédito no cadastro.",
  },
  {
    question: "Quantas lojas posso cadastrar no mesmo CPF ou CNPJ?",
    answer: "Uma loja elegível ao trial por documento (CPF ou CNPJ). Cada loja opera de forma 100% isolada, com seus próprios produtos, equipe e relatórios.",
  },
];

function FaqSection() {
  return (
    <section className="faq-section" id="duvidas" aria-labelledby="faq-title">
      <div className="landing-container faq-layout">
        <div className="section-heading" data-reveal="left">
          <p className="eyebrow"><span /> Dúvidas frequentes</p>
          <h2 id="faq-title">Antes de abrir<br /><em>sua loja.</em></h2>
          <p>Tudo o que você precisa saber antes de dar a partida, sem termos técnicos complicados.</p>
        </div>
        <div className="faq-list" data-reveal="stagger">
          {questions.map(({ question, answer }) => (
            <details className="faq-item" data-reveal="up" key={question}>
              <summary>{question}<span aria-hidden="true" /></summary>
              <p>{answer}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

export default FaqSection;