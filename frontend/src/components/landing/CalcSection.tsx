import { useState } from "react";

function CalcSection() {
  const [orders, setOrders] = useState(25);
  const [ticket, setTicket] = useState(40);
  const [rate, setRate] = useState(10);

  const monthly = orders * 30 * ticket * (rate / 100);
  const yearly = monthly * 12;

  const fmt = (v: number) =>
    v.toLocaleString("pt-BR", { style: "currency", currency: "BRL", minimumFractionDigits: 0 });

  return (
    <section className="calc-section" id="calculadora" aria-labelledby="calc-title">
      <div className="landing-container calc-layout">
        <div className="section-heading" data-reveal="left">
          <p className="eyebrow"><span />Faça as contas</p>
          <h2 id="calc-title">Faça a conta<br /><em>com a sua taxa.</em></h2>
          <p>
            Informe uma taxa de exemplo ou use o percentual do seu contrato. A simulação mostra
            quanto esse custo representa no volume de pedidos da sua loja.
          </p>
          <div className="calc-callout" data-reveal="pop">
            <strong>No Peditto, não há comissão por pedido.</strong>
            <span>O plano funciona por assinatura. Confira as condições antes de contratar.</span>
          </div>
        </div>
        <div className="calc-window" data-reveal="right">
          <div className="calc-window__bar">
            <span className="window-dots" aria-hidden="true"><i /><i /><i /></span>
            <span>SIMULADOR_TAXAS.EXE</span>
          </div>
          <div className="calc-window__body">
            <label className="calc-label">
              <span>Pedidos por dia</span>
              <output>{orders} pedidos</output>
            </label>
            <input
              className="calc-slider"
              type="range"
              min={5}
              max={100}
              value={orders}
              onChange={(e) => setOrders(Number(e.target.value))}
              aria-label="Pedidos por dia"
            />

            <label className="calc-label">
              <span>Ticket médio</span>
              <output>{fmt(ticket)}</output>
            </label>
            <input
              className="calc-slider"
              type="range"
              min={15}
              max={150}
              step={5}
              value={ticket}
              onChange={(e) => setTicket(Number(e.target.value))}
              aria-label="Ticket médio"
            />

            <label className="calc-label">
              <span>Taxa para simular</span>
              <output className="calc-label__rate">{rate}%</output>
            </label>
            <input
              className="calc-slider calc-slider--rate"
              type="range"
              min={1}
              max={40}
              step={1}
              value={rate}
              onChange={(e) => setRate(Number(e.target.value))}
                aria-label="Taxa percentual para simular"
            />

            <div className="calc-result">
              <span className="calc-result__label">Comissão retida por mês</span>
              <span className="calc-result__value">{fmt(monthly)}</span>
              <div className="calc-result__yearly">
                <span>Total em 1 ano:</span>
                <strong>{fmt(yearly)}</strong>
              </div>
              <span className="calc-result__note">
                Cenário ilustrativo com taxa de {rate}% · {orders * 30} pedidos/mês · faturamento de {fmt(orders * 30 * ticket)}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export default CalcSection;
