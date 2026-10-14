import { useState, type FormEvent } from "react";
import { ArrowRight, Check, LockKeyhole } from "lucide-react";
import { postAuth } from "../services/authApi";

type Channel = "EMAIL" | "SMS";
type DocumentType = "CPF" | "CNPJ";
type Stage = "start" | "verify" | "done";
type Registration = {
  document: string;
  email: string;
  phoneE164: string;
  password: string;
  legalName: string;
  storeName: string;
  code: string;
};

const emptyRegistration: Registration = {
  document: "",
  email: "",
  phoneE164: "",
  password: "",
  legalName: "",
  storeName: "",
  code: "",
};

type Props = { appearance: "page" | "hero" };

function RegistrationFlow({ appearance }: Props) {
  const [stage, setStage] = useState<Stage>("start");
  const [documentType, setDocumentType] = useState<DocumentType>("CNPJ");
  const [channel, setChannel] = useState<Channel>("EMAIL");
  const [verified, setVerified] = useState<Channel[]>([]);
  const [form, setForm] = useState<Registration>(emptyRegistration);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  function update(name: keyof Registration, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }

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

  async function register(event: FormEvent) {
    await submit(event, async () => {
      await postAuth("/api/v1/auth/registrations", {
        documentType,
        document: form.document,
        email: form.email,
        phoneE164: form.phoneE164,
        password: form.password,
        legalName: form.legalName,
        storeName: form.storeName,
      });
      setStage("verify");
      setMessage("Cadastro iniciado. Confirme seu e-mail e celular para proteger a conta.");
    });
  }

  async function verify(event: FormEvent) {
    await submit(event, async () => {
      await postAuth("/api/v1/auth/registrations/verify", {
        email: form.email,
        channel,
        code: form.code,
      });
      const next = [...verified, channel];
      setVerified(next);
      update("code", "");

      if (!next.includes("EMAIL")) {
        setChannel("EMAIL");
        setMessage("Celular confirmado. Agora confirme o código enviado ao e-mail.");
      } else if (!next.includes("SMS")) {
        setChannel("SMS");
        setMessage("E-mail confirmado. Agora confirme o código enviado ao celular.");
      } else {
        setStage("done");
        setMessage("Cadastro confirmado. Sua loja está pronta para você começar.");
      }
    });
  }

  return (
    <div className={`registration-flow registration-flow--${appearance}`}>
      {stage === "done" ? (
        <div className="registration-success">
          <span className="registration-success__icon"><Check size={22} aria-hidden="true" /></span>
          <p className="eyebrow">CADASTRO CONFIRMADO</p>
          <h2>Sua loja está no ponto.</h2>
          <p>{message}</p>
          <a className={appearance === "hero" ? "landing-button landing-button--gold" : "primary"} href="/entrar">
            Entrar na minha loja <ArrowRight size={17} aria-hidden="true" />
          </a>
        </div>
      ) : (
        <>
          {appearance === "hero" && (
            <header className="hero-signup-card__header">
              <p className="micro-label">30 DIAS PARA TESTAR</p>
              <h2>Seu delivery, do seu jeito.</h2>
              <p>Comece sem cadastrar cartão. Você organiza a loja no seu tempo.</p>
            </header>
          )}

          {stage === "start" ? (
            <form className="registration-form" onSubmit={(event) => void register(event)}>
              <div className="registration-pair">
                <label>Documento
                  <select value={documentType} onChange={(event) => setDocumentType(event.target.value as DocumentType)}>
                    <option value="CNPJ">CNPJ</option>
                    <option value="CPF">CPF</option>
                  </select>
                </label>
                <label>{documentType}
                  <input value={form.document} onChange={(event) => update("document", event.target.value)} placeholder={documentType === "CPF" ? "000.000.000-00" : "00.000.000/0000-00"} maxLength={18} autoComplete="off" required />
                </label>
              </div>
              <label>Seu nome
                <input autoComplete="name" value={form.legalName} onChange={(event) => update("legalName", event.target.value)} placeholder="Nome de quem responde pela loja" minLength={2} maxLength={160} required />
              </label>
              <label>Nome da loja
                <input autoComplete="organization" value={form.storeName} onChange={(event) => update("storeName", event.target.value)} placeholder="Como seus clientes conhecem a loja?" minLength={2} maxLength={120} required />
              </label>
              <div className="registration-pair">
                <label>E-mail
                  <input type="email" autoComplete="email" value={form.email} onChange={(event) => update("email", event.target.value)} placeholder="voce@exemplo.com" maxLength={254} required />
                </label>
                <label>Celular
                  <input type="tel" autoComplete="tel" value={form.phoneE164} onChange={(event) => update("phoneE164", event.target.value)} placeholder="+5511999999999" pattern="\+[1-9][0-9]{7,14}" required />
                </label>
              </div>
              <label>Crie uma senha
                <input type="password" autoComplete="new-password" value={form.password} onChange={(event) => update("password", event.target.value)} placeholder="Pelo menos 12 caracteres" minLength={12} maxLength={128} required />
              </label>
              <p className="registration-note">Uma loja por CPF ou CNPJ. Sem cartão no cadastro.</p>
              <button className={appearance === "hero" ? "landing-button landing-button--gold hero-submit" : "primary"} disabled={busy}>
                {busy ? "Preparando sua loja..." : "Começar meu teste grátis"}<ArrowRight size={17} aria-hidden="true" />
              </button>
            </form>
          ) : (
            <form className="registration-form" onSubmit={(event) => void verify(event)}>
              <p className="step-label">CONFIRME SEU {channel === "EMAIL" ? "E-MAIL" : "CELULAR"}</p>
              <p className="registration-message">{message || `Enviamos um código para ${channel === "EMAIL" ? form.email : "seu celular"}.`}</p>
              <label>Código de 6 dígitos
                <input className="code" inputMode="numeric" autoComplete="one-time-code" value={form.code} onChange={(event) => update("code", event.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="000000" pattern="[0-9]{6}" required />
              </label>
              <p className="registration-note"><LockKeyhole size={14} aria-hidden="true" /> O código é temporário e só deve ser digitado aqui.</p>
              <button className={appearance === "hero" ? "landing-button landing-button--gold hero-submit" : "primary"} disabled={busy}>
                {busy ? "Confirmando..." : "Confirmar código"}<ArrowRight size={17} aria-hidden="true" />
              </button>
              {message && <p className="message info" role="status">{message}</p>}
            </form>
          )}
          {error && <p className="message error" role="alert">{error}</p>}
          {appearance === "page" && stage === "start" && (
            <p className="registration-switch">Já tem uma conta? <a href="/entrar">Entrar</a></p>
          )}
        </>
      )}
    </div>
  );
}

export default RegistrationFlow;
