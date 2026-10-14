import { useEffect, useState, type FormEvent } from "react";
import { ArrowRight, Check, LockKeyhole } from "lucide-react";
import RegistrationFlow from "../components/RegistrationFlow";
import { deleteCurrentSession, postAuth } from "../services/authApi";

type Channel = "EMAIL" | "SMS";
type LoginStep = "credentials" | "channel" | "code" | "done";

function LoginFlow() {
  const [step, setStep] = useState<LoginStep>("credentials");
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [channel, setChannel] = useState<Channel>("EMAIL");
  const [preAuth, setPreAuth] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function submit(event: FormEvent, action: () => Promise<void>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await action();
    } catch {
      setError("Não foi possível concluir agora. Confira os dados ou tente novamente.");
    } finally {
      setBusy(false);
    }
  }

  async function login(event: FormEvent) {
    await submit(event, async () => {
      const result = await postAuth<{ preAuthenticationToken: string; availableChannels: Channel[] }>(
        "/api/v1/auth/login",
        { identifier, password },
      );
      setPreAuth(result.preAuthenticationToken);
      setChannel(result.availableChannels.includes("EMAIL") ? "EMAIL" : "SMS");
      setStep("channel");
    });
  }

  async function sendCode(event: FormEvent) {
    await submit(event, async () => {
      await postAuth("/api/v1/auth/login/codes", { preAuthenticationToken: preAuth, channel });
      setStep("code");
      setMessage("Se os dados estiverem corretos, o código chegará em alguns instantes.");
    });
  }

  async function confirmLogin(event: FormEvent) {
    await submit(event, async () => {
      await postAuth("/api/v1/auth/login/verify", { preAuthenticationToken: preAuth, channel, code });
      setStep("done");
      setMessage("Sessão aberta com segurança.");
    });
  }

  async function logout() {
    setBusy(true);
    setError("");
    try {
      await deleteCurrentSession();
      setStep("credentials");
    } catch {
      setError("Não foi possível encerrar a sessão agora.");
    } finally {
      setBusy(false);
    }
  }

  if (step === "done") {
    return (
      <div className="success">
        <span className="success-icon"><Check size={23} aria-hidden="true" /></span>
        <p className="eyebrow">ACESSO CONFIRMADO</p>
        <h1 id="auth-title">Bem-vindo de volta.</h1>
        <p>{message}</p>
        <button className="primary" disabled={busy} onClick={() => void logout()}>
          {busy ? "Encerrando..." : "Encerrar sessão"}<ArrowRight size={17} aria-hidden="true" />
        </button>
        <small>A sessão é protegida pelo servidor. Senhas e códigos não ficam salvos neste navegador.</small>
        {error && <p className="message error" role="alert">{error}</p>}
      </div>
    );
  }

  return (
    <>
      <p className="eyebrow">QUE BOM TER VOCÊ DE VOLTA</p>
      <h1 id="auth-title">Entre na sua loja.</h1>
      <p className="sub">Acesse o painel e continue de onde parou.</p>

      {step === "credentials" && (
        <form onSubmit={(event) => void login(event)}>
          <label>E-mail, CPF ou CNPJ
            <input autoComplete="username" value={identifier} onChange={(event) => setIdentifier(event.target.value)} placeholder="voce@loja.com.br ou documento" required maxLength={254} />
          </label>
          <label>Senha
            <input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Sua senha" required minLength={12} maxLength={128} />
          </label>
          <button className="primary" disabled={busy}>{busy ? "Conferindo..." : "Continuar"}<ArrowRight size={17} aria-hidden="true" /></button>
        </form>
      )}

      {step === "channel" && (
        <form onSubmit={(event) => void sendCode(event)}>
          <p className="step-label">02 · CONFIRMAÇÃO EM DUAS ETAPAS</p>
          <label>Onde quer receber o código?
            <select value={channel} onChange={(event) => setChannel(event.target.value as Channel)}>
              <option value="EMAIL">E-mail verificado</option>
              <option value="SMS">Celular verificado</option>
            </select>
          </label>
          <p className="hint"><LockKeyhole size={15} aria-hidden="true" />Código único, com validade curta. Não compartilhe.</p>
          <button className="primary" disabled={busy}>{busy ? "Enviando..." : "Enviar código"}<ArrowRight size={17} aria-hidden="true" /></button>
          <button className="back" type="button" onClick={() => setStep("credentials")}>Voltar</button>
        </form>
      )}

      {step === "code" && (
        <form onSubmit={(event) => void confirmLogin(event)}>
          <p className="step-label">03 · CÓDIGO ENVIADO POR {channel === "EMAIL" ? "E-MAIL" : "SMS"}</p>
          <label>Código de 6 dígitos
            <input className="code" inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="000000" pattern="[0-9]{6}" required />
          </label>
          <p className="hint"><LockKeyhole size={15} aria-hidden="true" />A equipe Peditto nunca pedirá este código por telefone.</p>
          <button className="primary" disabled={busy}>{busy ? "Validando..." : "Confirmar e entrar"}<ArrowRight size={17} aria-hidden="true" /></button>
          <button className="back" type="button" onClick={() => setStep("channel")}>Escolher outro canal</button>
        </form>
      )}

      {error && <p className="message error" role="alert">{error}</p>}
      {message && <p className="message info" role="status">{message}</p>}
      <p className="registration-switch">Ainda não tem uma loja? <a href="/criar-conta">Criar cadastro</a></p>
    </>
  );
}

function AuthPage() {
  const registering = window.location.pathname === "/criar-conta";

  useEffect(() => {
    document.title = registering ? "Criar loja | Peditto" : "Entrar na sua loja | Peditto";
    let robots = document.querySelector<HTMLMetaElement>('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement("meta");
      robots.name = "robots";
      document.head.append(robots);
    }
    robots.content = "noindex,nofollow";
  }, [registering]);

  return (
    <main className="auth-page">
      <header className="auth-header">
        <a className="auth-brand" href="/" aria-label="Peditto, página inicial">
          <img src="/peditto-mascot-avatar.png" alt="" />
          <span>peditto<span>.</span></span>
        </a>
        <a className="auth-help" href="mailto:ajuda@peditto.com.br">Precisa de ajuda?</a>
      </header>

      <div className="auth-main">
        <section className="auth-wrap" aria-labelledby="auth-title">
          <div className="auth-card">
            <div className="card-meta"><span>ÁREA DA LOJA</span><span>PTT—01</span></div>
            {registering ? <RegistrationFlow appearance="page" /> : <LoginFlow />}
          </div>
          <p className="secure"><LockKeyhole size={13} aria-hidden="true" />SESSÃO E PERMISSÕES VALIDADAS NO SERVIDOR</p>
        </section>
      </div>

      <footer className="auth-footer">
        <span>© 2026 Peditto</span>
        <a href="mailto:ajuda@peditto.com.br">Falar com a gente</a>
      </footer>
    </main>
  );
}

export default AuthPage;
