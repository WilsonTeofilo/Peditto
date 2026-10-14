import { useEffect, useState, type FormEvent } from "react";
import { ArrowRight, Check, LockKeyhole } from "lucide-react";

type Channel = "EMAIL" | "SMS";
type Mode = "login" | "register";

async function post<T>(path: string, body: unknown): Promise<T> {
  const csrfResponse = await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
  if (!csrfResponse.ok) throw new Error();
  const csrf = (await csrfResponse.json()) as { headerName: string; token: string };
  const response = await fetch(path, {
    method: "POST", credentials: "same-origin",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error();
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function App() {
  const [mode, setMode] = useState<Mode>(window.location.pathname === "/criar-conta" ? "register" : "login");
  const [step, setStep] = useState("start");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [docType, setDocType] = useState<"CPF" | "CNPJ">("CNPJ");
  const [channel, setChannel] = useState<Channel>("EMAIL");
  const [preAuth, setPreAuth] = useState("");
  const [verified, setVerified] = useState<Channel[]>([]);
  const [form, setForm] = useState({ identifier:"",password:"",document:"",email:"",phoneE164:"",legalName:"",storeName:"",code:"" });

  useEffect(() => {
    document.title = mode === "register" ? "Criar loja | Peditto" : "Entrar na sua loja | Peditto";
    let robots = document.querySelector<HTMLMetaElement>('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement("meta");
      robots.name = "robots";
      document.head.append(robots);
    }
    robots.content = "noindex,nofollow";
  }, [mode]);

  function field(name: keyof typeof form, value: string) { setForm(old => ({ ...old, [name]: value })); }
  function switchMode(next: Mode) {
    setMode(next); setStep("start"); setError(""); setMessage(""); setVerified([]); setPreAuth("");
  }
  async function run(event: FormEvent, action: () => Promise<void>) {
    event.preventDefault(); setBusy(true); setError(""); setMessage("");
    try { await action(); } catch { setError("Não foi possível concluir agora. Confira os dados ou tente novamente."); }
    finally { setBusy(false); }
  }
  async function login(event: FormEvent) {
    await run(event, async () => {
      const result = await post<{preAuthenticationToken:string;availableChannels:Channel[]}>("/api/v1/auth/login",{identifier:form.identifier,password:form.password});
      setPreAuth(result.preAuthenticationToken); setChannel(result.availableChannels.includes("EMAIL") ? "EMAIL" : "SMS"); setStep("channel");
    });
  }
  async function sendCode(event: FormEvent) {
    await run(event, async () => {
      await post("/api/v1/auth/login/codes",{preAuthenticationToken:preAuth,channel});
      setStep("code"); setMessage("Se os dados estiverem corretos, o código chegará em alguns instantes.");
    });
  }
  async function confirmLogin(event: FormEvent) {
    await run(event, async () => {
      await post("/api/v1/auth/login/verify",{preAuthenticationToken:preAuth,channel,code:form.code});
      setStep("done"); setMessage("Sessão aberta com segurança.");
    });
  }
  async function register(event: FormEvent) {
    await run(event, async () => {
      await post("/api/v1/auth/registrations",{documentType:docType,document:form.document,email:form.email,phoneE164:form.phoneE164,password:form.password,legalName:form.legalName,storeName:form.storeName});
      setChannel("EMAIL"); setStep("verify"); setMessage("Cadastro iniciado. Confirme o e-mail e o celular.");
    });
  }
  async function verifyContact(event: FormEvent) {
    await run(event, async () => {
      await post("/api/v1/auth/registrations/verify",{email:form.email,channel,code:form.code});
      const next = [...verified,channel]; setVerified(next); field("code","");
      if (!next.includes("EMAIL")) { setChannel("EMAIL"); setMessage("Agora confirme o código enviado ao e-mail."); }
      else if (!next.includes("SMS")) { setChannel("SMS"); setMessage("E-mail confirmado. Agora confirme o código enviado ao celular."); }
      else { setStep("done"); setMessage("Cadastro confirmado. Sua loja está pronta para você começar."); }
    });
  }
  async function logout() {
    setBusy(true);
    setError("");
    try {
      const csrfResponse = await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
      if (!csrfResponse.ok) throw new Error();
      const csrf = (await csrfResponse.json()) as { headerName: string; token: string };
      const response = await fetch("/api/v1/auth/sessions/current", {
        method: "DELETE",
        credentials: "same-origin",
        headers: { [csrf.headerName]: csrf.token },
      });
      if (!response.ok) throw new Error();
      switchMode("login");
    } catch {
      setError("Não foi possível encerrar a sessão agora.");
    } finally {
      setBusy(false);
    }
  }

  return <main className="shell">
    <header className="nav">
      <a className="brand" href="/"><span className="brand-mark"><img src="/peditto-mascot-avatar.png" alt="" /></span>peditto<span className="dot">.</span></a>
      <span className="nav-note"><i/> SEU BALCÃO DIGITAL</span><a className="help" href="mailto:ajuda@peditto.com.br">Precisa de ajuda?</a>
    </header>
    <section className="hero">
      <div className="intro">
        <p className="eyebrow">UM LUGAR PRA SUA LOJA</p><h1>Seu negócio.<br/><em>Seu ritmo.</em></h1>
        <p className="lead">Cardápio, pedidos e entregas organizados. Você cuida do trabalho; a gente deixa o balcão em ordem.</p>
        <div className="trial"><span className="trial-icon">30</span><span><b>30 dias para testar</b><small>Comece com a loja vazia. O jeito de abastecer é seu.</small></span></div>
        <div className="retro-window" aria-hidden="true"><div className="win-title"><i/><i/><i/><span>balcao_da_noite.exe</span></div><div className="win-content"><div className="win-nav"><b/><b/><b/></div><div className="win-main"><small>PAINEL DA LOJA</small><strong>pedido novo</strong><span>2 itens · acabou de chegar</span><div className="win-bar"><i/></div></div></div><div className="win-status"><span>● sistema pronto</span><span>00:42</span></div></div>
        <p className="caption">01 — seu espaço, do seu jeito</p>
      </div>
      <div className="auth-wrap"><section className="auth-card">
        <div className="card-meta"><span>ÁREA DA LOJA</span><span>PTT—01</span></div>
        {step === "done" ? <div className="success"><span className="success-icon"><Check size={23}/></span><p className="eyebrow">{mode === "login" ? "ACESSO CONFIRMADO" : "CADASTRO CONFIRMADO"}</p><h2>{mode === "login" ? "Bem-vindo de volta." : "Sua loja está no ponto."}</h2><p>{mode === "login" ? "Sua sessão está ativa. O painel da loja será a próxima etapa da construção." : message}</p><button className="primary" disabled={busy} onClick={() => mode === "register" ? switchMode("login") : void logout()}>{mode === "register" ? "Ir para entrar" : busy ? "Encerrando..." : "Encerrar sessão"} <ArrowRight size={17}/></button><small>A sessão é protegida pelo servidor. Senhas e códigos não ficam salvos neste navegador.</small>{error && <p className="message error" role="alert">{error}</p>}</div> : <>
          <p className="eyebrow">{mode === "login" ? "QUE BOM TER VOCÊ DE VOLTA" : "ABRA SEU ESPAÇO"}</p>
          <h2>{mode === "login" ? "Entre na sua loja." : "Vamos começar?"}</h2>
          <p className="sub">{mode === "login" ? "Acesse o painel e continue de onde parou." : "Um cadastro. Uma loja. O resto você monta no seu tempo."}</p>
          {mode === "login" && step === "start" && <form onSubmit={e=>void login(e)}>
            <label>E-mail, CPF ou CNPJ<input autoComplete="username" value={form.identifier} onChange={e=>field("identifier",e.target.value)} placeholder="voce@loja.com.br ou documento" required maxLength={254}/></label>
            <label>Senha<input type="password" autoComplete="current-password" value={form.password} onChange={e=>field("password",e.target.value)} placeholder="Sua senha" required minLength={12} maxLength={128}/></label>
            <button className="primary" disabled={busy}>{busy ? "Conferindo..." : "Continuar"} <ArrowRight size={17}/></button>
          </form>}
          {mode === "login" && step === "channel" && <form onSubmit={e=>void sendCode(e)}>
            <p className="step-label">02 · CONFIRMAÇÃO EM DUAS ETAPAS</p><label>Onde quer receber o código?<select value={channel} onChange={e=>setChannel(e.target.value as Channel)}><option value="EMAIL">E-mail verificado</option><option value="SMS">Celular verificado</option></select></label>
            <p className="hint"><LockKeyhole size={15}/> Código único, com validade curta. Não compartilhe.</p><button className="primary" disabled={busy}>{busy ? "Enviando..." : "Enviar código"} <ArrowRight size={17}/></button><button className="back" type="button" onClick={()=>setStep("start")}>Voltar</button>
          </form>}
          {mode === "login" && step === "code" && <form onSubmit={e=>void confirmLogin(e)}>
            <p className="step-label">03 · CÓDIGO ENVIADO POR {channel === "EMAIL" ? "E-MAIL" : "SMS"}</p><label>Código de 6 dígitos<input className="code" inputMode="numeric" autoComplete="one-time-code" value={form.code} onChange={e=>field("code",e.target.value.replace(/\D/g,"").slice(0,6))} placeholder="000000" pattern="[0-9]{6}" required/></label>
            <p className="hint"><LockKeyhole size={15}/> A equipe Peditto nunca pedirá este código por telefone.</p><button className="primary" disabled={busy}>{busy ? "Validando..." : "Confirmar e entrar"} <ArrowRight size={17}/></button><button className="back" type="button" onClick={()=>setStep("channel")}>Escolher outro canal</button>
          </form>}
          {mode === "register" && step === "start" && <form onSubmit={e=>void register(e)}>
            <div className="pair"><label>Cadastro<select value={docType} onChange={e=>setDocType(e.target.value as "CPF"|"CNPJ")}><option>CNPJ</option><option>CPF</option></select></label><label>{docType}<input value={form.document} onChange={e=>field("document",e.target.value)} placeholder={docType === "CPF" ? "000.000.000-00" : "00.000.000/0000-00"} maxLength={18} required/></label></div>
            <label>Nome de quem responde<input autoComplete="name" value={form.legalName} onChange={e=>field("legalName",e.target.value)} placeholder="Seu nome completo" minLength={2} maxLength={160} required/></label>
            <label>Nome da loja<input autoComplete="organization" value={form.storeName} onChange={e=>field("storeName",e.target.value)} placeholder="Como seus clientes conhecem a loja?" minLength={2} maxLength={120} required/></label>
            <div className="pair"><label>E-mail<input type="email" autoComplete="email" value={form.email} onChange={e=>field("email",e.target.value)} placeholder="voce@exemplo.com" maxLength={254} required/></label><label>Celular<input type="tel" autoComplete="tel" value={form.phoneE164} onChange={e=>field("phoneE164",e.target.value)} placeholder="+5511999999999" pattern="\+[1-9][0-9]{7,14}" required/></label></div>
            <label>Crie uma senha<input type="password" autoComplete="new-password" value={form.password} onChange={e=>field("password",e.target.value)} placeholder="Pelo menos 12 caracteres" minLength={12} maxLength={128} required/></label>
            <p className="fine">Um CPF ou CNPJ pode ter uma única loja e um único período de teste.</p><button className="primary" disabled={busy}>{busy ? "Preparando..." : "Criar minha loja"} <ArrowRight size={17}/></button>
          </form>}
          {mode === "register" && step === "verify" && <form onSubmit={e=>void verifyContact(e)}>
            <p className="step-label">CONFIRME SEU {channel === "EMAIL" ? "E-MAIL" : "CELULAR"}</p><p className="sub">O código foi enviado para {channel === "EMAIL" ? form.email : "o celular cadastrado"}.</p>
            <label>Código de 6 dígitos<input className="code" inputMode="numeric" autoComplete="one-time-code" value={form.code} onChange={e=>field("code",e.target.value.replace(/\D/g,"").slice(0,6))} placeholder="000000" pattern="[0-9]{6}" required/></label><button className="primary" disabled={busy}>{busy ? "Confirmando..." : "Confirmar contato"} <ArrowRight size={17}/></button>
          </form>}
          {error && <p className="message error" role="alert">{error}</p>}{message && <p className="message info" role="status">{message}</p>}
          <div className="switch"><span>{mode === "login" ? "Ainda não tem uma loja?" : "Já tem uma conta?"}</span><button onClick={()=>switchMode(mode === "login" ? "register" : "login")}>{mode === "login" ? "Criar cadastro" : "Entrar"}</button></div>
        </>}
      </section><p className="secure"><LockKeyhole size={13}/> SESSÃO E PERMISSÕES VALIDADAS NO SERVIDOR</p></div>
    </section>
    <section className="features"><div><b>Uma loja só sua</b><span>Seu espaço, seus produtos.</span></div><div><b>Pedidos organizados</b><span>Do balcão até a entrega.</span></div><div><b>Seu raio de entrega</b><span>Você define até onde vai.</span></div></section>
    <footer><span>© 2026 Peditto</span><span>Feito para quem cuida da própria loja.</span><a href="mailto:ajuda@peditto.com.br">Falar com a gente</a></footer>
  </main>;
}
export default App;
