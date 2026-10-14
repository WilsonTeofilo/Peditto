const messages = [
  "Cardápio digital no seu link",
  "Pedidos organizados na hora",
  "Área de entrega configurável",
  "Sem comissão por pedido",
  "30 dias de teste",
  "Sem app pra instalar",
];

function TickerRibbon() {
  const doubled = [...messages, ...messages];
  return (
    <div className="ticker-ribbon" aria-hidden="true">
      <div className="ticker-ribbon__track">
        {doubled.map((msg, i) => (
          <span key={i}>{msg}</span>
        ))}
      </div>
    </div>
  );
}

export default TickerRibbon;
