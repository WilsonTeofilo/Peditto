function LandingHeader() {
  return (
    <>
      <a className="skip-link" href="#conteudo">Pular para o conteúdo</a>
      <header className="landing-header">
        <div className="landing-container landing-header__inner">
          <a className="landing-brand" href="/" aria-label="Peditto, página inicial">
            <img src="/peditto-mascot-avatar.png" alt="" />
            <span>peditto<span className="landing-brand__dot">.</span></span>
          </a>
          <nav className="landing-nav" aria-label="Navegação principal">
            <a href="#problemas">Por que</a>
            <a href="#fluxo">Como funciona</a>
            <a href="#entrega">Área de entrega</a>
            <a href="#duvidas">Dúvidas</a>
          </nav>
          <div className="landing-header__actions">
            <a className="landing-login" href="/entrar">Entrar</a>
          </div>
        </div>
      </header>
    </>
  );
}

export default LandingHeader;
