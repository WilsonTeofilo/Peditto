# README — roteiro de construção MVC do Peditto

**Fonte de contexto para continuidade:** este roteiro define fases e critérios de aceite. Para retomar em um chat novo, leia primeiro [`CONTEXTO_PARA_NOVO_CHAT.md`](CONTEXTO_PARA_NOVO_CHAT.md), depois o relatório técnico e as decisões referenciadas. Estado revisado em 15/10/2026: V1–V16 aplicadas no PostgreSQL local 17.11; Flyway validou as 16 migrations, Hibernate `validate` passou e a aplicação iniciou em porta aleatória. RLS está forçada nas tabelas tenant-owned existentes, `peditto_app` segue sem `SUPERUSER`/`BYPASSRLS`, e testes integrados anteriores cobrem contexto transacional, isolamento A↔B, rollback e lookup público. Fases 4, 5 e 6 estão concluídas no escopo backend atual. Rotas de negócio permanecem fechadas por decisão explícita e uploads permanecem desabilitados. A Fase 3 segue aberta para validação integrada OTP/CSRF, step-up e concorrência.

**Atualização (15/10/2026):** além da baseline de segurança básica, smoke checks locais corrigiram a disponibilidade CSRF, a dependência indevida do health check no SMTP e a criação de `JSESSIONID`. Consulte também o relatório técnico para evidências e limitações.

## Ordem replanejada para a retomada do backend

Esta seção define a ordem do trabalho de backend e banco; ela não impede trabalho visual no frontend quando o usuário o priorizar. As fases abaixo não estão concluídas só porque já existem classes, entidades ou migrations: cada gate precisa ser comprovado no ambiente correspondente. A próxima integração entre vitrine e dados reais depende dos gates de identidade/segurança da Fase 3 e das autorizações da Fase 4. A demonstração visual da Fase 7 existe, mas não habilita rotas nem substitui esses gates.

Antes de endpoints privados ou dados reais, seguir a [linha de base de segurança](security/BASELINE_SEGURANCA.md). Ela fecha itens básicos que atravessam as fases, incluindo hash e política de senha, segredos, CPF/CNPJ, permissões do PostgreSQL, sessão/CSRF, logs, erros, dependências e backups.

### Gate 0 — segurança básica antes de abrir superfícies

1. Conferir algoritmo e parâmetros efetivos de senha; escolher e documentar uma política única para o `PasswordEncoder` existente.
2. Definir proteção e retenção de CPF/CNPJ; o schema atual guarda o valor normalizado e legível, então nenhum documento real deve ser inserido até a decisão de proteção de campo/consulta estar tomada.
3. Verificar `.gitignore`, ACL do `.env`, placeholders, saída de scripts/logs e caminho de rotação/revogação das chaves; não copiar segredo para diagnóstico.
4. Confirmar roles e rede do PostgreSQL: runtime mínimo, migrator separado, sem privilégio elevado, serviço local limitado a loopback e TLS com certificado validado fora de dev.
5. Confirmar cookies/CSRF, default-deny, rejeição de payload desconhecido, limites de requisição e ausência de PII/segredos em erro/log.

**Gate:** política de senha documentada e compatível com o encoder real; nenhum segredo ou documento real exposto; roles e superfície HTTP verificadas antes de ativar autenticação fora do ambiente local.

### 1. Fechar persistência e isolamento (Fases 1 e 4 — bloco A)

1. Confirmar o estado do PostgreSQL, roles e histórico do Flyway sem exibir ou copiar segredos. Fazer backup antes de qualquer mudança de schema.
2. [x] V9–V13 aplicadas no PostgreSQL local 17.11; histórico Flyway confirma as migrations e Hibernate `ddl-auto=validate` passa. Corrigido o bug de variável ambígua da V10 antes de aplicá-la.
3. [x] V11 cobre `stores`, `store_settings`, `delivery_zones`, `store_opening_hours`, `store_domains`, `trials`, `billing_invoices`, `organization_subscriptions`, `invoice_payments`, `financial_ledger_entries` e `idempotency_records`; domínio recebeu `organization_id` e FK tenant-aware.
4. [x] `peditto_app` não é superuser nem tem `BYPASSRLS`; RLS/`FORCE ROW LEVEL SECURITY` cobre 25 tabelas. Lookup de domínio público usa função `SECURITY DEFINER` com owner `peditto_migrator`, `search_path` fixo, retorno mínimo e execução concedida apenas ao runtime.
5. [x] Teste integrado com fixtures descartáveis comprovou A↔B nas tabelas operacionais novas, lookup do domínio verificado, contexto GUC no transaction manager, reuso da mesma conexão Hikari e rollback. RLS não substitui autorização por objeto e não protege contra SQL injection executado como `peditto_app`.

**Gate:** concluído para iniciar catálogo/setup; nenhuma rota de negócio foi liberada. RLS continua sendo defesa adicional às consultas tenant-scoped, FKs compostas e autorização por objeto.

### 2. Fechar identidade, MFA e sessão (Fase 3 — bloco B)

1. Validar o cadastro transacional e a unicidade concorrente de CPF/CNPJ; a loja começa vazia e o trial só começa após as verificações exigidas.
2. Integrar OTP/outbox com os canais autorizados, configuração de segredos e comportamento de falha; verificar expiração, uso único, tentativas, reenvio e respostas anti-enumeração.
3. Fechar cookie, CSRF e validação de origem no fluxo real do browser; CORS continua fechado enquanto não houver necessidade e allow-list de origem definida.
4. Completar step-up/reautenticação para mudanças de senha, contatos, MFA e sessões. Não permitir troca de fator baseada em estado enviado pelo cliente.
5. Confirmar algoritmo e parâmetros de senha: o código usa PBKDF2 versionado, enquanto parte da documentação cita Argon2id/BCrypt. Registrar uma política única antes de habilitar o fluxo em ambiente compartilhado.

**Gate:** senha sem OTP não cria sessão; códigos expirados/reutilizados falham; reset e revogação encerram as sessões adequadas; nenhum segredo aparece em resposta ou log.

### 3. Ligar autorização por recurso e tenant (Fase 4 — bloco C)

1. Fechar a matriz endpoint × papel × capability × recurso, incluindo leitura, escrita, transições, exportação e mídia.
2. Derivar usuário e membership da sessão vigente; o header seletor nunca concede acesso. Aplicar autorização de objeto e consulta scoped em cada caso de uso/repository.
3. Separar resolução de host público da autorização privada. Não confiar em `Host`/headers encaminhados fora da configuração de proxy confiável; não criar papel de CEO como bypass de tenant.
4. Só liberar endpoints individualmente depois de contrato DTO allow-list, CSRF quando aplicável, autorização server-side, resposta mínima e verificação negativa de IDOR.

**Gate:** trocar UUID, organização, slug, host ou ID aninhado não permite ler ou alterar dados de outra loja; tentativa não revela a existência do recurso.

### 4. Construir o primeiro fluxo operacional (Fases 5 a 8)

1. **Fase 5 — configuração da loja:** perfil, timezone, horários, métodos de recebimento, zona circular e publicação explícita; cobertura recalculada no servidor.
2. **Fase 6 — catálogo e mídia:** categoria, produto e adicional com CRUD scoped; upload seguro somente quando storage escolhido e validado.
3. **Fase 7 — API da vitrine, cliente e carrinho:** política de identidade do comprador por loja, carrinho anônimo protegido e preços sempre relidos no servidor; a implementação visual React fica fora desta retomada.
4. **Fase 8 — API e domínio de pedidos:** checkout transacional/idempotente, snapshots, transições válidas e histórico imutável. Interfaces de cliente/loja ficam fora desta retomada. Adapters de pagamento só entram após escolha e validação do provider; não bloquear o modelo de pedidos aguardando integração especulativa.

**Gate:** APIs e casos de uso operam em tenant de teste e falham nos cenários de acesso cruzado, concorrência, replay e adulteração de preço.

### 5. Implementar ciclo SaaS e governança da plataforma (Fases 9 e 10)

1. **Fase 9 — billing:** estados de trial/assinatura/fatura, pagamento manual hospedado, webhook autenticado e idempotente, tolerância de 15 dias sem encargos, bloqueio e retenção/exportação conforme ADR 0003.
2. **Fase 10 — CEO:** somente depois de identidade de operador separada, MFA/step-up, grants mínimos, audit append-only e métricas agregadas. Sem acesso irrestrito a lojas.

**Gate:** pagamento só altera acesso após reconciliação server-side; faturas confirmadas não são reescritas; suspensão e reativação são determinísticas e auditáveis.

### 6. Operação e segurança são transversais; lançamento é o gate final

- Em cada bloco: logs sem PII/segredos, limites, timeouts, tratamento de falhas, migrations compatíveis, testes da regra e revisão de autorização.
- Antes de processar dinheiro ou dados reais: backup/restore, retenção e exclusão, monitoramento, gestão/rotação de segredos, TLS/proxy confiável, revisão jurídica aplicável e revisão independente das rotas críticas.
- A antiga Fase 12 continua sendo o gate integrado de qualidade e lançamento, não o primeiro momento em que se escrevem testes. A Fase 11 de operação começa no primeiro fluxo que precisa ser operado e se completa antes do piloto.
- SEO avançado e aquisição orgânica ficam para a Fase 13; a vitrine fictícia `/loja/demo` não é indexável nem deve ser anunciada como loja real.

**Próximo trabalho técnico:** conectar a vitrine visual da Fase 7 à projeção pública tenant-scoped, definir a identidade/conta do comprador e fechar autorização e validação do carrinho no servidor. As rotas seguem em `denyAll` até existir matriz de capability e revisão de acesso. A Fase 8 fica responsável por revalidar a área no checkout e gravar snapshot mínimo. Antes de considerar mídia pronta para produção multi-instância, trocar/adicionar adapter de object storage privado; o adapter atual é filesystem privado, sem servir arquivos estaticamente, e upload está desligado.

## Como interpretar MVC neste projeto

O backend será uma API REST com Spring MVC, organizada em camadas: **Controller → Application Service → Domain/Repository (JPA) → PostgreSQL**. React/TypeScript é a camada visual. Não vamos misturar JSP/Thymeleaf nem colocar regra em controller/entidade. Entidades JPA não são DTOs, contratos HTTP nem views.

## Resultado do primeiro MVC utilizável (MVP 0)

Uma pessoa se cadastra por CPF ou CNPJ, verifica contato, cria uma loja vazia e recebe o trial único de 30 dias. Entra com e-mail ou documento + senha e confirma segundo fator enviado ao e-mail/celular verificado que escolheu. Configura loja e raio, abastece o cardápio, publica a vitrine no subdomínio; cliente pode consultar, montar carrinho, informar localização/endereço, pedir com pagamento Pix/cartão via provider ou dinheiro na entrega. O dono recebe e atualiza pedidos e o cliente acompanha. A equipe não consegue acessar outra loja trocando URL/IDs.

### Assunções seguras para iniciar (revisáveis por ADR antes de implementar)

1. MVP: um CPF/CNPJ normalizado → uma organização → uma loja → um trial. Expandir para múltiplas lojas exige decisão e migração futuras.
2. Trial começa após e-mail e telefone verificados, cadastro confirmado e loja vazia provisionada; dura 30 dias corridos persistidos em UTC. Sem reset em nova conta com o mesmo documento.
3. Fatura SaaS é manual por ciclo: Pix ou cartão no checkout hospedado; sem débito automático, cartão em vault do Peditto, ou persistência de token reutilizável. Dinheiro na entrega é só compra na loja, nunca mensalidade SaaS.
4. Comprador paga Pix/cartão pelo provedor ou dinheiro apenas na entrega. O Peditto e a loja não armazenam PAN/CVV/PIN nem opção “salvar cartão”.
5. Atraso: 15 dias corridos de tolerância após vencimento, sem multa, juros ou encargos. Ao fim, bloquear pedidos públicos e escritas da loja no backend; manter painel em leitura, cobrança, segurança da conta, logout e exportação autorizada.
6. Cancelamento não tem multa nem renovação futura. Acesso permanece até o fim do período pago; depois a conta fica inativa por até 90 dias, com exportação disponível, seguida de eliminação/anonimização dos dados operacionais.
7. Centro + raio circular é primeira versão de cobertura; endereço é geocodificado e distância verificada no servidor. Sem rastreamento contínuo. Polígonos/ruas excluídas ficam para etapa seguinte.
8. Um preço novo vale prospectivamente a partir da data definida; não altera faturas/períodos já emitidos.

## Regras para organizar o código

Os padrões obrigatórios de legibilidade, comentários, complexidade e escolha de estruturas estão em [`PADROES_DE_CODIGO_E_ALGORITMOS.md`](PADROES_DE_CODIGO_E_ALGORITMOS.md). Devem ser aplicados em todas as fases e revisões; algoritmos do livro são ferramentas para raciocinar sobre custo e estruturas, não motivo para reinventar criptografia ou adicionar complexidade sem necessidade.

```text
backend/src/main/java/com/peditto/
  common/{api,config,errors,persistence,security,tenancy,audit}
  identity/{api,application,domain,infrastructure}
  organizations/{api,application,domain,infrastructure}
  stores/{api,application,domain,infrastructure}
  catalog/{api,application,domain,infrastructure}
  customers/{api,application,domain,infrastructure}
  cart/{api,application,domain,infrastructure}
  orders/{api,application,domain,infrastructure}
  billing/{api,application,domain,infrastructure}
  platformadmin/{api,application,domain,infrastructure}
```

Regras: DTO request/response explícitos; allow-list de campos e unknown-property rejection; `@Transactional` por caso de uso; `BigDecimal`/`NUMERIC`; UTC em banco e timezone por loja; repositories com `tenantId`/ownership obrigatório; Flyway cria/altera DB e Hibernate permanece `validate`; controllers retornam `ProblemDetail`; erros sem stack trace/PII. Use `@ManyToOne` LAZY para referências e `@OneToMany` LAZY só quando agregado precisa navegar filhos. Não usar `CascadeType.REMOVE` em histórico. Relações nunca são serializadas diretamente.

## Sequência de tarefas

### Fase 0 — congelar decisões de MVP e riscos

- [x] Registrar ADRs de tenancy/escopo, identidade/2FA/sessão, billing/pagamentos, localização, console CEO/campanhas e stack: [`docs/decisions/`](decisions/).
- [x] Definir CPF/CNPJ como titular e identificador de login permitido; documento único por organização/trial. Guardar normalizado, restringir acesso, omitir de logs e validar titularidade antes de ativar uso comercial; mudança exige step-up e auditoria.
- [x] Provedores continuam como adapters; escolha final explicitamente adiada com responsável e gates no [`ADR 0007`](decisions/0007-adiamento-de-provedores.md). Nenhum vendor ou dado real será configurado nesta fase.
- [x] Registrar threat model inicial: [`docs/security/THREAT_MODEL_MVP.md`](security/THREAT_MODEL_MVP.md), cobrindo takeover, IDOR/tenant, abuso de trial, OTP, billing, upload, domínio, localização e abuso interno.
- [x] Definir aceite MVP, uso exclusivo de dados sintéticos e invariante backend-first: navegador nunca é fronteira de segurança; autorização, tenant, MFA e regras de negócio são validados no servidor.

**Estado da Fase 0: CONCLUÍDA (14/10/2026).** Decisões de produto, stack, tenancy, identidade, cobrança, localização, console e threat model registrados. A seleção dos providers foi postergada com responsável, critérios e gates no ADR 0007. Revisão dos termos de cobrança sem encargos e da retenção pós-suspensão é gate antes do piloto/cobrança real, não decisão implícita. Nenhuma integração externa ou conta real foi criada.

### Fase 1 — estabilizar banco antes dos endpoints

- [x] V1: organizations, app_users, user_documents, memberships, stores, trials, auth_challenges, store_domains, billing_invoices e audit_events; a tabela `late_charges` foi criada em V1 por uma regra antiga e a migration V9 a remove com guarda contra registros existentes. Entidade JPA `AuditEvent` mapeia a tabela de auditoria.
- [x] Aplicar V1–V8 no PostgreSQL local da porta 15432; Hibernate `ddl-auto=validate` passou. Banco local contém estrutura e dados sintéticos de rate limiting/pré-autenticação, sem cadastro real.
- [x] V3: sessões/refresh revogáveis por hash, reset tokens por hash, outbox com payload ciphertext/key version e deduplicação; OTP fica cifrado no payload e a entrega ocorre pelo worker da Fase 3.
- [x] V2/V3: configurações e zonas de entrega, horários semanais, customer profiles, endereços e consentimento promocional com escopo tenant e validação estrutural.
- [x] V2/V3: categorias, produtos, adicionais e metadados de mídia com FKs compostas; o backend ainda fará sniff/re-encode antes de aceitar mídia na Fase 6.
- [x] V3/V4: clientes, endereços, carrinhos/itens/adicionais, pedidos e snapshots de preço/custo/endereço/contato, eventos imutáveis, pagamentos sem campos de cartão e registros de idempotência únicos por tenant/ator/operação/chave.
- [x] V1/V5: faturas, planos/preços versionados, assinatura, pagamentos de fatura, eventos de provider únicos e ledger append-only. A antiga entidade de encargo diário foi removida do modelo; V9 remove a tabela legado com guarda contra dados existentes. Segredo/assinatura do webhook será aplicado no adapter da Fase 9.
- [x] Papel `peditto_app` sem superuser/CREATEDB/CREATEROLE/BYPASSRLS; migrações usam `peditto_migrator`. V10/V11 codificam RLS/`FORCE ROW LEVEL SECURITY` em 25 tabelas e foram aplicadas/testadas. Não confiar em RLS como defesa contra SQL injection.
- [x] V1: índice único parcial permite no máximo 1 OWNER ativo por organização.
- [x] Modelo de Fase 1 mantém exclusão lógica/histórico por estado e impede exclusão em cascata; onboarding e preservação de OWNER ativo são execução da Fase 3.
- [x] V1/V3/V4/V5: unicidade de CPF/CNPJ/slug/owner/invoice, FKs tenant-aware, evento de provider e chaves idempotentes; audit, order events e ledger rejeitam UPDATE/DELETE no banco. Verificação de propriedade do domínio segue na Fase 5.

**Status da Fase 1: modelo inicial concluído; validação local passou.** V1–V13 estão aplicadas no PostgreSQL local e Hibernate `validate` passou em 15/10/2026. O gate de isolamento A↔B via transaction manager/Hikari também passou; a autorização individual por rota continua obrigatória (ADR 0008).

### Fase 2 — fundação Spring MVC e API segura

- [x] Base da Fase 2: pacote `common/security` com autorização `denyAll` por padrão e method security habilitado; controllers seguem fechados até receberem políticas server-side explícitas.
- [x] Base da Fase 2: `ApiExceptionHandler` retorna `ProblemDetail` genérico, inclui request ID e não ecoa payload/valores; erros internos logam apenas ID e tipo da exceção.
- [x] Base da Fase 2: Jackson rejeita propriedades desconhecidas globalmente; DTOs/mappers ainda serão criados por endpoint e continuam obrigatórios.
- [x] Base da Fase 2: backend nega todas as rotas por padrão, mantém CSRF habilitado, desabilita CORS até allow-list explícita e ativa method security. Forwarded headers continuam desabilitados até existir proxy confiável; rate limit de autenticação já está ativo, e TLS/HSTS permanece gate de deploy.
- [x] Actuator expõe somente `GET /actuator/health`, sem detalhes/componentes; `/actuator/info` e demais endpoints estão desabilitados. Swagger/OpenAPI não foi incluído como dependência.
- [x] Datasource runtime usa `peditto_app` com SELECT/INSERT/UPDATE e DELETE somente no bucket temporário; Flyway usa `peditto_migrator`, ambos sem privilégios de superuser. `.env` local permanece ignorado e ACL restrita; `infra/dev.ps1` aceita allow-list sem imprimir valores, e `.env.example` só tem placeholders. Configuração usa variáveis obrigatórias para segredos.
- [x] Logs ECS JSON sem request body/PII; request ID aleatório; headers `nosniff`, `DENY`, `no-referrer` e `Permissions-Policy`; forwarded headers não são confiados. CORS segue desligado e CSRF ligado.
- [x] Corpo de `/api/**` e `/auth/**` limitado a 2 MiB, inclusive stream sem Content-Length; erro genérico 413. Rate limit Postgres atômico por IP de socket com fingerprint HMAC: 10/minuto e bloqueio de 15 minutos em `/auth/**`; Fase 3 também inclui limites HMAC por documento/e-mail/telefone/destino.
- [x] Gate de produção para TLS/HSTS e injeção via secret manager documentado: aplicar no primeiro deploy, depois da escolha de destino; forwarded headers só com proxy confiável. Métricas operacionais seguem na Fase 11.

**Status da Fase 2: CONCLUÍDA (14/10/2026).** Foram verificados health `UP`, `/actuator/info` e rota privada em `403`, payload acima de 2 MiB em `413`, logs ECS e validação de schema. TLS/HSTS e secret manager estão definidos como gates de implantação e devem ser ativados antes do primeiro acesso público. Cada endpoint futuro exige DTO allow-list, autenticação e autorização server-side; nenhuma rota de negócio está liberada.

### Fase 3 — identidade, cadastro e trial (primeiro vertical slice)

- [x] Migration V9 impõe UNIQUE(organization_id), preserva a chave composta das FKs e remove a tabela legada de encargo somente se vazia. Aplicada e confirmada no PostgreSQL local.

- [x] DTO allow-list e validação server-side de e-mail, E.164, nome, senha, slug e CPF/CNPJ; senha e identificadores são redigidos em `toString`.
- [~] Cadastro `@Transactional` cria organization + AppUser com PBKDF2 versionado + UserDocument + Membership OWNER convidado + Store vazia + settings + OTPs no outbox; unicidade do banco e resposta uniforme cobrem corridas/duplicados. Trial e ativação ficam para depois dos dois contatos verificados.
- [~] OTP de 6 dígitos usa CSPRNG, HMAC fora do DB, expiração de 10 minutos, máximo de 10 tentativas, consumo single-use sob lock, throttling por IP/destino e payload AES-GCM no outbox. Worker agendado e adapters SMTP/Twilio estão implementados; entrega fica desabilitada por padrão e requer configuração válida do ambiente.
- [~] Serviço de login aceita e-mail ou CPF/CNPJ + senha, cria pré-autenticação opaca de 5 min e retorna canais; escolha de canal emite OTP, e sessão opaca de 12 h só nasce após OTP válido. Antes de emitir OTP e antes de criar sessão, estado enabled é relido sob lock. Filtro consulta hash da sessão no banco; cookie `__Host-`, HttpOnly/Secure/SameSite=Lax, logout atual/global e auditoria estão preparados. Rotação substitui token mantendo família; reutilização de token revogado revoga a família ativa.
- [x] Sessão escolhida no ADR 0002: cookie host-only HttpOnly/Secure/SameSite com CSRF e revogação server-side; JWT só se surgir requisito concreto de cliente nativo/integração.
- [~] Recuperação de senha está implementada exigindo os dois canais verificados e revogando sessões. Lista/revogação das próprias sessões, rotação e logout têm endpoints; troca de e-mail/telefone/MFA com reautenticação/step-up ainda não foi exposta.
- [~] Controllers DTO-only para cadastro, verificação, login MFA, sessão e recuperação são condicionais a `OTP_DELIVERY_ENABLED=true`. O endpoint CSRF é sempre disponível, independente dos providers. Default-deny segue ativo para rotas não entregues.
- [~] Smoke checks locais: `/actuator/health` respondeu UP, `/api/v1/auth/csrf` respondeu 200 com cookie CSRF Secure/SameSite, rota privada não reconhecida foi negada em 403 e não emitiu `JSESSIONID`. Isso não substitui fluxo de browser com origem real.
- [ ] Automatizar concorrência CNPJ/CPF/trial, replay/expiração OTP, login MFA, recuperação, CSRF e rotação/revogação de sessão. `mvn test` tem 3 unit tests para rate limit e classificação de conflito único no cadastro; ainda não cobre os fluxos integrados.

**Contratos implementados (provisórios; ainda sem aceite integrado):** `/api/v1/auth/registrations`, verificação, login, escolha/verificação de OTP, recuperação de senha, `/me`, listagem/revogação de sessões, rotação, logout e `/api/v1/auth/csrf`. OTP nunca é retornado no corpo. Falta validar configuração SMTP/Twilio em ambiente protegido e fluxo de browser/CSRF-origin; troca de contato/fator segue bloqueada até step-up, verificação do novo destino, notificação segura e auditoria.

**Pronto quando:** cadastro duplicado falha sem revelar a conta original; usuário não entra só com senha; novo tenant tem loja vazia; nenhum campo `role`, `trial`, `owner`, `verified` ou `tenantId` do payload altera estado privilegiado.

### Fase 4 — tenancy e autorização antes de catálogo — concluída como gate de fundação

- [x] Lookup de domínio customizado verificado e ativo por função restrita no PostgreSQL; encaminhamento de `Host`/proxy não foi ativado nem é confiado.
- [x] `TenantContext` request-scoped: o filtro aceita `X-Organization-Id` apenas como seletor, exige UUID e associação `ACTIVE` com o usuário da sessão, responde 404 quando não autorizado e sempre limpa o ThreadLocal. O filtro roda apenas na cadeia Spring Security; não propagar para thread async.
- [x] `TenantAuthorization` tem allow-list de capabilities, matriz OWNER/MANAGER/STAFF e verificações de propriedade tenant-aware; CEO/plataforma não é papel de membership.
- [x] Filtro valida membership ativo para o tenant selecionado e limpa contexto; contexto GUC é aplicado na transação. O onboarding ativa o tenant recém criado dentro da transação antes de gravar recursos sujeitos a RLS.
- [x] RLS completa nas 25 tabelas, FKs compostas nas relações operacionais e testes A↔B/rollback com conexão reaproveitada.
- [x] Rotas de negócio permanecem em default-deny. A matriz endpoint × capability × recurso e seus testes negativos serão exigidos ao adicionar cada endpoint, sem liberar superfície genérica antecipadamente.

**Pronto quando:** concluído como infraestrutura; novos endpoints só entram após autorização por papel/recurso, DTO allow-list, CSRF e testes negativos de IDOR.

### Fase 5 — setup da loja, domínio e delivery

- [x] Casos de uso e DTOs allow-list para perfil, endereço, timezone, pagamentos e taxa; Jackson rejeita propriedades desconhecidas.
- [x] Horários substituíveis com limite centralizado de quatro intervalos por dia, ordenação e rejeição de sobreposição. O contrato usa dia 0=segunda a 6=domingo.
- [x] Zona circular com limites centralizados e validação server-side de coordenadas/precisão; a distância usa Haversine no backend.
- [x] Checklist de onboarding indica o que falta antes de publicar.
- [x] Public lookup por slug via função `SECURITY DEFINER`; V13 calcula o horário local com timezone da loja e deixa a vitrine informar loja aberta/fechada.
- [x] Fechamento e reabertura manuais da loja; reabertura exige setup completo e cada alteração é auditada.
- [x] Guards privados por `settings:read`/`settings:write`, membership do tenant e CSRF; funções de aplicação também conferem capability no servidor.
- [x] Limites de setup centralizados em `StorePolicy`; V13 substitui e remove a função de lookup anterior. Mudança de limite persistido exige migration aditiva para manter Java e PostgreSQL alinhados.
- [x] Decisão de segurança: rotas de configuração e vitrine continuam inacessíveis por `anyRequest().denyAll()` até nova autorização. Nenhuma alteração no default-deny.
- [x] URL de loja do MVP usa slug. Domínio próprio/subdomínio e DNS ficam opcionais para implantação posterior, dependem de domínio-base e proxy confiável.

**Endpoints preparados e fechados no filtro HTTP:** `/api/v1/stores/current/settings`, `/setup-status`, `/delivery-zone`, `/opening-hours`, `/publish`, `/availability/close`, `/availability/reopen` e `/api/v1/public/stores/{slug}` com `/delivery-check`.

**Status:** Fase 5 concluída. Perfil, checklist, horários, zona circular, consulta pública por slug, status por horário e comandos de fechamento auditados estão implementados. Endpoints permanecem fechados por `denyAll`.

### Fase 6 — catálogo e mídia

- [x] Domínio JPA de categoria/produto/adicional valida nome, descrição, vínculo tenant, preço decimal e quantidade; mutações usam arquivamento lógico e `@Version`.
- [x] DTOs request/response explícitos; nenhum campo tenant, status, versão ou privilégio é aceito para atribuição pelo payload.
- [x] Serviços de categoria, produto e adicional implementam criar/listar/editar/arquivar/restaurar; verificam capability e tenant no servidor, e registram auditoria sem guardar payload.
- [x] Repositórios consultam por organização e loja; relações de categoria/produto/adicional também são resolvidas sob o tenant atual.
- [x] Paginação de produto limitada e ordenação por enum allow-list, com `id` como desempate estável.
- [x] Nenhum controller foi criado; rotas de catálogo permanecem indisponíveis enquanto o default-deny estiver ativo.
- [x] Separar disponibilidade operacional de arquivamento e registrar preço inicial/alterações em histórico tenant-scoped imutável; produto segue sem exclusão física e FK de pedido usa `ON DELETE RESTRICT`.
- [x] Escolher filesystem privado fora do webroot como adapter inicial. Validação permite apenas PNG/JPEG com MIME/extensão/conteúdo coerentes, limita bytes/dimensões/pixels, re-encoda para PNG e grava sob chave aleatória. URLs HMAC expiram em 10 minutos. Flag de upload e multipart continuam desabilitados; nenhum endpoint de upload ou leitura foi criado.
- [x] Criar leitura pública com função SQL `SECURITY DEFINER` e DTOs independentes com allow-list de campos: produtos, preço e adicionais ativos; nenhum campo de custo/margem existe na projeção. A consulta respeita loja e organização ativas e disponibilidade do produto.
- [x] Corrigir a ordem: geolocalização opcional e fallback CEP/endereço ficam na Fase 7; revalidação de entrega e snapshot mínimo ficam na Fase 8.

**Estado:** Fase 6 concluída em domínio, aplicação, persistência e projeção pública. V14–V16 aplicadas e validadas no banco local; rotas continuam fechadas e upload desabilitado por configuração.

### Fase 7 — vitrine, temas por loja, cliente e carrinho

#### Experiência visual e escolha de tema

- [x] Criar a primeira experiência visual React, mobile-first, em `/loja/demo`, com busca, categorias, produtos, detalhes/adicionais, sacola editável, endereço manual, solicitação opcional de localização, estados vazios, motion, foco por teclado e `prefers-reduced-motion`. O Restaurante Dona Erza e seus dados são fictícios; essa tela não está conectada à API nem cria pedidos.
- [x] Criar três presets visuais fechados e responsivos: `PEDITTO_ORIGINAL` (Gelatto Totosu), `FAST_FOOD_RED` (Restaurante Dona Erza) e `CAFE_NATURAL_GREEN` (Muru Muru Coffee). A implementação visual está em CSS escopado; persistência e seleção por loja continuam pendentes.
- [x] Apresentar os três previews na seção `/#temas`; cada card abre uma fixture própria usando o mesmo `StorefrontPage`. A galeria é pública, não exige autenticação e as vitrines de demonstração recebem `noindex`; não há pedidos nem persistência reais.
- [x] Manter `/loja/demo` como alias legado para a demonstração da Dona Erza. Os slugs diretos são `/loja/gelatto-totosu`, `/loja/restaurante-dona-erza` e `/loja/muru-muru-coffee`; todos exibem conteúdo fictício.
- [ ] Criar no painel autenticado a prévia da própria loja e a seleção do preset. Só aplicar após confirmação explícita do proprietário/autorizado; o cliente público vê a seleção salva daquela loja. Trocar tema não pode alterar dados nem aparência de outro tenant.
- [ ] Persistir `store_settings.theme_key` com default `PEDITTO_ORIGINAL`, coluna textual limitada por constraint e enum Java com `@Enumerated(EnumType.STRING)`. API aceita somente chaves allow-listed via DTO estrito; não receber CSS, HTML, classe, URL ou cores arbitrárias.
- [ ] Resolver o tema da loja real no servidor pelo host/slug verificado e tenant correspondente. O frontend mapeia a chave para tokens locais imutáveis/CSS variables; query string, estado React e claims do cliente não definem a identidade visual persistida.
- [ ] Proteger escrita com capability, CSRF, autorização por objeto e escopo tenant; manter rotas fechadas em `denyAll` até aprovação da matriz de autorização. Auditar alterações de tema e testar chave desconhecida, ID/slug adulterado e isolamento A↔B.

#### Catálogo público, cliente e carrinho

- [ ] Conectar a vitrine ao catálogo público tenant-scoped e criar estados reais de loading/erro/loja fechada. A projeção pública nunca inclui custo ou margem.
- [ ] Definir política de conta cliente por loja; guardar telefone/e-mail mínimos, contato verificado quando preciso e endereços por cliente/tenant.
- [ ] Carrinho anônimo com identificador aleatório/expirável e proteção contra troca de id; merge após login com revalidação de preço/disponibilidade.
- [x] Na prévia, localização é solicitada somente após clique explícito; há fallback de endereço manual e o mapa avisa que é ilustrativo. Nenhuma decisão de cobertura é tomada pelo navegador.
- [ ] No fluxo conectado, não confiar em preço, custo, quantidade máxima ou additional IDs enviados pelo front; endpoint público retorna apenas projeção permitida e a disponibilidade/cobertura é validada novamente no checkout.
- [ ] Persistir endereço e coordenadas apenas com finalidade informada, retenção mínima, acesso tenant-scoped e base legal definida; se recusar/indisponível, manter o caminho CEP/endereço manual.
- [x] Aplicar na prévia acessibilidade básica, foco e teclado nos diálogos, estados vazios, responsividade e `prefers-reduced-motion`; manter identidade Cozy Late Night + retro delivery.
- [x] Pausar animações contínuas quando hero/mapa saem da viewport; manter o loading de rede como tarefa posterior à animação principal e somente para espera real.
- [ ] Criar editor de vitrine para logo e textos curtos com limites server-side, contadores e prévia responsiva; regras em [`decisions/0009-vitrine-identidade-e-imagens.md`](decisions/0009-vitrine-identidade-e-imagens.md).
- [ ] Adicionar asset de logo tenant-scoped e fluxo de recorte/compressão no navegador, com revalidação e re-encode independentes no backend; upload continua desligado até revisar a rota.
- [ ] Projetar o bloco “mais pedido” como seleção automática por pedidos concluídos e o segundo destaque como escolha do lojista, sem duplicar produto. Definir janela de popularidade e fallback de loja nova antes de codificar a consulta.

**Organização visual atual:** `frontend/src/data/demo-stores/` guarda fixtures, slugs e metadados da galeria; `frontend/src/assets/demo-stores/` contém apenas mídia de runtime; `frontend/src/styles/storefront/themes/` agrupa as skins; originais e referências ficam em `docs/ux/demo-stores/`, fora de `src/` e `public/`. A configuração de cards da galeria fica em `frontend/src/data/demo-stores/themePreviews.ts`; consulte [`frontend/README.md`](../frontend/README.md) para o mapa de pastas.

**Limites e conclusão:** galeria pública é uma demonstração estática pré-cadastro; prévia do painel é privada e só grava após confirmação; vitrine operacional carrega tema e conteúdo da loja resolvida no servidor. A Fase 7 segue aberta até conectar projeção pública, identidade/carrinho e autorização server-side. Checkout, revalidação final e snapshot permanecem na Fase 8.

### Fase 8 — pedidos e fluxo da loja (MVP de operação)

- [ ] Checkout transacional/idempotente recalcula catálogo, adicionais, entrega, zona, taxa, horário e total.
- [ ] Revalidar no checkout a distância/área de entrega no servidor; persistir apenas o snapshot mínimo de endereço/coord. necessário para cumprir o pedido.
- [ ] Criar pedido e linhas snapshots de produto/adicional, preço/custo, endereço, tipo (delivery/retirada/local), observação, forma de pagamento.
- [ ] Estados e comandos explícitos: pending → accepted → preparation → ready/dispatch → delivered/completed; cancel com motivo e regras.
- [ ] Pagamento Pix/cartão por provider sem cartão salvo; dinheiro só na entrega com troco; payment state separado do order state.
- [ ] Painel React para fila e detalhe; mudanças concorrentes usam `@Version`/lock e order_event; comanda térmica sanitizada.
- [ ] Cliente acompanha apenas o pedido próprio; notificação transacional desacoplada via outbox.

**Pronto quando:** happy path do cliente até loja concluindo pedido e cenário cancelado/refund coerente; nenhum estado impossível via API.

### Fase 9 — planos e cobrança SaaS

- [ ] Modelar planos, preço versionado, entitlement/limites, ciclo, invoice, pagamento e configuração de tolerância.
- [ ] Trial de 30 dias; gerar fatura manual e lembretes D-6, no vencimento e durante atraso por e-mail/SMS com preferências transacionais.
- [ ] Portal cria pagamento Pix/cartão hospedado por provider, sem débito automático e sem método/cartão salvo; guardar referência, evento e comprovante necessário.
- [ ] Implementar tolerância de 15 dias corridos sem multa, juros ou encargos. Não criar job/tabela de cobrança diária.
- [ ] Máquina de acesso explicita `TRIAL`, `TRIAL_EXPIRED`, `ACTIVE`, `PAST_DUE_GRACE`, `SUSPENDED`, `CANCELED` e `RETENTION`; trial expirado sem fatura aceita não é inadimplência.
- [ ] Na suspensão, bloquear checkout/pedidos públicos, alterações e importação; permitir leitura, pagamento, segurança da conta e exportação explicitamente autorizada.
- [ ] Exportar produtos, pedidos e vendas sem dados identificáveis de clientes; escopo tenant, permissão específica, expiração e auditoria.
- [ ] Definir ações permitidas para pedidos aceitos antes da suspensão; pagamento reconciliado reativa automaticamente.
- [ ] Cancelamento sem multa: parar renovação, manter acesso até o fim do período pago, depois manter conta inativa por até 90 dias e eliminar/anonimizar dados operacionais, respeitando retenções legais/contábeis.
- [ ] Webhook valida assinatura/timestamp/idempotência; conciliar pagamento e restaurar acesso conforme fatura quitada.
- [ ] Revisar termos, avisos, retenção, exportação e eliminação com jurídico/contabilidade antes de lançamento.

### Fase 10 — console seguro de CEO (administração da plataforma)

**É seguro ter controles de CEO se forem tratados como superfície crítica separada.** Não dar à conta de CEO um bypass silencioso da autorização tenant. Separar operador Peditto dos usuários de loja.

#### Identidade e limites

- [ ] Tabelas/contexto próprios `platform_operators`, grants/roles e audit de plataforma; sem usar `MembershipRole` da loja.
- [ ] Papéis mínimos: `PLATFORM_OWNER`, `PLATFORM_BILLING`, `PLATFORM_CAMPAIGNS`, `PLATFORM_ANALYTICS`, `PLATFORM_SUPPORT`; permissão por ação, acesso temporário e revisão periódica.
- [ ] MFA obrigatório, step-up para preço/campanha/exportação/suspensão, sessão curta e alertas de login; IP/VPN allow-list apenas como camada extra, nunca auth única.
- [ ] Registrar ator, motivo, request id, antes/depois e resultado em audit append-only; impedir que o mesmo endpoint se auto-promova.
- [ ] Suporte vê dados mínimos; abrir detalhe de tenant exige justificativa, tempo limitado e trilha; impersonation desligada por padrão.

#### Preços e promoções de assinatura

- [ ] `plans` e `plan_price_versions` com effective-from/to; nenhuma alteração retroativa de invoices.
- [ ] CRUD de planos com preview de lojas afetadas, comparação de preço, data de vigência, confirmação/step-up e audit trail.
- [ ] Promoções com elegibilidade, janela, plano, desconto máximo, limites/uso, código, orçamento e termos; não editar invoice já paga.
- [ ] Exibir métricas de trials/ativos/atrasos/cancelamentos/recuperação; distinguir receita faturada e recebida; receita reconhecida vem de webhook reconciliado, desconta reembolsos e não soma compra de delivery de cliente como receita SaaS.

#### Campanhas promocionais por e-mail

- [ ] Consentimento de marketing separado das notificações operacionais; checkbox desligado por padrão e registro de finalidade, texto/versão, horário, origem e canal.
- [ ] Lista de supressão/unsubscribe, link de descadastro em todo e-mail de marketing, opt-out imediato e não reimportar endereço removido.
- [ ] Segmentação só entre pessoas que aceitaram marketing; não mostrar/baixar lista geral de e-mails. Não incluir dados de pedido/CPF/localização sem necessidade.
- [ ] Campanha é job/outbox assíncrono: audiência estimada, preview e envio de teste, limite de velocidade, deduplicação, idempotência, pausa/cancelamento e relatório de bounce/complaint/unsubscribe.
- [ ] Separar reputação/domínio/transporte de e-mail transacional (OTP/fatura/pedido) e marketing. Controle de CEO nunca pode disparar OTP por ferramenta de campanha.

#### Indicadores e faturamento SaaS

- [ ] Dashboard agregado: lojas ativas, pedidos por período, retenção, trial→pago, MRR/receita recebida, atraso, reembolso e chargeback (conforme provider).
- [ ] Top lojas por volume de pedidos/receita da loja em painel agregado, com definição explícita da métrica, janela e timezone.
- [ ] Separar receita Peditto (faturas SaaS pagas, líquidas de reembolso/estorno) de faturamento do lojista com pedidos. CEO vê agregados por padrão.
- [ ] Exportação precisa de permissão específica, finalidade, limite, expiração e audit; jamais disponibilizar cartão, senha, OTP, endereço de cliente ou dado de pagamento sensível.

**Pronto quando:** role de loja não abre console Peditto, conta plataforma não recebe autorização automática em todo tenant, mudanças de preço/campanha ficam versionadas e auditadas, métricas usam evento financeiro reconciliado, campanhas respeitam opt-in/supressão e podem ser interrompidas.

### Fase 11 — analytics e operação

- [ ] Faturamento da loja por períodos/método/estados com receita, taxas, cancelamentos e reembolsos definidos.
- [ ] Job/worker e outbox com retentativa, DLQ, idempotência, alertas e monitoramento de lag.
- [ ] Logs, tracing, métricas, health e dashboards; nenhuma PII/credencial/OTP em logs.
- [ ] Backups criptografados, PITR, restore drill, RPO/RTO e runbooks.
- [ ] Retention, export/deleção, suporte LGPD, incidente de segurança e contato para denúncia.

### Fase 12 — segurança, qualidade e lançamento

- [~] Revisão inicial de legibilidade aplicada ao backend e primeiro frontend implementado em 14/10/2026. Revisão contínua, integração ponta a ponta e gates de produção continuam pendentes.
- [ ] Testes unitários das regras de domínio e estados.
- [ ] Integração com PostgreSQL real (Testcontainers), Flyway clean/upgrade, concorrência em cadastro/trial/pagamento e lock de pedido.
- [ ] Segurança: DTO unknown fields, matriz RBAC, IDOR tenant A↔B em cada recurso, OTP replay/expiry/brute-force, rate limit, CSRF/CORS, upload, webhook replay.
- [ ] E2E React de onboarding → publicar loja → cliente pedir → loja concluir → invoice manual.
- [ ] Pipeline: format/lint, build, testes, migration verify, dependency/secret scan, SBOM, imagens assinadas e deploy controlado.
- [ ] Threat review externo antes de produção; pentest das rotas de admin, tenant, billing, upload e host resolver.
- [ ] Go-live controlado com dados sintéticos, monitoramento, rollback de app (migrations expand/contract) e restore comprovado.

### Fase 13 — apresentação pública, SEO e aquisição orgânica

- [~] Página inicial de apresentação criada com pitch direto, CTA de teste, benefícios reais e mascote oficial. Validar textos com lojistas antes da publicação comercial.
- [x] Registrar a leitura visual da mascote, paleta oficial, tom do pitch e regras de marca em [`docs/brand/PITCH_E_IDENTIDADE_VISUAL.md`](brand/PITCH_E_IDENTIDADE_VISUAL.md).
- [x] Favicon da mascote, título e meta description base adicionados; login/cadastro devem receber `noindex` e nunca entrar no sitemap público.
- [ ] Confirmar domínio/canonical e ambiente público; servir a página de marketing com HTML renderizado no servidor ou prerender estático, sem depender da execução de JavaScript para título, conteúdo e links principais.
- [ ] Criar pesquisa de palavras-chave por intenção em português brasileiro para sistema de delivery de restaurante, cardápio digital, pedidos online, delivery próprio e gestão de pedidos. Escrever páginas distintas para públicos/necessidades reais; evitar keyword stuffing e alegações sem prova.
- [ ] Implementar `robots.txt`, sitemap XML de URLs canônicas, metadados únicos por página, Open Graph com URL absoluta, favicon nos tamanhos/documentos recomendados e `hreflang` apenas se houver outras localidades/idiomas.
- [ ] Adicionar dados estruturados somente quando fatos, domínio, preço e oferta estiverem confirmados; validar JSON-LD e conferir páginas indexáveis no Google Search Console.
- [ ] Garantir acessibilidade semântica, mobile, HTTPS e Core Web Vitals; monitorar com dados reais (referências de bom resultado: LCP ≤ 2,5 s, INP < 200 ms, CLS < 0,1), sem prometer posição/ranking.
- [ ] Publicar conteúdo útil baseado em conhecimento real da operação (organização de pedidos, raio de entrega, cardápio e rotina de loja); medir impressões, cliques, conversões e cadastro consentido, revisando conteúdo que não atende à busca.
- [ ] Criar Google Business Profile apenas se houver negócio/local elegível conforme as regras do Google; o SaaS não deve inventar endereço local para obter ranqueamento.

**Pronto quando:** páginas públicas podem ser rastreadas e indexadas pelo domínio final, páginas privadas estão fora do índice, conteúdo responde buscas reais sem copiar marcas concorrentes, métricas de tráfego/conversão têm consentimento e nenhum texto promete recurso/resultado inexistente.

## Invariante: segurança no backend

Frontend nunca decide acesso, papel, tenant, MFA, preço, status, trial, disponibilidade ou propriedade. Ocultar controles na interface é apenas UX; toda rota, transição de estado e consulta valida autenticação, autorização, tenant e regras no servidor. Valores enviados pelo cliente são sempre input não confiável, mesmo quando a interface produz o payload.

## Do que não abrir mão

- Entidade não é DTO; UI não é autorização.
- Toda tabela de loja tem tenant e toda query privada confirma ownership/papel.
- Tudo que muda acesso/dinheiro/campanha tem step-up e auditoria.
- Nunca persistir cartão; pagamento SaaS é manual; pagamento de pedido passa por provedor.
- Não mandar propaganda a quem não optou; contato promocional é independente do necessário para conta/pedido.
- Não executar migration destrutiva nem remover histórico financeiro.
- Não implementar console de CEO antes de haver autenticação plataforma, RBAC separado e audit log confiável.

## Backlog de segurança adaptado do MangaTeofilo

A análise completa, limites e mapeamento Java/PostgreSQL estão em [`docs/security/REUSO_DAS_DEFESAS_MANGATEOFILO.md`](security/REUSO_DAS_DEFESAS_MANGATEOFILO.md). Itens abaixo devem ser concluídos dentro das fases existentes e antes de liberar a respectiva superfície. Não duplicar controles como se estivessem prontos: os itens marcados são tarefas a implementar ou validar.

### Fase 1 — persistência e invariantes

- [x] Sessões/reset revogáveis por hash, outbox cifrado, rate bucket, idempotência e eventos externos com unique keys persistidos; não armazenar OTP/token reutilizável em claro.
- [x] FKs e constraints tenant-aware/únicas para documentos, carrinho/pedido, período de invoice, eventos externos e idempotência; valores financeiros em `NUMERIC` mapeados a `BigDecimal`.
- [x] Ledger, auditoria e eventos do pedido append-only impostos por triggers; bucket de abuso tem fingerprint HMAC e expurgo após um dia.
- [x] Nenhuma coluna ou entidade guarda PAN/CVV/token reutilizável; guardam-se somente provider/reference/estado/valores necessários.

### Fase 2 — contratos e segurança HTTP

- [x] Jackson rejeita campos JSON desconhecidos e entidades não são retornadas diretamente; DTO allow-list continua obrigatório por endpoint, e ainda não há endpoint de negócio para demonstrar o mapeamento.
- [x] Limite de corpo, errors ProblemDetail e logs sanitizados estão implementados; allow-list de content type, paginação e ordenação será definida junto aos endpoints.
- [x] Até existir proxy confiável, usa-se apenas IP do socket e `server.forward-headers-strategy=none`; bucket HMAC atômico protege `/auth/**` (10/min, bloqueio 15 min, expurgo diário). Fase 3 adicionou bucket HMAC por e-mail/telefone/documento/destino e limites por conta.
- [x] Fingerprint de IP usa HMAC-SHA256 com segredo local separado, sem armazenar IP; rotação planejada invalida buckets ativos. CAPTCHA adaptativo só será avaliado na Fase 3.
- [x] CORS desligado, CSRF ligado, headers de API configurados e forwarded headers não confiáveis; TLS/HSTS será aplicado na borda após escolha do deploy. Origin não concede autorização.
Parser CPF/CNPJ e verificação de titularidade estão corretamente alocados na Fase 3; o banco já força unicidade e comprimento.

### Fase 3 — cadastro, autenticação e sessão

- [~] OTP de verificação/login usa hash/HMAC, expiração, throttling, lock single-use, outbox cifrado e auditoria; worker SMTP/Twilio e controllers existem, com entrega desligada até configuração de ambiente.
- [~] Sessão opaca server-side, cookie seguro, logout, rotação/reuse, recuperação com os dois canais e gestão de sessões próprias têm serviços e rotas; validação integrada e troca segura de contato/MFA faltam.
- [ ] Não conceder papel por payload ou claim desacompanhada de fonte de autoridade vigente no servidor.

### Fase 4 — tenancy e autorização — fundação concluída

- [x] Default-deny e matriz inicial de capability existem; a autorização por objeto será condição obrigatória para qualquer endpoint futuro de leitura/escrita/exportação/mídia/transição.
- [ ] Roles de `platform_operators` separados de `memberships`; sem fallback de claim para superadmin, sem herança automática de acesso às lojas e sem autoelevação. MFA/step-up para ações críticas.
- [x] Repositories existentes usam escopo tenant; contexto vem de membership ativa. IDs opacos não substituem ownership. Novos repositories devem manter o mesmo padrão.
- [ ] Slug e domínio normalizados, allow-list/reservas e verificação de propriedade; comparar host/origin exatos após parsing, sem prefix match. Host encaminhado apenas de proxy confiável.
- [x] V10/V11 ativam/forçam RLS em 25 tabelas; transaction manager define GUC tenant-local em cada transação. Testes integrados confirmam onboarding, GUC/Hikari, A↔B, função de lookup público e rollback.
- [ ] DTOs de saída mínimos por papel; revisar explicitamente CPF, contatos, endereço, custo/margem, hashes, tokens e dados de pagamento para impedir vazamento/IDOR.

### Fase 6 — uploads

- [ ] Aceitar apenas formatos definidos, tamanho/dimensões limitados; conferir magic bytes e decodificar/re-encodar imagem raster no servidor; rejeitar SVG/conteúdo ativo e filename do usuário.
- [ ] Object key aleatória, storage privado, acesso assinado e curto após autorização tenant; não buscar URL remota arbitrária.
- [ ] SSRF só vira requisito se um fluxo realmente fizer fetch backend de URL externa; nessa hipótese, allow-list, validação DNS/IP privada/link-local/metadata, redirects controlados e egress deny.

### Fase 8 — pedidos e pagamentos de cliente

- [ ] Checkout DTO aceita apenas produto/adicional, quantidade, opção, endereço e forma de pagamento permitidos; recalcular preço, disponibilidade, taxa, raio e total server-side no momento de confirmar.
- [ ] Idempotência atômica no PostgreSQL por tenant/ator/operação/chave: mesmo fingerprint repete resposta, fingerprint diferente retorna `409`; transaction + outbox e chave idempotente no provider impedem pedido/cobrança duplicados.
- [ ] Webhook falha fechado sem segredo; verificar HMAC com comparação timing-safe, timestamp conforme protocolo, event ID único, replay e estado consultado no provider. Nunca confirmar dinheiro só por redirect/browser.
- [ ] Conciliar provider com ledger imutável; usar `NUMERIC`/`BigDecimal`; divergência alerta primeiro. Reparo exige lançamento compensatório auditado e revisão, não sobrescrita silenciosa.
- [ ] Pix de comprador ocorre via provider; não armazenar PAN/CVV/token. Dinheiro só na entrega conforme estado permitido.

### Fase 9 — cobrança SaaS

- [ ] Reusar idempotência/webhook/ledger da Fase 8 em faturas e eventos SaaS, com chaves e escopo próprios; garantir evento de provider único por fatura; não calcular multa nem juros.
- [ ] Reconciliar receitas recebidas, estornos e reembolsos; diferenciar faturamento Peditto de vendas das lojas; não existe receita de multa ou juros por atraso.

### Fase 10 — console CEO

- [ ] Resolver operador e grants no registro server-side; negar por padrão, MFA obrigatório e step-up para preço, promoção, suspensão, export e dados privados; escopo mínimo, justificativa e audit append-only.
- [ ] Métricas agregadas por padrão. Acesso excepcional a tenant é temporário, motivado, registrado e sem impersonation habilitada por padrão.

### Fase 11 — operação

- [ ] Alertar sobre abuso, falhas OTP/webhook, divergência de ledger e mudanças de privilégio; não registrar documento, OTP, token, segredo, endereço preciso nem conteúdo de cartão.
- [ ] Rotação de segredos, backups criptografados, restore testado, retenção/expurgo e runbook de revogação/incident response.

### Fase 12 — validação antes do lançamento

- [ ] Testes server-side de unknown fields/mass assignment, adulteração de preço, cadastro concorrente de mesmo CPF/CNPJ, OTP replay, brute force, revogação, IDOR cross-tenant e escalação de papel.
- [ ] Testar duas requisições concorrentes com a mesma chave de idempotência; repetir webhook válido e inválido, assinatura ausente, replay e evento fora de ordem.
- [ ] Testar uploads com MIME falso, SVG, arquivo excessivo e acesso cruzado entre tenants; teste SSRF apenas se existir fetch remoto.
- [ ] CI com scanner de segredos que não imprime valor detectado, varredura de dependências/SBOM separada, build/migrations e testes de segurança. Tratar secret encontrado como comprometido e rotacionar.
- [ ] Revisão independente de rotas CEO, auth, tenant, upload e pagamentos antes do piloto; registrar riscos residuais e resposta operacional.
## Estado a atualizar a cada sessão

### Revisão de código e legibilidade — 14/10/2026

- [x] Reformatar modelos JPA compactados para separar anotações, campos e regras de domínio; explicitar condições de validação do raio de entrega com limites nomeados.
- [x] Centralizar a atualização atômica de buckets PostgreSQL de rate limit em `RateLimitBucketStore`, removendo SQL duplicado entre o filtro por IP e o limitador de identidade.
- [x] Remover a entidade JPA `ApiRateLimitBucket`, sem consumidores, e associações inversas não usadas em `AppUser`/`Organization`; o schema continua sendo gerenciado pelo Flyway.
- [x] Restringir captura de falhas do rate limiter a falhas de criptografia/acesso ao banco para não ocultar exceções de programação como indisponibilidade controlada.
- [x] Formatar `pom.xml` e trechos compactados de filtros/repositories. Varredura não encontrou `@Data`, `@Autowired` em campo, `BeanUtils`, `System.out/err`, `TODO` ou `FIXME` em código de produção.
- [x] `mvn -q -DskipTests compile` e início da aplicação em porta temporária passaram; Flyway confirmou V1–V8 e Hibernate validou as entidades mapeadas.
- [~] Primeiro frontend de identidade criado e build validado; painel, catálogo, pedido, acessibilidade com leitor de tela e integração real permanecem nas próximas fases. Testes automatizados e revisão de contrato continuam pendentes.

- **Concluído:** Fases 0–2 encerradas com gates futuros explicitados; schema base (Flyway V1–V8), incluindo migrations aditivas V7–V8 de OTP e pré-autenticação; fundação de segurança Fase 2; compile, startup em porta efêmera, Hibernate `validate` e checagens HTTP pontuais.
- **Banco local:** PostgreSQL 17 na porta 15432; `peditto_app` sem privilégios elevados, `peditto_migrator` separado, `.env` ACL apenas para a conta Windows atual e ignorado pelo Git.
- **Decisões de billing atualizadas:** não há multa, juros ou encargo por atraso; há tolerância de 15 dias, suspensão, exportação sem dados de clientes e retenção de até 90 dias após suspensão/cancelamento. Ver ADR 0003. Essas regras documentais ainda precisam ser implementadas e não indicam que os fluxos já existem.
- **Estado em 15/10/2026:** fundação da Fase 4 concluída; V1–V16 aplicadas e startup/Hibernate validate passaram; gates prévios de GUC/Hikari, A↔B e rollback passaram. Fase 3 segue aberta por integração OTP/browser, step-up e concorrência. Fases 5 e 6 concluídas no backend; rotas continuam fechadas e upload continua desabilitado. Ver [`REVISAO_TECNICA_E_PENDENCIAS.md`](REVISAO_TECNICA_E_PENDENCIAS.md).
- **Bloqueadores para produção:** validar credenciais/entregabilidade OTP em ambiente protegido; validar CSRF/browser e rotação de sessão; troca segura de fatores; testes automatizados de autenticação, concorrência e cross-tenant; completar cobertura RLS, conectar autorização a cada endpoint/recurso; proxy/TLS/secret manager, observabilidade e revisão jurídica.












