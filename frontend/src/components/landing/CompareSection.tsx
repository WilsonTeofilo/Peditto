const rows = [
  { feature: "Comissão por pedido", peditto: "Sem comissão", platforms: "Varia por serviço e plano", whatsapp: "Sem comissão por pedido" },
  { feature: "Cardápio", peditto: "Sua loja no link", platforms: "No app e/ou por link", whatsapp: "Link compartilhado" },
  { feature: "Organização de pedidos", peditto: "Painel de pedidos", platforms: "Recursos do serviço", whatsapp: "Conversas e confirmações" },
  { feature: "Área de entrega", peditto: "Raio configurável", platforms: "Varia por serviço", whatsapp: "Combinação manual" },
  { feature: "Identidade da loja", peditto: "Sua identidade", platforms: "Regras da plataforma", whatsapp: "Seu perfil" },
  { feature: "App para o cliente", peditto: "Não precisa instalar", platforms: "Depende do serviço", whatsapp: "WhatsApp instalado" },
  { feature: "Cobrança do sistema", peditto: "Assinatura", platforms: "Varia por serviço e plano", whatsapp: "Sem assinatura do Peditto" },
];

function renderCell(val: string) {
  if (val === "check") return <span className="check" title="Disponível">✓</span>;
  if (val === "cross") return <span className="cross" title="Não possui">✕</span>;
  if (val === "partial") return <span className="partial" title="Limitado ou condicional">~</span>;
  return val;
}

function CompareSection() {
  return (
    <section className="compare-section" id="comparativo" aria-labelledby="compare-title">
      <div className="landing-container">
        <div className="section-heading" data-reveal="up">
          <p className="eyebrow"><span />Comparativo direto</p>
          <h2 id="compare-title">Peditto, plataformas tradicionais<br /><em>ou mensagens avulsas?</em></h2>
          <p>O que cada formato entrega na prática pro caixa e pra rotina da sua operação.</p>
        </div>
        <div className="compare-table-wrap" data-reveal="pop">
          <table className="compare-table">
            <thead>
              <tr>
                <th scope="col">Recurso</th>
                <th scope="col">Peditto</th>
                <th scope="col">Grandes Plataformas</th>
                <th scope="col">WhatsApp / Mensagens</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.feature}>
                  <td>{row.feature}</td>
                  <td>{renderCell(row.peditto)}</td>
                  <td>{renderCell(row.platforms)}</td>
                  <td>{renderCell(row.whatsapp)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

export default CompareSection;
