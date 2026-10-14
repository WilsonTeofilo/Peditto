# Threat model — MVP Peditto

- **Estado:** threat model inicial da Fase 0; rever a cada módulo/provider e antes de piloto.
- **Data:** 14/10/2026.
- **Escopo:** cadastro/login, tenant/store, vitrine/pedido, trial/cobrança, mapas, campanhas e console da plataforma.
- **Fora do escopo neste momento:** desenho de rede cloud/provedor final, app nativo e pentest. Nenhum controle listado abaixo está pronto só por constar aqui.

## Ativos que exigem proteção

| Ativo | Sensibilidade | Impacto de comprometimento |
|---|---|---|
| Senha, sessão, OTP, chaves de assinatura/pepper | Crítica | takeover, fraude, acesso a vários tenants |
| CPF/CNPJ, e-mail, celular e endereço | Alta | fraude, enumeração, abuso e exposição de PII |
| Tenant, papéis, catálogo privado, equipe | Alta | mistura de lojas, escalação de privilégio |
| Pedido, histórico, localização/entrega do comprador | Alta | exposição de hábitos/endereço e fraude operacional |
| Faturas, pagamentos e provider reference | Alta | fraude financeira, cobrança errada, exposição comercial |
| Consentimento e lista de campanha/suppression | Alta | spam, reclamação, dano reputacional e violação de preferência |
| Audit log e eventos financeiros | Alta | impedir investigação, repudiar ação ou adulterar indicadores |
| Dados agregados/análises e estado SaaS | Média/Alta | decisão operacional errada, abuso de plataforma |

## Atores e limites de confiança

- Comprador anônimo: vê apenas loja publicada, dados públicos e cálculo de disponibilidade. Seu navegador/Host/payload são não confiáveis.
- Comprador autenticado: vê o próprio perfil, carrinho e pedidos daquela loja, não pede permissão administrativa.
- Dono/funcionário: pertence a organização/loja; papel é definido no servidor e revogável.
- Operador/CEO Peditto: conta separada; não é automaticamente membro ou owner das lojas. Acesso excepcional a tenant exige capacidade, justificativa e audit.
- Provedores de SMS, e-mail, mapa, storage e pagamento: terceiros; só recebem dados mínimos e callbacks são input não confiável até validar assinatura/estado.
- Infra/DB/CI: limites administrativos com credenciais distintas. Código de desenvolvimento não recebe dado real.

Fluxo principal: navegador → proxy confiável/host allow-list → API Spring → resolução de domínio/tenant → authorization por membership/ownership → service transacional → repository/DB. Worker/outbox usa identidade própria e payload limitado. Integrações externas via adapters com timeout, allow-list e idempotência. Plataforma CEO separada por role e superfície.

## Cenários prioritários e defesa necessária

| Prioridade / ameaça | Cenário | Defesa obrigatória antes do MVP/piloto | Sinal de detecção |
|---|---|---|---|
| **P0 — IDOR/cross-tenant** | Usuário troca UUID, slug, item aninhado, query ou Host e lê/altera pedido/catalogo de outra loja. | Resolver host em allow-list; host só escolhe contexto público; membership/capability/ownership em cada serviço; repository tenant-scoped; FKs tenant compostas; RLS extra após validar pool; negação por padrão. | 404/deny cross-tenant, alertas de tentativa repetida, testes A→B em todo endpoint/export/asset. |
| **P0 — account takeover** | Credential stuffing, senha vazada, interceptação de OTP, brute force de código ou recovery abuse. | Argon2id/BCrypt parametrizado; rate limit distribuído; OTP via canal verificado, expiry, hash/HMAC, uso único/limite, challenge vinculado; step-up, sessão revogável, recuperação forte; respostas anti-enumeração. | Picos de falhas, OTP resend/attempt, IP/ASN/dispositivo em risco com retenção limitada; alerta e hold. |
| **P0 — privilege/mass assignment** | Payload adiciona `role=OWNER`, `tenantId`, `enabled`, MFA bypass, preço/total/trial/owner. | Request DTO allow-list, rejeitar unknown, mapper explícito; entity nunca HTTP; server authoritative status/role/tenant/totals; tests payload poisoning. | Log de propriedade proibida sem gravar valores secretos, alertas em endpoints críticos. |
| **P0 — duplicação trial / race** | Duas requests concorrentes ou novo email/slug tenta gerar trial de novo; atacante usa documento falso. | Unique constraints/transaction de onboarding; trial único por organization/documento; idempotency; verificação e revisão exceções; abuse signals sem fingerprint único como prova. | conflitos de documento, trial exception audit, signups repetitivos e fraud review. |
| **P0 — fraude billing/webhook** | Request forja pagamento, webhook replay ou amount/org errado; lógica de acesso suspende antes/depois do prazo incorreto. | Hosted checkout; assinatura/timestamp/replay; amount/reference reconciliados server-side; idempotência; estado de acesso derivado da fatura e do calendário de tolerância; audit append-only; sem encargo por atraso ou edição de fatura paga. | reconciliação divergente, webhook duplicado, divergência de estado da assinatura e alertas de atraso. |
| **P0 — vazamento de cartão** | PAN/CVV capturado em formulário, log, analytics, banco ou acesso interno. | UI/provider hosted checkout e token de uso imediato; bloquear campos PAN/CVV no API; redaction/WAF/logging; proibir cartão salvo/vault recorrente no SaaS e loja. | DLP/secret scan e alertas de payload/log sensível; revisão de contratos provider. |
| **P1 — abuso de CEO/support** | Operador muda preço, dispara email total, exporta PII ou impersona sem motivo. | Operator identity separada, MFA e step-up, RBAC por ação, aggregate-by-default, motivo/acesso com prazo, audit imutável, preview/confirm/cancel window campanhas, restrição de rede extra. | Ações privilegiadas e exportações geram alerta; revisão periódica de grants. |
| **P1 — spam/consent abuse** | Campanha enviada para todo mundo sem opt-in ou após opt-out; e-mail transacional misturado. | Marketing opt-in separado/desmarcado, texto e versão, unsubscribe/suppression, provider com complaint/bounce, transactional separado, campaign throttling e pause. | complaint, bounce, unsubscribe, sudden volume e provider reputation. |
| **P1 — fraude/erro de localização** | GPS falso/impreciso, SSRF por URL geocoder, tracking excessivo, rua fora da área aceita indevidamente. | Browser location só por ação; endereço manual; geocode provider allow-list; cobertura revalidada no checkout server-side; círculo rotulado como distância, não rota; minimizar/expirar coordenadas. | discrepância address/coordinates, provider error/cost, geolocation retention audit. |
| **P1 — upload/SSRF/XSS** | Arquivo executável/polyglot, SVG ativo, URL para metadata/private IP, XSS via descrição. | MIME sniff + allow-list, reencode image, limites, storage privado, URLs assinadas curtas, SSRF egress/DNS/redirect blocking, output encoding/CSP. | rejeições, scans malware, outbound egress logs sem query secrets. |
| **P1 — domínio takeover** | Host malformado, domínio antigo liberado, slug reservado, proxy header spoof. | Trusted proxy config; canonical host/IDN normalization; verified DNS/TLS; active-domain constraints; remove/release cooldown; no fallback tenant. | hosts desconhecidos, domínio ativo sem verify, mudança auditada. |
| **P1 — privacidade/insider** | Staff/CEO acessa endereço, CPF ou pedidos fora da função; logs retêm PII. | Minimize fields, column encryption where needed, purpose-limited access, tenant scope, retention/export/delete, audit, role separation, no PII in telemetry. | anomalous reads/exports, role review, DLP logs. |
| **P2 — indisponibilidade/data loss** | Ataque volumétrico, migration ruim, provider fora, perda de DB/queue. | Rate limit/WAF, quotas, timeout/circuit breaker, backup/PITR/restore drill, migration expand/contract, incident/runbooks. | SLO, lag, error budget, restore result and provider health. |

## Invariantes de segurança

**Regra de autoridade:** o backend é a única fronteira de segurança e a fonte da verdade. O frontend pode reforçar a experiência, mas nenhum controle, claim, validação ou estado mantido no cliente substitui autorização, validação de propriedade/tenant, MFA, rate limit ou regra de negócio aplicada pelo servidor.

1. O browser não escolhe tenant, role, estado de pagamento, trial, MFA, preço final ou custo.
2. Cada query/mutação privada confirma usuário, membership ativo, capability e ownership. IDs UUID reduzem enumeração, mas não são autorização.
3. Recursos públicos da vitrine não retornam custo, documento de dono, contato privado, estoque interno, dados de outro cliente ou metadado de segurança.
4. Sem senha/OTP/cartão em logs, seed, analytics, exceção ou audit metadata. OTP/outbox é protegido e apagado após entrega/expiry.
5. Transição monetária/status e alteração de acesso são transacionais, idempotentes e auditáveis.
6. Campanha promocional não usa destinatário sem consentimento ativo; transaction messages continuam permitidas por finalidade separada e regras aplicáveis.
7. Console plataforma não herda papel de loja e não tem bypass tenant implícito.

## Perguntas/risco residual para resolver antes de piloto

- Escolha/contrato e data location dos providers de SMS, e-mail, pagamento, mapas, armazenamento e logs.
- Revisão dos termos sem encargos, notificações, suspensão e cancelamento com jurídico/contabilidade.
- Política de retenção/exportação por 90 dias após suspensão/cancelamento, eliminação/anonimização e tratamento de localização/documento.
- Política de reautenticação e recovery quando o usuário perde ambos canais.
- Testes independentes e pentest; este documento não é certificação nem prova de conformidade.

## Gatilhos de revisão do threat model

Rever ao adicionar: novo provider/webhook, domínio customizado, plano multi-loja, mudança de identidade/JWT, exportação, impersonation, pagamento online, PostGIS/roteamento, app mobile, API pública ou novos papéis.

