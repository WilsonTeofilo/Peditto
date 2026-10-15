# Revisão técnica e pendências do Peditto

**Revisado em:** 15/10/2026
**Escopo:** estado real do repositório após avanço das Fases 3 e 4. Este documento acompanha [`README_ROTEIRO_MVC_E_SAAS.md`](README_ROTEIRO_MVC_E_SAAS.md); manter ambos alinhados.

## Verificação local atual — 15/10/2026

- PostgreSQL 17.11 iniciou localmente. Flyway confirma V9–V13 aplicadas; Hibernate `ddl-auto=validate` e startup completo do Spring passaram.
- `peditto_app` segue com `rolsuper=false` e `rolbypassrls=false`. RLS e `FORCE ROW LEVEL SECURITY` cobrem 25 tabelas tenant-owned. V11 inclui contexto tenant para `stores`, configurações, zona de entrega, horários, domínios, trial, faturamento, pagamentos, ledger e idempotência.
- `TenantTransactionIntegrationTest` executou quatro provas com `peditto_app`: conexão Hikari reutilizada sem vazamento de GUC; onboarding ativando tenant dentro da transação; capabilities baseadas em membership; A↔B para dados operacionais, lookup público somente por hostname verificado/ativo e rollback integral dos fixtures sintéticos. Nenhum dado de loja foi mantido.
- Smoke checks após correção: `/actuator/health` → 200/UP; `/api/v1/auth/csrf` → 200 e cookie `__Host-PEDITTO_CSRF` Secure/SameSite; rota privada sem handler → 403; a resposta não cria `JSESSIONID`.
- Corrigidos três defeitos reproduzidos: endpoint CSRF estava oculto quando OTP estava desabilitado; health tentava conectar ao SMTP vazio; Spring podia iniciar sessão HTTP paralela à sessão opaca da aplicação. CSRF agora independe do provider; health local não consulta mail; segurança HTTP usa `STATELESS`.
- V10 falhou inicialmente por variável PL/pgSQL ambígua; a migration não aplicada foi corrigida, repetida e confirmada como sucesso. Não editar V10 após sua aplicação local; mudanças de cobertura devem entrar em V11.
- `infra/dev.ps1` agora permite os nomes de configuração SMTP/Twilio do `.env.example`, aceita campos opcionais vazios quando OTP está desligado, valida formato e completude quando habilitado e mantém valores fora da saída.
- Limitações ainda abertas: SMTP/Twilio não foram usados para envio real; troca de fator com step-up não existe; testes ainda não cobrem concorrência nem fluxo integrado de autenticação. Controllers da Fase 5 já têm guards por capability, mas as rotas continuam inacessíveis pelo `denyAll` global conforme decisão do usuário.

## Atualização das Fases 4, 5 e 6 — 15/10/2026

- Fase 4 fechada como fundação de isolamento/autorização: contexto de tenant aplicado na transação, capabilities com allow-list, membership ativa, RLS forçada em 25 tabelas e função de domínio verificado. Isso não fecha os gates restantes da Fase 3 (MFA integrado, step-up, providers e concorrência).
- Fase 5 concluída no backend: configuração allow-list, checklist, agenda semanal, zona circular, publicação, consulta pública por slug, status aberto/fechado por horário local e comandos auditados de fechar/reabrir. Limites de operação ficam em `StorePolicy`; SQL versionado mantém restrições do schema.
- V13 adiciona lookup público `SECURITY DEFINER` com `search_path` fixo, owner `peditto_migrator`, EXECUTE concedido apenas a `peditto_app`; informa status e horário local e remove a função V12 substituída. Foi aplicada pelo Flyway e validada no startup Spring em loopback/porta efêmera.
- As rotas privadas usam guards `settings:read`/`settings:write`; os serviços repetem capability/tenant no backend. A autorização do usuário permanece: nenhuma rota de setup, vitrine ou catálogo foi retirada do `denyAll`.
- Fase 6 concluída no escopo backend: DTOs allow-list, serviços scoped, disponibilidade operacional, preço imutável por histórico append-only, preservação de produtos referenciados por pedidos, storage privado preparado com upload desligado e projeção pública de catálogo separada. V14–V16 foram aplicadas; Flyway e Hibernate validate passaram e o app iniciou em porta aleatória.
- `mvn -q -DskipTests compile` passou. Não foram executados testes automatizados nesta atualização.
- Fase 6: rotas permanecem fechadas pelo `denyAll`; upload segue desligado e não há controller de upload/leitura. O adapter atual é filesystem privado para o primeiro deployment; para múltiplas instâncias, configurar object storage privado antes de ligar uploads. Geolocalização/fallback CEP pertence à Fase 7; revalidação de checkout e snapshot mínimo pertencem à Fase 8.

## Revisão de legibilidade do backend — 14/10/2026

- Modelos JPA que estavam comprimidos foram reformatados para leitura por responsabilidade; campos e invariantes existentes foram preservados.
- O SQL atômico duplicado do rate limit foi movido a `RateLimitBucketStore`, compartilhado pelo filtro de IP e pelo limitador de identidade. A política e os fingerprints continuam nos componentes responsáveis por cada fluxo.
- A entidade JPA `ApiRateLimitBucket` não tinha consumidores (o acesso ao bucket é SQL direto) e foi removida; associações inversas sem uso em `AppUser`/`Organization` também foram retiradas. Flyway continua sendo a fonte do schema.
- Limites geográficos em `DeliveryZone` agora têm nomes explícitos e validação separada. A captura de erro do filtro de rate limit foi estreitada para falhas esperadas de banco/criptografia.
- `pom.xml` e trechos longos de filtros/repositories foram formatados. A varredura estática não encontrou os padrões listados no roteiro (Lombok `@Data`, injeção em campo, `BeanUtils`, stdout, TODO/FIXME abandonados).
- A validação anterior de backend iniciou a aplicação em porta temporária, com V1–V8 e Hibernate `validate`; essa checagem precede o frontend e V9/V10.
- Criado `frontend/` com React + TypeScript + Vite, telas responsivas de entrada/cadastro, MFA por e-mail/SMS, integração CSRF same-origin e Vite proxy para `/api`. O estilo usa papel creme, ameixa e terracota, janela retrô tátil e fontes locais do sistema, sem gradientes ou neon.
- Página pública inicial inclui pitch baseado na dor levantada pelo TCC, avatar/logo e favicon derivados da mascote oficial; a leitura visual e o texto foram registrados em `docs/brand/PITCH_E_IDENTIDADE_VISUAL.md`. O roteiro ganhou a Fase 13 com SEO técnico, conteúdo, indexação e medição.
- `npm install` criou o lockfile; npm reportou zero vulnerabilidades. `npm run build` passou e a página foi conferida no navegador local. O fluxo ainda não foi integrado a API ativa, pois o PostgreSQL continua indisponível e a entrega OTP requer configuração externa.

**Limite desta revisão:** os modelos de pedido/catálogo/cliente ainda são estruturas de persistência iniciais, sem comportamento de aplicação ou endpoints. Não foram criados `Money`, strategies, interfaces de provider ou value objects especulativos; introduzi-los junto aos casos de uso reais evita abstração prematura.

## Registro histórico — avanço das Fases 3 e 4 em 14/10/2026

- A regra de uma loja por organização agora tem migration V9 com `UNIQUE(organization_id)`, preservando a chave composta usada pelas FKs. A migration aborta se encontrar organizações com lojas duplicadas.
- A tabela antiga de encargos só é removida pela V9 se estiver vazia; o modelo JPA `LateCharge` foi retirado. A regra de produto não cobra multa, juros ou encargo por atraso.
- Login relê o estado `enabled` do usuário sob lock antes do envio de OTP e da criação da sessão, evitando confiar no estado carregado pela pré-autenticação.
- `mvn -q clean compile -DskipTests` passou após essas mudanças. Não foram executados testes automatizados nem aplicada a migration V9 ao PostgreSQL local nesta revisão.
- Implementados worker/outbox com retry limitado, transporte SMTP/Twilio, validação de configuração fail-fast quando habilitado e controllers DTO-only de registro, login MFA, recuperação, sessão e CSRF. OTP não é devolvido em resposta. Os controllers são condicionais a `OTP_DELIVERY_ENABLED=true`; rotas não implementadas continuam deny-all.
- Recuperação exige códigos válidos nos dois canais verificados, troca a senha e revoga sessões. Troca de contato e de fator MFA não foi exposta; ainda exige fluxo de step-up, notificação e auditoria.
- Fase 4 foi fechada como fundação: capability allow-list, membership ativa, ownership por tenant, RLS nas 25 tabelas e lookup público de domínio verificado. Cadastro e verificação agora ajustam o GUC na transação antes de gravar tabelas protegidas; V11 foi aplicada e validada com rollback fixtures.
- Registro histórico: Fase 5 iniciou com `DeliveryCoverage` (distância de grande círculo, raio e coordenadas validados) e repositories tenant-scoped para configurações, horários, zona e domínio. Atualização de 15/10 detalha a implementação dos casos de uso/controllers mantendo o default-deny.
- `mvn -q test` passou: 10 testes, 0 falhas/erros, incluindo testes integrados contra PostgreSQL local. Não foi feito envio real por SMTP/Twilio nem teste de concorrência/autenticação completa.

## Registro histórico — início da Fase 4 em 14/10/2026

- Criado `TenantContext` request-local, que só fornece organização por meio de `requireOrganizationId()` e falha fechado se não houver contexto autorizado.
- Criado `TenantContextFilter`: `X-Organization-Id` é somente um seletor; o backend valida UUID e membership `ACTIVE` para o principal UUID da sessão opaca. Associação ausente/inválida recebe 404 genérico. O contexto é limpo em `finally` em todos os caminhos.
- Filtro integrado depois da autenticação na cadeia Spring Security e desabilitado como filtro servlet automático, evitando execução antes da sessão. `anyRequest().denyAll()` permanece intacto.
- Compilação Maven após a implementação passou com `-DskipTests`. Não foram executados testes; o contexto ainda não é usado por controllers e não deve ser considerado isolamento completo.
- Ainda faltam endpoints de negócio e autorização efetivamente aplicados em cada rota, resolução de domínio público allow-listed, execução das migrations e validação A↔B na mesma conexão Hikari. Não propagar o ThreadLocal para tarefas assíncronas; tarefas futuras devem definir contexto explicitamente ou usar operações administrativas isoladas.
- `TenantAuthorization` oferece allow-list fechada e matriz por OWNER/MANAGER/STAFF; não há wildcard para `platform:*`. Produtos/pedidos são consultados também por tenant. Nenhum endpoint de negócio está liberado.
## Estado executivo

Fases 0, 1 e 2 estão documentadas como concluídas. V1–V16 estão aplicadas no banco local; Flyway validou as migrations e Hibernate `validate` passou. A Fase 4 está concluída como fundação, sem liberar rotas de negócio. A Fase 3 permanece aberta pelos gates de autenticação listados abaixo; Fases 5 e 6 estão concluídas no backend, com todas as rotas de negócio fechadas e upload desabilitado.

**A API de autenticação está implementada, mas ainda não validada como fluxo integrado.** Rotas existem sob `/api/v1/auth`; controllers de identidade ficam condicionados à habilitação de entrega OTP e CSRF sempre responde. `mvn test` passou com três testes unitários para rate limit/anti-enumeração do cadastro; faltam testes integrados, credenciais de teste fora do projeto, validação com providers e browser, e troca segura de fatores. Endpoints de negócio seguem `denyAll` até autorização por recurso ser conectada.

Além dos três unit tests do cadastro, a suíte inclui cobertura geográfica e testes integrados de GUC/Hikari, isolamento A↔B, domínio verificado e rollback (estado registrado no ciclo anterior). Nesta atualização apenas a compilação foi executada; não há testes automatizados de concorrência ou fluxo completo de MFA/provedores.

## O que está implementado

### Identidade e abuso

- DTO de registro com allow-list e rejeição global de propriedades JSON desconhecidas; campos privilegiados não são copiados de payload para entidade.
- Normalização de e-mail, telefone E.164 e nome; CPF/CNPJ canonizado e validado por dígitos verificadores.
- Senha codificada com `DelegatingPasswordEncoder` e PBKDF2 versionado; respostas genéricas para conflitos de cadastro e início de login.
- Cadastro transacional cria organização, usuário, documento, membership OWNER, loja vazia/configuração e desafios. Trial e ativação só acontecem depois da confirmação dos dois contatos.
- OTP numérico de 6 dígitos gerado por CSPRNG; HMAC com segredo separado do banco, vinculado a usuário/finalidade/canal/expiração; comparação timing-safe, expiração, tentativas limitadas e consumo sob lock.
- Outbox persiste payload cifrado em AES-GCM com nonce e versão da chave; worker agenda entrega SMTP/SMS via Twilio, com lease, retry limitado, backoff, expiração e redaction. Não executado contra credenciais/provider nesta sessão.
- Buckets de abuso persistidos no PostgreSQL usam fingerprints HMAC: cadastro por documento/e-mail/telefone, OTP por destino/finalidade e login. Limite por IP continua usando fingerprint e endereço do socket, sem confiar em `X-Forwarded-For`.
- Login aceita e-mail ou CPF/CNPJ + senha; resposta mantém forma/canais genéricos, criação de pré-autenticação de curta duração e MFA escolhido entre canais declarados. A sessão só é criada após OTP válido.

### Sessões e segurança HTTP

- Token aleatório opaco de sessão; somente SHA-256 do token fica no banco. Sessão de 12 horas validada no servidor por filtro e estado ativo do usuário.
- Cookie de sessão `__Host-`, `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/`; CSRF permanece ligado e CORS desligado. Endpoint emite token para o React; validação em browser/origin real segue pendente.
- Logout da sessão atual e global revogam sessões e registram auditoria.
- Rotação substitui o token mantendo família; reutilização de token revogado tenta revogar as sessões ativas remanescentes da família e gera evento de auditoria. Sem endpoint público de rotação.
- `anyRequest().denyAll()` segue ativo. Não há permissões concedidas por claims do cliente.
- Segredos de HMAC e AES-GCM são variáveis obrigatórias no ambiente; script de desenvolvimento valida allow-list e formato sem imprimir valores. `.env` local está ignorado e não deve ser copiado para commits ou respostas.

## Pendências para fechar a Fase 3

1. Configurar SMTP e Twilio no secret manager do ambiente, validar SPF/DKIM/DMARC, sender, limite/custo e entrega sem revelar OTP em logs; conferir monitoramento da outbox.
2. Fazer validação integrada dos contratos DTO-only e CSRF em navegador/origem de frontend antes de habilitar `OTP_DELIVERY_ENABLED` em ambiente compartilhado.
3. Definir como a resposta genérica do login entrega/oculta erro sem permitir enumeração; somente os canais verificados devem aparecer, com destino mascarado e escolha submetida por allow-list. Hoje a lista de canais é fixa e requer revisão antes do controller.
4. Ligar corretamente cookie e CSRF: token CSRF legível em endpoint dedicado, `CookieCsrfTokenRepository`/handler adequado ao React e validação de Origin/Referer como defesa adicional. CORS exato somente se frontend ficar em origem diferente.
5. [x] Login relê e valida o estado habilitado sob lock antes de solicitar OTP ou criar sessão.
6. Recuperação foi implementada exigindo OTP dos dois canais e revogando sessões. Validar limites/fluxo integrado e definir mensagens seguras de notificação; recuperação não troca contato nem contorna MFA.
7. Troca de e-mail/telefone exige autenticação recente/step-up, verificação do novo destino, notificação no destino anterior quando possível, cooldown, auditoria e política de recuperação.
8. Listar sessões próprias com metadados mínimos e revogar sessão específica; não expor tokens/IPs/user-agent bruto. Logout global já existe como serviço interno.
9. Fechar matriz de rate limit (IP + conta/destino), CAPTCHA adaptativo após sinais de abuso, métricas sem PII e proteção contra lockout induzido por terceiros.
10. Cobrir por testes unitários/integração: corrida por CPF/CNPJ, trial único, OTP replay/expirado/limite, login com MFA, desativação concorrente, rotação e reuse, CSRF, unknown fields, enumeração e revogação. Testes não foram executados nesta revisão.
11. Verificar que CNPJ só autentica o titular OWNER da organização e que não será usado como identificador para funcionários convidados. Se usuário puder pertencer a várias lojas, resolver contexto por membership depois de autenticar.

## Gates antes de liberar endpoints e dados privados

- Não remover o default-deny; só liberar endpoint após DTO, autenticação, CSRF, autorização e teste negativo.
- Exercitar contexto A↔B, commit/rollback e conexão reutilizada do pool; RLS é defesa em profundidade, não substitui autorização.
- Implementar endpoints de negócio com TenantContext, ownership por objeto, queries tenant-scoped e matriz RBAC/ABAC já iniciadas.
- Aplicar FKs compostas para impedir referências cruzadas e testes de IDOR em leitura, escrita, busca, exportação e mídia.
- Autenticação de plataforma/CEO separada de membership de loja, MFA obrigatório, step-up e auditoria antes de criar console administrativo.

## Gatilhos de produção

- TLS/HSTS na borda, proxy confiável configurado explicitamente, secrets manager e rotação dos segredos locais/de produção.
- Roles PostgreSQL mínimas separadas para aplicação e migração; backup criptografado com restore testado, alertas e resposta a incidente.
- Provedores de pagamento hospedados e webhooks autenticados/idempotentes; nunca receber ou persistir PAN/CVV. Rever termos de cobrança sem multa/juros, suspensão, cancelamento e retenção com jurídico/contábil.
- Provider de mapa escolhido sob termos/licenças adequados; validação server-side do raio e coleta de localização apenas por ação explícita do cliente.
- LGPD: finalidade, minimização, retenção, exclusão/exportação, contratos de operador e processo de incidente.

## Próximos passos ordenados

1. Automatizar os gates da Fase 3: cadastro concorrente/trial único, OTP expirado/replay, login MFA, recuperação, rotação/reuso de sessão e rejeição de campos desconhecidos.
2. Configurar SMTP/Twilio em ambiente de teste protegido e validar entrega, retries, expiração e logs redigidos. Não gravar segredos no projeto.
3. Concluir testes integrados de concorrência e autenticação da Fase 3 antes de habilitar os canais reais fora de dev.
4. Fechar troca de contato/MFA com reautenticação, step-up, verificação do novo destino, notificação no endereço anterior, cooldown e auditoria.
5. Implementar o primeiro endpoint vertical de catálogo com DTO allow-list, capability server-side, tenant repository, RLS e casos negativos cross-tenant; só então liberar a rota.
6. Implementar configuração da loja com DTOs explícitos/capabilities; expor domínio público apenas por host normalizado e proxy confiável definido.

## Referências de implementação

- Roteiro completo e critérios de aceite: [`README_ROTEIRO_MVC_E_SAAS.md`](README_ROTEIRO_MVC_E_SAAS.md).
- Decisão de identidade/sessão: [`decisions/0002-identidade-e-sessao.md`](decisions/0002-identidade-e-sessao.md).
- Provedores de OTP escolhidos: [`decisions/0007-adiamento-de-provedores.md`](decisions/0007-adiamento-de-provedores.md).
- RLS e contexto de tenant: [`decisions/0008-rls-tenant-context.md`](decisions/0008-rls-tenant-context.md).
- Threat model: [`security/THREAT_MODEL_MVP.md`](security/THREAT_MODEL_MVP.md).
