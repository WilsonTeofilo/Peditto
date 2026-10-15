# Peditto — Arquitetura e plano de reconstrução SaaS

> Documento de arquitetura, plano e estado da implementação.
> O produto usa Java + Spring Boot, React + TypeScript + Vite e PostgreSQL, com API REST e isolamento entre lojas. O frontend atual usa CSS modular e tokens em CSS variables; Tailwind não está instalado.

## Estado da implementação — 15/10/2026

A fundação usa Java 21 + Spring Boot 4.1.1, JPA e Flyway. V1–V16 foram aplicadas no PostgreSQL local 17.11; Flyway validou as migrations, Hibernate `ddl-auto=validate` passou e a aplicação iniciou em porta aleatória. `peditto_app` não tem `SUPERUSER`/`BYPASSRLS` e RLS está forçada em tabelas tenant-owned. Fases 4, 5 e 6 estão concluídas no backend. A API continua inacessível por `anyRequest().denyAll()` e uploads continuam desligados. A Fase 3 segue aberta para validação integrada de MFA, step-up, provedores externos e concorrência. A Fase 7 tem uma experiência visual funcional de demonstração com três lojas fictícias, mas ainda não está integrada à API nem aceita pedidos reais. Consulte o [handoff para o próximo chat](docs/CONTEXTO_PARA_NOVO_CHAT.md), o [relatório técnico](docs/REVISAO_TECNICA_E_PENDENCIAS.md) e o [roteiro de fases](docs/README_ROTEIRO_MVC_E_SAAS.md).

As regras de legibilidade, comentários enxutos, manutenibilidade e uso criterioso de estruturas/complexidade algorítmica estão em [`docs/PADROES_DE_CODIGO_E_ALGORITMOS.md`](docs/PADROES_DE_CODIGO_E_ALGORITMOS.md). São parte do padrão de implementação para backend e frontend.

Em 15/10/2026, a vitrine demonstrativa foi organizada para reutilizar um esqueleto React com fixtures de conteúdo e três skins independentes. A documentação de arquitetura, identidade e fases descreve os limites entre demonstração, produto conectado e segurança. Consulte o handoff antes de continuar; ele aponta os documentos de referência e o estado local do Git.
## 1. Objetivo e premissa

O projeto de origem, **Chokko Melt**, é um sistema web de pedidos e gestão para confeitaria, criado como TCC e estudo de caso de um negócio no Grajaú, São Paulo. A nova iniciativa, em `D:\Projetos\Peditto`, transforma essa premissa em um SaaS de delivery/gestão que possa atender várias lojas independentes.

Cada loja que se cadastra começa com seu próprio espaço vazio e abastece seu catálogo, configurações, equipe e operação. Não se deve clonar catálogo de demonstração nem compartilhar pedidos entre lojas. Cada estabelecimento tem endereço próprio, por exemplo `minhaloja.peditto.com`, além do painel do dono. O hostname é resolvido no servidor para um `tenant_id`; ele nunca é aceito como autorização por si só. Toda consulta e toda mutação de recurso da loja também exigem identidade autenticada, papel/permissão válida e pertencimento ao tenant.

O cadastro permite identificar o responsável/negócio por CPF ou CNPJ, normalizado e validado no servidor. O documento deve ser único no conjunto de organizações/contas elegíveis ao trial; CPF identifica cadastro individual e CNPJ identifica pessoa jurídica. A constraint de unicidade no banco é a última barreira contra corridas. Um mês de avaliação gratuita é concedido uma única vez por documento/negócio elegível, com início, fim e estado persistidos no servidor. Não permitir reiniciar trial apagando e recriando usuários; alterações excepcionais de titularidade/documento exigem verificação e fluxo auditado de suporte. Planejar cobrança recorrente como obrigação manual por ciclo, sem débito automático nem armazenamento de cartão.

## 2. Leitura do sistema existente

A leitura abrangeu os READMEs, roadmap e documentação de regras, guia de integração JS/PHP, dicionário e DDL MySQL, estrutura completa de arquivos e fluxos PHP centrais de autenticação, pedido/carrinho, gestão de produto e status do pedido.

### Propósito e atores atuais

- **Cliente final:** consulta cardápio público, monta carrinho, autentica ao fechar pedido, escolhe delivery/retirada/consumo local, acompanha pedido e mantém perfil/endereço.
- **Administrador/dono e funcionário:** recebe e atualiza pedidos, administra catálogo e adicionais, consulta faturamento, gerencia pessoas/permissões e configuração da loja.
- **Operação:** impressão térmica de comanda, horário de funcionamento e taxa de entrega.
- **Financeiro:** registros de forma/status do pagamento e, em parte da implementação, custos e snapshots para margem.

### Regras de negócio encontradas

- Cardápio navegável sem login; autenticação necessária para concluir o pedido.
- Fluxo de pedido: carrinho → checkout → pedido pendente → aceito/em preparo → enviado/retirada → entregue/concluído, ou cancelado com motivo.
- Formas registradas: Pix, débito, crédito e dinheiro; dinheiro pode registrar valor entregue e troco. Os documentos dizem que não há gateway/pagamento online.
- Recebimento: delivery, retirada ou consumo local; delivery pode usar endereço e taxa configurada.
- Referências legadas: Grajaú-SP, funcionamento 15h–22h, limite de 10 unidades por produto/opção e até 10 produtos distintos. São regras do estudo de caso e devem virar configurações por loja ou limites explícitos de domínio, não constantes globais do SaaS.
- Produto indisponível é ocultado/congelado sem apagar histórico; pedido deve preservar snapshot de nome, preço e adicionais mesmo após alteração do catálogo.
- Cancelados não compõem faturamento realizado. O legado tem escopo documental conflitante: um texto fala apenas em faturamento bruto, outro descreve custo/lucro. No novo produto, relatório financeiro precisa declarar fórmula e estados considerados; manter margem como recurso opcional e baseada em snapshots auditáveis.
- Configuração existente cita WhatsApp, horários e taxa padrão. A avaliação por pedido aparece no banco/dicionário, mas não como requisito central estável.
- A loja recém-criada no SaaS deve começar vazia; os dados iniciais existentes (categoria, produto, adicional e usuário admin local) são somente fixtures legadas e não devem ser copiados para novos tenants.

### Estado e limites observados no legado

- PHP procedural/OO simples, HTML renderizado no servidor e JavaScript com `fetch`/AJAX; MySQL/InnoDB; pastas `user`, `admin`, `classes`, `includes`, `database`.
- Há classes para autenticação, carrinho, endereço, categoria e produto. Os arquivos de ação PHP fazem validação, SQL, upload, sessão, resposta e fluxo visual no mesmo lugar.
- O DDL contém entidades de loja/configuração, usuário interno, cliente, endereço, categoria, produto, adicional, carrinho, pedido, itens, pagamentos, status e avaliação; catálogo e usuários são globais porque o sistema original servia uma única confeitaria.
- README e guias divergem sobre funções concluídas: algumas partes são chamadas de mock/pendentes, enquanto SQL e ações de pedido/produto já existem. Isso exige inventário de comportamento e migração consciente, sem declarar cada requisito legado como pronto ou manter inconsistências.
- O login mistura cliente e administrador no mesmo fluxo e há permissões legadas baseadas em campos/strings. Isso deve ser substituído por identidades, membership por loja e autorização no servidor.
- Upload legado usa extensão/nome e pasta local; novo SaaS deve armazenar mídia fora do webroot, validar conteúdo real e servir por storage/CDN controlado.
- O SQL MySQL legado contém `DROP DATABASE`; nunca executar isso na nova base. PostgreSQL deve ser criado apenas por migrações versionadas, não por dump destrutivo.

## 3. Produto SaaS: conceitos e fronteiras

Separar claramente três superfícies:

1. **Marketing e aquisição:** página pública, demonstração visual, preços/planos e cadastro de empresa.
2. **Console Peditto:** conta, segurança, assinatura/trial, gestão de membros e acesso direto à única loja da organização.
3. **Loja pública e painel da loja:** `slug.peditto.com` para vitrine e rotas de administração dessa loja, com sessão e contexto de tenant corretos.

O cadastro inicial deve criar, em transação:

- `Organization`/negócio, com tipo de documento (`CPF` ou `CNPJ`) e valor normalizado único;
- usuário proprietário (`User`) e associação `Membership` com papel `OWNER`;
- um `Store` com slug único e estado inicial `SETUP_REQUIRED`/`DRAFT`;
- configuração mínima e trial de 30 dias, sem produtos, categorias, adicionais, pedidos nem clientes falsos.

Cadastro não deve ativar automaticamente pedidos se perfil, endereço/entrega, horário e políticas mínimas não estiverem configurados. Onboarding guiado explica o que falta. Regra definida: um CPF ou CNPJ normalizado corresponde a uma organização, uma loja e um único trial. A cardinalidade é imposta pelo banco com `UNIQUE(store.organization_id)` e validada no onboarding; o seletor de lojas não existe no MVP. Uma expansão futura exige nova decisão de produto e migração explícita. Para conter abuso de trial, combinar unicidade do documento com verificação de e-mail e telefone, sinais de risco e revisão de exceções. IP/dispositivo são sinais de abuso, nunca identificadores únicos nem motivo isolado de bloqueio permanente.

### Identidade e isolamento por hostname

- Produção: `loja.peditto.com`; domínios customizados ficam para etapa posterior, com verificação de posse e proteção contra takeover.
- `TenantResolutionFilter` lê host normalizado, resolve slug/domínio ativo e coloca `TenantContext` request-scoped. Rejeitar host desconhecido, ambíguo, domínio não verificado ou tenant suspenso.
- `TenantContext` é apenas contexto de resolução, nunca prova de acesso. Recursos privados verificam simultaneamente tenant da rota/host, membership ativo do usuário e permissão sobre o recurso.
- Rotas de plataforma (superadmin/suporte) ficam em domínio e guard separados, com MFA obrigatório, menor privilégio, acesso de rede restrito quando viável e auditoria. Não reutilizar papel de loja como papel de operador Peditto.
- No PostgreSQL usar `tenant_id NOT NULL` em toda tabela tenant-owned, FKs compostas `(tenant_id, id)` para impedir vínculos cruzados, índices começando por `tenant_id`, e Row-Level Security como defesa adicional. A aplicação configura tenant na transação e usa role do banco sem `BYPASSRLS`; nunca confiar só em RLS/contexto de conexão com pool.
- Separar dados globais de identidade/empresa de dados da loja. Clientes compradores são isolados por loja; decidir explicitamente se uma conta de cliente pode ser reutilizada em lojas diferentes, evitando compartilhar histórico/PII entre estabelecimentos.

## 4. Arquitetura proposta

### Monólito modular primeiro

Começar com um **monólito modular** com fronteiras claras, API stateless e processamento assíncrono para tarefas laterais. Evitar microserviços prematuros; pedidos, tenant e transação precisam de consistência. Módulos de domínio:

- `identity`: usuários, credenciais, MFA, sessões, recuperação e verificação de contato;
- `organizations`: empresa/CNPJ, memberships, convites e papéis;
- `tenancy`: resolução de domínio, contexto, políticas e guardas de isolamento;
- `stores`: perfil, slug, endereço, horários, entrega e estado operacional;
- `catalog`: categorias, produtos, adicionais, mídia e disponibilidade;
- `cart`: carrinho e itens;
- `orders`: checkout, snapshots, estados, eventos, cancelamentos e comanda;
- `payments`: intenção/registro manual, forma, estado e reconciliação; gateway só quando planejado;
- `customers`: comprador, endereço e privacidade;
- `finance`: relatórios derivados de pedidos/pagamentos;
- `subscriptions`: trial, plano, limites e estados de acesso;
- `notifications`: e-mail, notificações e tarefas assíncronas;
- `audit`: trilha de ações críticas e eventos de segurança.

### Camadas por módulo

`api/controller → application/service (casos de uso) → domain (regras/invariantes) → infrastructure (JPA, PostgreSQL, storage, e-mail)`.

- Controllers REST finos: HTTP, DTO, status code e headers; sem regra ou SQL.
- DTOs específicos de request/response por caso de uso; nunca expor entidades JPA.
- Services transacionais orquestram casos de uso e autorização de objeto.
- Domínio concentra estados válidos, cálculos monetários, transições e invariantes.
- Repositories expressam consultas de domínio e recebem `tenantId` explicitamente; nunca `findById(id)` em dado de tenant sem escopo/propriedade.
- Infraestrutura implementa persistência, migrations, storage, e-mail e integrações.
- `@ControllerAdvice` converte erros em `ProblemDetail` RFC 9457 sem stack trace, SQL ou segredo ao cliente.
- Validação em DTO (`jakarta.validation`), regras de domínio no serviço e constraints no PostgreSQL: as três camadas têm papéis distintos.

### Stack base

- Java LTS atual suportado pelo ambiente na época da implementação; Spring Boot, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Bean Validation, Actuator restrito.
- PostgreSQL com Flyway (ou Liquibase, escolher um só), transações e `NUMERIC` para valores monetários; nunca `float/double` para dinheiro.
- React + TypeScript + Vite; CSS modular com tokens em CSS variables, cliente HTTP centralizado e validação UX que não substitui backend. Não há Tailwind no frontend atual.
- OpenAPI/Swagger apenas em ambiente autorizado e sem expor endpoints internos/segredos.
- Docker Compose para desenvolvimento; produção com TLS, proxy confiável, gestão de segredos, backups e observabilidade.
- Dependências fixadas, atualização automatizada, análise de vulnerabilidade e SBOM no pipeline.

## 5. Modelo relacional inicial (PostgreSQL)

Nomes podem seguir padrão consistente em inglês ou português; escolher e manter. IDs UUID são adequados para expor recursos sem sequenciais previsíveis, mas não substituem autorização. Timestamps em UTC (`timestamptz`); timezone de exibição por loja.

### Plataforma e billing

- `organization(id, document_type, document_normalized UNIQUE, legal_name, trade_name, status, created_at)`; aceitar CPF ou CNPJ, normalizar e validar no servidor, nunca registrar o número em logs. Restringir acesso, definir retenção e avaliar criptografia de campo; validação matemática não comprova titularidade.
- `app_user(id, email_normalized UNIQUE, phone_e164 UNIQUE, password_hash, email_verified_at, phone_verified_at, enabled, created_at)` + `user_document(id, user_id, document_type, document_normalized UNIQUE)` para login por e-mail ou documento pessoal verificado; a resolução de CNPJ de organização para usuário owner só ocorre se essa opção for habilitada e não ambígua. Nunca expor qual identificador está cadastrado. Credencial separada de perfil.
- `membership(id, organization_id, user_id, status, created_at, UNIQUE(organization_id,user_id))`; `membership_role`/políticas de papéis controladas pelo servidor.
- `store(id, organization_id, slug UNIQUE, display_name, status, timezone, created_at, UNIQUE(organization_id))`: uma loja por organização no MVP.
- `trial(id, organization_id, starts_at, ends_at, status, granted_reason, UNIQUE(organization_id))` (trial único; extensão é ação administrativa auditada, nunca endpoint público). Registrar também sinais/decisões antifraude minimizados e com retenção definida, sem transformar fingerprint invasivo em requisito padrão.
- `subscription(id, organization_id, plan_id, status, period_start, period_end, provider_reference)`; pagamentos e webhooks idempotentes se billing entrar no escopo.
- `store_domain(id, store_id, hostname_normalized UNIQUE, verified_at, status)`.

### Loja e operação

- `store_settings(store_id UNIQUE, currency, timezone, opening_hours, ordering_enabled, delivery_policy, default_delivery_fee, contact, updated_at)`.
- `delivery_zone(id, tenant_id, store_id, center_lat, center_lng, radius_meters, active, version)`; MVP representa o limite como círculo geográfico definido pela loja. Se necessário, adicionar `delivery_polygon`/áreas de exceção por geometria PostGIS, sem listar manualmente toda rua do mapa.
- `category(id, tenant_id, name, sort_order, active)`; `product(id, tenant_id, category_id, name, description, price, cost, active, image_asset_id, version)`.
- `add_on(id, tenant_id, name, price, cost, active)` e junção `product_add_on(tenant_id, product_id, add_on_id)`.
- `asset(id, tenant_id, storage_key, detected_mime, size_bytes, checksum, status)`.
- `store_customer(id, tenant_id, name, email/phone conforme necessidade, created_at)`; índice/uniqueness deve respeitar política de identidade por loja.
- `customer_address(id, tenant_id, customer_id, fields...)`.
- `cart(id, tenant_id, customer_id ou session_ref, status, updated_at)`, `cart_item`, `cart_item_add_on`.
- `customer_order(id, tenant_id, public_number, customer_id nullable conforme política, status, fulfillment_type, address_snapshot, subtotal, delivery_fee, total, notes, cancellation_reason, created_at, updated_at, UNIQUE(tenant_id, public_number))`.
- `order_item(id, tenant_id, order_id, product_id nullable, product_name_snapshot, unit_price_snapshot, unit_cost_snapshot, quantity, notes)`; `order_item_add_on` mantém nome/preço/custo/quantidade snapshots.
- `order_event(id, tenant_id, order_id, from_status, to_status, actor_user_id, reason, created_at)` para trilha não ambígua.
- `payment_record(id, tenant_id, order_id, method, status, amount, cash_received, change_due, provider_reference nullable)`; métodos permitidos ao comprador: Pix, cartão ou dinheiro na entrega, conforme configuração da loja. Nunca armazenar PAN, CVV, trilha magnética, senha, PIN ou segredo de cartão. Para cartão, usar checkout hospedado/tokenização de provedor compatível e token não reversível/limitado, sem salvar método para compras futuras.
- `billing_invoice(id, organization_id, period_start, due_at, amount, status, paid_at, provider_reference)`; não há cobrança ou cálculo de multa, juros ou encargo por atraso. A migration V9 remove a tabela legada `late_charges` somente se estiver vazia e impõe a unicidade de loja por organização; V9 foi aplicada no banco local. Guardar apenas referência/estado do pagamento SaaS, nunca dados completos do cartão.
- `review` opcional, única por pedido se adotada.
- `audit_event(id, tenant_id nullable, actor_id, action, resource_type, resource_id, request_id, metadata_redacted, created_at)` append-only para ações relevantes.

### Relacionamentos JPA

Usar `@ManyToOne(fetch = LAZY)` do filho para pai; `@OneToMany(mappedBy=..., cascade=...)` apenas onde ciclo de vida exige. Coleções não devem ser serializadas diretamente. Evitar `CascadeType.ALL`/`REMOVE` em agregados que tenham histórico; remoções lógicas e FKs restritivas protegem pedidos. `@ManyToMany` puro não serve para produto/adicional quando a associação tiver regras/atributos; usar entidade de associação `ProductAddOn`. Definir `equals/hashCode` seguro para entidades e evitar `toString` que percorra relações. DTOs e mappers manuais explícitos (ou MapStruct com configuração estrita) controlam campos publicados.

## 6. Segurança obrigatória

Os controles mínimos transversais — incluindo armazenamento de senhas, segredos, PostgreSQL, sessão, validação, logs, dependências e backups — estão organizados em [`docs/security/BASELINE_SEGURANCA.md`](docs/security/BASELINE_SEGURANCA.md). A baseline é obrigatória antes de abrir endpoints privados ou usar dados reais; os controles avançados abaixo complementam essa base.

### Mass assignment e contrato de payload

- Obrigatório separar DTO de request e entidade; nenhuma entidade JPA ligada automaticamente ao JSON.
- Allow-list explícita por endpoint: construtores/fábricas/mappers copiam somente campos permitidos. IDs de tenant, owner, `role`, status interno, preço/custo calculado, trial, MFA, privilégio, auditoria e metadados são determinados no servidor.
- Rejeitar propriedades desconhecidas (`FAIL_ON_UNKNOWN_PROPERTIES`) para payloads de escrita, com resposta 400 e log seguro. DTOs pequenos, sem campos genéricos livres ou `Map<String,Object>` para dados privilegiados.
- Desabilitar binding automático de entidade e não usar `BeanUtils.copyProperties` indiscriminadamente. Testar payload com campos extras como `role`, `tenantId`, `enabled`, `mfaEnabled`, `trialEndsAt` e verificar rejeição/ausência de efeito.
- Response DTOs também têm allow-list: senha/hash, tokens, MFA secret, chaves, CPF/CNPJ sem necessidade, dados de outra loja nunca saem.

### Autenticação, sessão e MFA

- Login em qualquer área (console SaaS, painel da loja e conta de cliente) começa com e-mail ou CPF/CNPJ + senha e exige depois um código de segundo fator. Após validar a senha, a pessoa escolhe e-mail ou celular previamente verificado na própria conta para receber o código; a sessão só é criada após a validação do OTP.
- Desafio OTP de curta duração e uso único, limitado por tentativas e reenvios, vinculado à tentativa de login e armazenado como hash. Respostas genéricas evitam enumeração. Não aceitar destino arbitrário no pedido de envio. Se só houver um canal verificado, oferecer apenas esse canal.
- Recuperação de senha/conta, mudança do canal, troca/desativação do segundo fator e recuperação da conta têm fluxo próprio, reautenticação e auditoria; não podem ser autorizadas por campos enviados pelo navegador.

- Preferência inicial: access token curto em cookie `HttpOnly; Secure; SameSite` ou BFF/session cookie seguro; avaliar necessidade real de JWT. Se JWT: assinatura assimétrica/rotação de chaves, `iss`, `aud`, `exp`, `nbf`, `jti`, algoritmo allow-list, revogação/rotação de refresh token e armazenamento hashado. Nunca confiar em role/tenant claims antigas para autorização; revalidar estado/versão de sessão e membership no servidor.
- Refresh token em cookie seguro, rotativo e revogável; logout revoga. Proteção CSRF para auth baseada em cookie; CORS com origens exatas, sem wildcard com credenciais.
- Senha com Argon2id (ou BCrypt com custo adequado), política de senha, verificação de e-mail, recuperação com token curto, de uso único, armazenado em hash e resposta anti-enumeração.
- MFA obrigatório para console de suporte/admin da plataforma; exigir step-up MFA/reautenticação recente para trocar e-mail, senha, MFA, dono, CNPJ, membros/papéis, domínio, exportar dados e ações financeiras críticas. MFA nunca é campo alterável pelo cliente. Recuperação de MFA auditada e resistente a fraude.
- Papéis globais separados dos papéis da loja. Papéis iniciais: `OWNER`, `MANAGER`, `STAFF` e permissões finas; autorização por política no servidor, negação por padrão e escopo de recurso. A interface esconder botão não é controle de acesso.

### IDOR, multi-tenancy e regras de negócio

- Toda leitura/escrita privada verifica `tenant_id`, membership ativo, permissão e ownership. Responder 404 para recursos fora do escopo quando isso reduz enumeração; não revelar se outro tenant/id existe.
- Queries com escopo tenant obrigatório no próprio repository e FKs compostas. Operações em pedido validam dono/loja, estado atual e transição legal sob transação/lock apropriado.
- Recalcular no servidor total, preço, adicionais disponíveis, limites, taxa, troco, disponibilidade e trial. Não aceitar `total`, `status`, `ownerId` ou custo enviados pelo browser.
- Checkout idempotente; pedidos e pagamento consistentes em transação. Chave de idempotência vinculada a usuário/loja e request hash.
- Testes de autorização cruzada entre tenant A e B em todos os endpoints, inclusive nested IDs, exportação, pesquisa, arquivos e WebSocket/notificações futuras.

### Brute force e abuso

- Rate limiting estrito por IP e identificador normalizado para login, cadastro, recuperação, envio de OTP e endpoints caros; limites no gateway e aplicação, storage compartilhado (ex. Redis) em escala.
- Backoff e bloqueio temporário progressivo, sem permitir lockout permanente induzido por atacante; CAPTCHA adaptativo em login/recuperação e cadastro sob risco, provider validado no backend.
- Monitorar cadastros em massa, falhas distribuídas, abuso de trial/CNPJ, enumeração, domínios e uploads. Alertar com dados minimizados e retenção definida.

### Upload, URLs, SSRF e headers

- Allow-list de extensões e MIME detectado por conteúdo/magic bytes, limite de tamanho/dimensões, nome aleatório, checksum e reprocessamento seguro de imagem. Rejeitar HTML/SVG ativo, executáveis, polyglots e conteúdo com discrepância MIME/extensão.
- Storage privado por tenant; download autorizado por backend ou URL assinada curta e escopada. Upload nunca executável nem servido do classpath/webroot.
- Backend não busca URL arbitrária informada pelo usuário. Se integração buscar recurso remoto, allow-list de host/protocolo, bloqueio de IP privado/link-local/metadata, DNS rebinding-safe, redirecionamentos controlados, timeouts e egress restrito.
- Validar/sanitizar query, path, slug, hostname, redirecionamentos e filtros; escapar saída conforme contexto. HTTPS/TLS, CSP, HSTS, `X-Content-Type-Options`, política de referrer e limites de request.

### Localização e cobertura de entrega

- Na configuração, o lojista escolhe ponto central e raio de entrega; painel e vitrine exibem mapa com círculo e legenda clara de dentro/fora. Cliente pode permitir localização do navegador para comparar sua posição com a área. Solicitar geolocalização só após ação contextual, explicar a finalidade e permitir informar CEP/endereço manualmente quando negada ou indisponível.
- A checagem usa coordenadas do cliente ou endereço geocodificado, compara distância geodésica com o raio e recalcula no servidor durante o checkout. Localização do browser é aproximada e pode ser imprecisa; nunca garantir cobertura apenas por um círculo se houver barreiras reais. Para precisão por ruas/bairros, adicionar polígonos/áreas excluídas ou validação de endereço via provedor de mapas.
- Não é necessário nem apropriado “captar todas as ruas” nem rastrear continuamente o cliente: geocodificar sob demanda e reter apenas o necessário para a entrega. Compartilhar com o cliente e a loja o mesmo contorno publicado, com estados “entrega disponível” e “fora da área”, sem revelar a localização exata de outros usuários.
- Coordenadas são dados pessoais sensíveis pelo contexto de localização: minimizar precisão/retensão, restringir acesso, proteger em trânsito/repouso, obter consentimento/permissão quando aplicável e avaliar termos, custo, limites e privacidade do provedor cartográfico.

### Segredos, privacidade e operação

- Segredos só via secret manager/env injetado; nenhum segredo em Git/log/README. Rotação de chaves, criptografia de backups e acesso mínimo.
- Minimização e retenção de dados pessoais, consentimento/aviso, exportação/exclusão conforme política e obrigações da LGPD; trilha de acesso às operações sensíveis.
- Logs estruturados com correlation/request ID, sem senha, token, MFA secret, conteúdo de recuperação ou cartão; métricas e alertas de segurança.
- Backups PostgreSQL criptografados, PITR/restore testado, migrations reversíveis quando possível, rotina de incidente e plano de continuidade.

## 7. API REST (contrato inicial)

Prefixo `/api/v1`. JSON explícito, paginação limitada, filtros allow-listed, ordenação allow-listed, datas ISO-8601 UTC, dinheiro decimal como string ou número decimal JSON definido no contrato. Respostas de erro `application/problem+json`; versionar mudanças incompatíveis. A API pública da vitrine lê somente dados publicados da loja resolvida pelo host.

### Auth e onboarding

- `POST /auth/registrations` — cria organização, owner, loja vazia e trial em transação; payload permitido: tipo e número de documento (CPF ou CNPJ), nome empresarial quando aplicável, nome do responsável, e-mail, celular, senha, slug pretendido e aceite de termos (nunca role/trial/status).
- `POST /auth/sessions` recebe identificador (e-mail ou CPF/CNPJ) + senha e inicia desafio; `POST /auth/sessions/{challengeId}/send-code` recebe escolha entre canais já verificados; `POST /auth/sessions/{challengeId}/verify` valida OTP e só então cria sessão. `DELETE /auth/sessions/current`, `POST /auth/tokens/refresh` (se adotado refresh token).
- `POST /auth/password/forgot`, `POST /auth/password/reset`, `POST /auth/email/verify`.
- `POST /auth/mfa/enroll`, `/challenge`, `/verify`, `/disable` com reautenticação/step-up; segredos nunca retornam após provisionamento. `POST /me/security/delivery-channels` gerencia/verifica canais; mudanças exigem prova atual.
- `GET /me`, `GET /me/memberships`; `POST /organizations/{id}/invites`, `POST /invites/{token}/accept` com autorização e token de uso único.

### Loja pública e configurações

- `GET /public/store` e `GET /public/catalog/categories`, `GET /public/catalog/products` no host da loja.
- `GET /public/delivery-zone` retorna somente centro/raio/contorno publicado; `POST /public/delivery-check` recebe coordenada ou endereço e responde disponibilidade e motivo genérico, sem expor dados privados. Cliente envia coordenadas apenas após permissão/contexto.
- `GET/PATCH /stores/current/settings`; `PATCH` recebe só campos de configuração permitidos, valida owner/manager e step-up quando crítico.
- `GET /stores/current/members`; `POST /stores/current/invites`; `PATCH/DELETE /members/{id}`; papel só alterado por ator autorizado, jamais via DTO genérico.

### Catálogo e operações

- `GET/POST /stores/current/categories`; `GET/PATCH/DELETE /categories/{id}`.
- `GET/POST /stores/current/products`; `GET/PATCH/DELETE /products/{id}`; `POST /products/{id}/availability` como comando explícito.
- `GET/POST/PATCH/DELETE /add-ons` e associação produto-adicional em endpoints dedicados.
- `POST /assets/presign` e `POST /assets/{id}/complete` se storage assinado for adotado; autorização e validação antes de anexar mídia.
- `GET/POST /cart`, `POST /cart/items`, `PATCH/DELETE /cart/items/{id}`; carrinho anônimo pode migrar para usuário sem confiar em IDs do cliente.
- `POST /orders` cria checkout com chave idempotente e cálculo servidor; `GET /orders/{id}` só para cliente dono ou membro da loja com permissão; `GET /orders` paginado e tenant-scoped. Pagamentos do pedido: Pix, cartão ou dinheiro na entrega; dinheiro exige validação de troco. Cartão é digitado no checkout seguro do provedor e não fica salvo no Peditto nem na loja.
- `POST /orders/{id}/accept`, `/start-preparation`, `/dispatch`, `/complete`, `/cancel` com transições explícitas, motivo e permissão. Não usar `PATCH status` arbitrário.
- `GET /orders/{id}/receipt`; geração de comanda autorizada, protegida e sem dados extras.
- `GET /finance/reports/revenue?from&to&groupBy`; validar intervalo e definir receita reconhecida, cancelamentos/reembolsos e timezone.

### Assinatura e suporte

### Acesso em atraso, exportação e cancelamento

- Após 15 dias corridos de tolerância sem pagamento, o backend suspende a aceitação de novos pedidos e as operações de escrita da loja. O painel permanece disponível em leitura; criar/editar, importar e gerenciar membros fica bloqueado. Cobrança, segurança da própria conta, logout e exportação autorizada permanecem acessíveis. A loja pública informa apenas que está temporariamente fechada.
- A exportação durante a suspensão cobre produtos, pedidos e vendas/financeiro sem nome, telefone, endereço, coordenadas, identificador ou observações livres de clientes. A autorização é verificada no servidor, com escopo de tenant, permissão específica, link temporário e registro de auditoria.
- Pedidos já aceitos antes da suspensão recebem transições explícitas para concluir ou cancelar; a suspensão não deve deixar pedidos ativos sem tratamento.
- Após pagamento confirmado e reconciliado, a loja é reativada automaticamente. Nenhum valor de multa, juros ou encargo é acrescentado à fatura por atraso.
- Cancelar a assinatura interrompe renovações futuras, sem multa de cancelamento. Se o período atual estiver pago, o acesso segue até seu término. Depois, a conta fica inativa por até 90 dias, com exportação disponível; ao fim do prazo, eliminar ou anonimizar dados operacionais. Registros mínimos sujeitos a retenção legal/contábil seguem política separada, com acesso restrito.
- A retenção de 90 dias também começa após a suspensão por inadimplência. Notificar o responsável sobre o prazo e a data prevista para eliminação. Cancelar não cria encargos por atraso.

- `GET /billing/trial`, `GET /billing/subscription`, `GET /billing/invoices`; `POST /billing/invoices/{id}/pay` inicia pagamento manual Pix/cartão no provedor sem débito automático e sem persistir cartão. O usuário paga novamente a cada ciclo; salvar apenas referência, estado e recibo necessário.
- Webhooks de billing com assinatura validada, replay defense e idempotência. Nenhum endpoint público atualiza trial por payload. O trial dura 30 dias. Ao terminar, deve existir plano/preço apresentado e aceito e uma fatura válida; sem isso, o estado é `TRIAL_EXPIRED`, não inadimplência. Após vencimento de fatura, há 15 dias corridos de tolerância; não se cobra multa, juros ou qualquer encargo por atraso. Notificar por e-mail e celular seis dias antes do vencimento e no vencimento. Ao fim da tolerância, suspender a operação pública e aplicar as permissões de leitura/exportação definidas neste README. O pagamento confirmado reativa a loja automaticamente.
- APIs de suporte em domínio separado, MFA, RBAC de plataforma, motivo e audit log; impersonação, se um dia necessária, é temporária, visível, aprovada e auditada.

## 8. Experiência visual e marca

### Direção aprovada

**Cozy Late Night + Y2K/Windows 98 autêntico + delivery**: calor de mesa de estudos à noite e prazer tátil de interfaces antigas, sem transformar a vitrine de comida numa página corporativa. A imagem de referência anexada é um app de delivery Taco Bell em celular: moldura acolhedora clara, cabeçalho de marca, busca, banner alimentar, categorias, cards de produtos com preço/avaliação/ação de adicionar, promoção e navegação inferior. Usar isso como referência de hierarquia e conveniência, sem copiar marca, layout ou conteúdo.

### Regras de arte

- Proibidos fundo preto/terminal, glassmorphism exagerado, cards genéricos translúcidos, cantos enormes, neon ciano/roxo/verde, gradientes de enfeite e layout clichê de startup/cripto.
- Paleta orgânica quente com roxo ameixa como acento e scrollbar roxa coerente. O texto fornecido não definiu códigos finais: fechar tokens a partir da imagem anexada e protótipo antes de congelar. Ponto de partida a validar: creme/papel `#F7EEE2`, marrom tinta `#38251F`, ameixa `#54245F`, lavanda empoeirada `#B79AB9`, pêssego/terracota `#D98654`, borda areia `#D8C7B2`. Usar contraste WCAG, estados de foco e contraste também em controles.
- Três papéis tipográficos: (1) display retrô condensada para títulos/placas; (2) sans humanista e legível para corpo/formulários; (3) mono pixel/bitmap apenas para microdetalhes/labels estilo sistema antigo. Não usar Inter, Roboto ou `system-ui` como identidade. Escolher fontes licenciadas e carregar localmente quando possível; fonte retrô nunca deve prejudicar leitura.
- Física tátil via bordas firmes, sombras curtas direcionais, relevo de botão controlado, padrões/grão discretos de papel e componentes inspirados em janelas/abas físicas. Textura sem gradiente e sem ruído excessivo.
- Motion design forte na página de apresentação: animações com propósito (transição entre janela/loja, entrada de produtos, feedback de adicionar ao carrinho), performance e `prefers-reduced-motion`. Fluxos operacionais priorizam rapidez e não podem atrasar interação.
- Copy em português direto e concreto, sem chuva de emoji nem jargão de marketing. Ícones de pasta/disquete só quando fizerem parte da metáfora visual.
- Mobile-first. Referência delivery guia a vitrine; painel do comerciante deve otimizar fila, catálogo e financeiro em desktop/tablet e continuar responsivo.

### Temas por loja e galeria de demonstração

Cada loja escolhe um preset visual fechado, isolado por tenant: `PEDITTO_ORIGINAL` (roxo, amarelo e creme; Gelatto Totosu), `FAST_FOOD_RED` (vermelho, amarelo e creme; Restaurante Dona Erza) e `CAFE_NATURAL_GREEN` (verde e creme; Muru Muru Coffee). As referências visuais orientam paleta e clima; marcas, mascotes e layouts não serão copiados.

A galeria pré-cadastro na seção `/#temas` apresenta as três lojas fictícias usando a mesma vitrine React e fixtures diferentes, sem páginas clonadas. Cada card abre `/loja/<slug>` e as rotas de demonstração são marcadas como `noindex`. `/loja/demo` permanece como alias legado para Dona Erza. No painel autenticado, o lojista pré-visualiza e confirma o tema da própria loja; a vitrine pública recebe do servidor somente a chave do preset resolvido para o host/slug daquela loja. Fixtures e previews ficam em `frontend/src/data/demo-stores/`, skins em `frontend/src/styles/storefront/themes/`, imagens usadas em `frontend/src/assets/demo-stores/` e originais/referências fora do bundle em `docs/ux/demo-stores/`.

O backend valida uma allow-list de chaves e persiste `store_settings.theme_key` (default `PEDITTO_ORIGINAL`); o frontend associa cada chave a tokens locais imutáveis. Não aceitar CSS/HTML/cores arbitrárias nem confiar em query string ou estado do navegador. Escrita exige autorização por objeto, tenant, capability e CSRF; rotas continuam fechadas até a matriz de autorização ser aprovada. Ver [`docs/decisions/0009-vitrine-identidade-e-imagens.md`](docs/decisions/0009-vitrine-identidade-e-imagens.md) e a [Fase 7](docs/README_ROTEIRO_MVC_E_SAAS.md#fase-7--vitrine-temas-por-loja-cliente-e-carrinho).

### Tokens e componentes React

Guardar tokens em CSS variables com nomes semânticos (papel, tinta, ameixa, lavanda, terracota e linhas; fontes display/body/mono; sombras e raios discretos). Componentes acessíveis e reutilizáveis devem surgir conforme os fluxos reais forem implementados. Não espalhar cores hexadecimais nem criar design system genérico sem identidade.

## 9. Organização proposta do repositório

```text
Peditto/
  README.md
  docs/
    decisions/                 # ADRs de tenancy, auth, domínio, billing, design
    domain/                    # glossário, regras, estados e fluxos
    security/                  # threat model, matriz de autorização, checklist
    api/                       # OpenAPI e convenções
    ux/                        # referências, tokens e fluxos
  backend/
    src/main/java/.../
      common/{config,errors,security,tenancy,audit}
      identity/{api,application,domain,infrastructure}
      organizations/{api,application,domain,infrastructure}
      stores/{...} catalog/{...} cart/{...} orders/{...}
      payments/{...} finance/{...} subscriptions/{...}
    src/main/resources/db/migration/
    src/test/java/...
  frontend/
    src/app/ src/features/ src/shared/ src/styles/
  infra/
    compose.yaml               # somente serviços locais
    deployment/                # configuração sem segredos
```

Manter módulos funcionais completos, nomes explícitos e arquivos de tamanho razoável. Evitar controllers gigantes, DTO genérico, magic strings, dependências circulares e regra de negócio em componente React.

## 10. Qualidade, testes e entrega contínua

- Testes unitários de regras (CNPJ, trial, preço, transição de pedido, papéis) e testes de integração com PostgreSQL real/Testcontainers.
- Testes de contrato de DTO para campos extras/unknown e serialização sem segredos.
- Matriz automatizada de RBAC/ABAC e teste de isolamento tenant A↔B cobrindo cada recurso, nested route, busca, relatórios e mídia.
- Testes de concorrência para CNPJ único, criação de trial, checkout/idempotência, numeração de pedido e transições simultâneas.
- Frontend: validação, navegação teclado, leitor de tela, mobile, contraste, `prefers-reduced-motion`, estados vazios e erro da API.
- Pipeline: format/lint, compilação, testes, migrações em banco temporário, scan de dependências/segredos, build de containers e publicação versionada. Não executar deploy automático sem ambiente e política aprovados.
- Logs/métricas com request ID, tenant ID pseudonimizado e sem PII desnecessária. Alertas para erros 5xx, falhas login, abuse, falhas de webhook e atraso de notificações.

## 11. Fases recomendadas

A ordem executável está detalhada em [`docs/README_ROTEIRO_MVC_E_SAAS.md`](docs/README_ROTEIRO_MVC_E_SAAS.md). V9–V16 e RLS A↔B foram validadas localmente. As Fases 4, 5 e 6 estão concluídas no backend; rotas seguem fechadas e upload desligado. Na Fase 7, o esqueleto React, as três identidades visuais de demonstração e a galeria pré-cadastro estão implementados. Integração com a API, persistência isolada do preset, conta do cliente e carrinho seguro continuam pendentes. SEO avançado continua na Fase 13.

A migração de dados do Chokko Melt continua opcional: não copiar o banco de TCC diretamente. Se os dados forem necessários, criar importador com mapeamento, validação, relatório de rejeições e tenant escolhido; nunca importar credenciais de exemplo.

## 12. Perguntas de produto a fechar antes da implementação

1. A regra do MVP é uma organização por CPF/CNPJ, uma loja por organização e um trial por documento. Definir se CNPJ será único pelo número completo ou pela raiz para tratar filiais.
2. O trial começa após verificação dos contatos e provisionamento da loja. A fatura inicial precisa decorrer de plano/preço apresentado e aceito; definir o instante exato de vencimento e a experiência se o usuário não converter.
3. Definir o calendário exato da suspensão após os 15 dias corridos, inclusive pagamento confirmado perto do limite.
4. Como comprovar titularidade do CPF/CNPJ e lidar com cadastros de pessoa física? Validação matemática não comprova titularidade.
5. Cliente final terá conta reutilizável entre lojas ou cadastro separado por loja? Recomendação de privacidade: isolamento por loja, com conta externa opcional e consentida.
6. Qual provedor processará Pix/cartão dos compradores e as faturas SaaS, sem armazenar cartão? O gateway deve ser hospedado/tokenizado e não habilitar cobrança recorrente automática.
7. Slug definitivo será loja.peditto.com, haverá subdomínio de painel separado ou painel dentro da loja? Quais ambientes/domínios serão usados?
8. O raio será sempre circular ou haverá polígonos/ruas excluídas e cálculo por tempo de rota? Qual provedor de mapas/geocodificação será adotado? Horário e área de entrega serão configuráveis por loja?
9. O nome do produto é Peditto e a imagem Taco Bell é apenas referência de hierarquia visual? Confirmar antes de fechar identidade e domínio.
10. Exportação: fechar papéis autorizados, campos exatos, reautenticação e formatos antes da implementação.

## 13. Princípios que não podem regredir

- Uma empresa/CNPJ não ganha trial duplicado pelo fluxo público.
- Loja nova começa vazia e só aparece publicamente quando configurada/publicada.
- Todo dado operacional tem `tenant_id`; nenhum ID sozinho prova autorização.
- Nenhuma entidade JPA é input/output HTTP.
- Status, papel, tenant, MFA, totais, custo, owner e trial são controlados no servidor.
- Pedidos guardam snapshots e transições; histórico financeiro não muda quando catálogo muda.
- Segurança e isolamento são verificados em testes automatizados; UI não é barreira de segurança.
- O design mantém caráter próprio, calor, textura e hierarquia de delivery, com contraste, acessibilidade e boa performance.
