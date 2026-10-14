import { MapPin } from "lucide-react";

function DeliveryAreaSection() {
  return (
    <section className="area-section" id="entrega" aria-labelledby="area-title">
      <div className="landing-container area-layout">
        <div className="area-copy" data-reveal="left">
          <p className="eyebrow"><span /> Sua área de entrega</p>
          <h2 id="area-title">Atenda onde faz sentido <em>pra sua loja.</em></h2>
          <p>
            Defina o ponto da sua loja e o raio de atendimento em quilômetros.
            O cliente digita o CEP ou rua e o sistema confere instantaneamente se o endereço
            está dentro da área antes de fechar o pedido.
          </p>
          <div className="area-note">
            <MapPin size={17} aria-hidden="true" />
            <span>Você no controle da rota. Sem motoqueiro rodando longe sem necessidade.</span>
          </div>
        </div>

        <div className="area-map" data-reveal="right">
          <div className="area-map__header"><span>MAPA_ENTREGA.SYS</span><span>RAIO ATIVO</span></div>
          <svg className="area-map__drawing" viewBox="0 0 480 300" role="img" aria-labelledby="map-title map-description">
            <title id="map-title">Exemplo de raio de entrega</title>
            <desc id="map-description">
              Um ponto de loja dentro de um círculo de atendimento; um endereço fora do círculo fica fora da área.
            </desc>
            <rect width="480" height="300" fill="#f2e5d5" />
            <g fill="none" stroke="#fffaf1" strokeWidth="12" strokeLinecap="round">
              <path d="M-20 80 235 118 510 54" />
              <path d="M-10 223 189 186 500 250" />
              <path d="M105 -15 142 120 95 315" />
              <path d="M350 -15 313 116 376 315" />
            </g>
            <g fill="none" stroke="#d8c8b6" strokeWidth="1.5">
              <path d="M-20 80 235 118 510 54" />
              <path d="M-10 223 189 186 500 250" />
              <path d="M105 -15 142 120 95 315" />
              <path d="M350 -15 313 116 376 315" />
            </g>
            <circle className="area-map__radius" cx="240" cy="151" r="98" fill="#54245f" fillOpacity=".09" stroke="#54245f" strokeWidth="2" strokeDasharray="7 6" />
            <circle cx="240" cy="151" r="20" fill="#54245f" stroke="#fffaf1" strokeWidth="5" />
            <path d="M240 142v18m-8-9h16" stroke="#fffaf1" strokeWidth="2" strokeLinecap="round" />
            <circle cx="179" cy="106" r="8" fill="#9e5a35" stroke="#fffaf1" strokeWidth="3" />
            <circle cx="390" cy="150" r="8" fill="#963e35" stroke="#fffaf1" strokeWidth="3" />
            <rect x="118" y="61" width="92" height="25" rx="3" fill="#fffaf1" />
            <text x="164" y="78" textAnchor="middle" fill="#38251f" fontSize="10" fontFamily="IBM Plex Mono, monospace">DENTRO</text>
            <rect x="349" y="171" width="106" height="25" rx="3" fill="#fffaf1" />
            <text x="402" y="188" textAnchor="middle" fill="#38251f" fontSize="10" fontFamily="IBM Plex Mono, monospace">FORA DA ÁREA</text>
          </svg>
          <div className="area-map__legend">
            <span><i className="legend-dot legend-dot--inside" />Dentro do raio atendido</span>
            <span><i className="legend-dot legend-dot--outside" />Fora do raio (aviso automático)</span>
          </div>
        </div>
      </div>
    </section>
  );
}

export default DeliveryAreaSection;
