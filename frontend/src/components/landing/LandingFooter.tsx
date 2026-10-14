function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="landing-container landing-footer__inner">
        <a className="landing-brand landing-brand--footer" href="/" aria-label="Peditto, página inicial">
          <img src="/peditto-mascot-avatar.png" alt="" />
          <span>peditto<span className="landing-brand__dot">.</span></span>
        </a>
        <span>Cardápio, pedidos e entrega para sua loja.</span>
        <a href="/entrar">Entrar na minha loja</a>
      </div>
    </footer>
  );
}

export default LandingFooter;