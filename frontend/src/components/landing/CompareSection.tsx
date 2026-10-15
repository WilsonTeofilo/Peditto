import { Check, Minus, X } from "lucide-react";

type Tone = "positive" | "negative" | "depends";
type Result = { tone: Tone; text: string };
type ComparisonRow = {
  feature: string;
  peditto: Result;
  platforms: Result;
  messages: Result;
};

const rows: ComparisonRow[] = [
  {
    feature: "Comissão por pedido",
    peditto: { tone: "positive", text: "Sem comissão por pedido" },
    platforms: { tone: "depends", text: "Taxas variam por serviço" },
    messages: { tone: "positive", text: "Sem comissão" },
  },
  {
    feature: "Cardápio e divulgação",
    peditto: { tone: "positive", text: "Link e identidade da sua loja" },
    platforms: { tone: "depends", text: "Dentro das regras do app" },
    messages: { tone: "depends", text: "Link enviado manualmente" },
  },
  {
    feature: "Organização dos pedidos",
    peditto: { tone: "positive", text: "Pedidos reunidos no painel" },
    platforms: { tone: "positive", text: "Ferramentas do aplicativo" },
    messages: { tone: "negative", text: "Pedidos misturados nas conversas" },
  },
  {
    feature: "Área de entrega",
    peditto: { tone: "positive", text: "Raio definido pela loja" },
    platforms: { tone: "depends", text: "Recursos variam por serviço" },
    messages: { tone: "negative", text: "Endereço conferido manualmente" },
  },
  {
    feature: "Marca da loja",
    peditto: { tone: "positive", text: "Sua loja em primeiro plano" },
    platforms: { tone: "depends", text: "Compartilha espaço com o app" },
    messages: { tone: "positive", text: "Contato direto com o cliente" },
  },
  {
    feature: "Aplicativo para o cliente",
    peditto: { tone: "positive", text: "Abre pelo navegador" },
    platforms: { tone: "depends", text: "Depende da plataforma" },
    messages: { tone: "depends", text: "Precisa usar o mensageiro" },
  },
  {
    feature: "Personalização da vitrine",
    peditto: { tone: "positive", text: "Temas visuais para combinar com sua loja" },
    platforms: { tone: "depends", text: "Aparência definida pela plataforma" },
    messages: { tone: "depends", text: "Visual livre, montagem manual" },
  },
  {
    feature: "Custo para a loja",
    peditto: { tone: "negative", text: "Assinatura mensal" },
    platforms: { tone: "depends", text: "Taxas e planos variam" },
    messages: { tone: "positive", text: "Sem mensalidade do sistema" },
  },
];

const indicators = {
  positive: { label: "Vantagem", Icon: Check },
  negative: { label: "Ponto de atenção", Icon: X },
  depends: { label: "Varia conforme o serviço", Icon: Minus },
} satisfies Record<Tone, { label: string; Icon: typeof Check }>;

function ComparisonResult({ result }: { result: Result }) {
  const { label, Icon } = indicators[result.tone];
  return (
    <span className={`compare-result compare-result--${result.tone}`}>
      <Icon size={15} strokeWidth={2.5} aria-label={label} />
      <span>{result.text}</span>
    </span>
  );
}

function CompareSection() {
  return (
    <section className="compare-section" id="comparativo" aria-labelledby="compare-title">
      <div className="landing-container">
        <div className="section-heading" data-reveal="up">
          <p className="eyebrow"><span /> Comparativo direto</p>
          <h2 id="compare-title">Peditto, plataformas tradicionais<br /><em>ou mensagens avulsas?</em></h2>
          <p>Veja o que muda no custo, na autonomia e no trabalho de cada dia.</p>
        </div>
        <div className="compare-legend" aria-label="Legenda da tabela">
          <span className="compare-result compare-result--positive"><Check size={15} aria-hidden="true" /> Vantagem</span>
          <span className="compare-result compare-result--negative"><X size={15} aria-hidden="true" /> Ponto de atenção</span>
          <span className="compare-result compare-result--depends"><Minus size={15} aria-hidden="true" /> Depende do serviço</span>
        </div>
        <div className="compare-table-wrap" data-reveal="pop3d">
          <table className="compare-table">
            <thead>
              <tr>
                <th scope="col">Na rotina</th>
                <th scope="col">Peditto</th>
                <th scope="col">Plataformas tradicionais</th>
                <th scope="col">WhatsApp / mensagens</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.feature}>
                  <th scope="row">{row.feature}</th>
                  <td><ComparisonResult result={row.peditto} /></td>
                  <td><ComparisonResult result={row.platforms} /></td>
                  <td><ComparisonResult result={row.messages} /></td>
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
