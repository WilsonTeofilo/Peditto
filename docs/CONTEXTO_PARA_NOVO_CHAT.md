# Contexto para continuar o Peditto em um novo chat

**Atualizado:** 15/10/2026
**Repositório:** `D:\Projetos\Peditto`
**Branch local:** `Develop`
**Último commit publicado antes das mudanças locais:** `76ffcfe` — `fix: keep natural theme logo rounded at all sizes`

Este arquivo é um resumo de retomada, não substitui os critérios completos. Comece por aqui e depois consulte o roteiro e o relatório técnico. A árvore de trabalho contém alterações locais ainda não publicadas; preserve-as e revise `git status --short` e o diff antes de limpar, resetar, comitar ou publicar.

## Produto e arquitetura

Peditto é um SaaS multi-tenant de cardápio, pedidos e operação de delivery para pequenos restaurantes, confeitarias e lojas. Uma loja nova começa vazia. A arquitetura é um monólito modular Spring Boot/PostgreSQL e um frontend React + TypeScript + Vite. O frontend atual usa CSS modular e tokens em CSS variables; Tailwind constava de uma proposta inicial, mas não foi adotado.

No produto, todas as lojas usarão o mesmo esqueleto React da vitrine; identidade, textos, catálogo e preset visual vêm dos dados resolvidos para aquela loja. Não criar uma página/componente clonado por loja e não transformar cada preset em apenas uma troca de cor. Separar composição reutilizável, fixture/conteúdo e skin visual.

## Vitrines demonstrativas já feitas

| Preset | Loja fictícia | Identidade |
|---|---|---|
| `PEDITTO_ORIGINAL` (`original`) | Gelatto Totosu | Ameixa/roxo, amarelo quente e creme; sorveteria relaxante na Faria Lima, fundada em 1999, dona Asai Ayumi; produtos ilustrativos na faixa de R$ 12–40. |
| `FAST_FOOD_RED` (`fast-food`) | Restaurante Dona Erza | Vermelho, amarelo e creme; direção de delivery/confeitaria e lanchonete. |
| `CAFE_NATURAL_GREEN` (`natural`) | Muru Muru Coffee | Verde floresta, sálvia, marfim e marrom café; cafeteria artesanal/familiar. Fundada em 1988 por Alberto José, no bairro Pilar, Ouro Preto–MG; Alberto Júnior gerencia e Wilson Teófilo cuida da cozinha e aprimora receitas antigas. |

A galeria fica em `/#temas`. Rotas: `/loja/gelatto-totosu`, `/loja/restaurante-dona-erza`, `/loja/muru-muru-coffee`. `/loja/demo` é alias legado para Dona Erza. Todas são fixtures fictícias pré-cadastro, sem API, persistência, conta ou pedidos reais; ficam `noindex`.

O diretório de trabalho visual foi organizado assim:

- `frontend/src/components/storefront/`: esqueleto e interações compartilhadas.
- `frontend/src/data/demo-stores/`: conteúdo/fixtures, slugs e metadados da galeria (`themePreviews.ts`).
- `frontend/src/assets/demo-stores/`: somente imagens importadas pelo frontend em runtime.
- `frontend/src/styles/storefront/themes/`: skins `original/`, `fast-food/` e `natural/`.
- `docs/ux/demo-stores/`: logos-fonte, materiais de referência e originais que não devem entrar no bundle.
- `frontend/README.md` e `docs/ux/README.md`: mapas curtos das pastas e do uso dos assets.

Build do frontend passou após a reorganização anterior. Não foram executados testes automatizados nesta atualização documental.

## Estado das fases

- Fases 0–2: concluídas conforme roteiro.
- Fase 3: **aberta**. Fechar validações integradas OTP/CSRF no browser, SMTP/Twilio em ambiente de teste seguro, concorrência, sessão/replay e step-up para troca de contato/fator MFA. Não copiar segredos para repositório ou logs.
- Fase 4: fundação de tenant/capabilities/RLS concluída; isso não significa que rotas de negócio foram liberadas.
- Fases 5 e 6: concluídas no escopo backend registrado no roteiro e relatório. Não repetir implementação já fechada sem requisito novo ou falha comprovada.
- Fase 7: três identidades visuais e navegação de demonstração feitas no frontend. Ainda falta conexão segura com catálogo público real, seleção/persistência de preset por loja, editor autenticado da própria vitrine, mídia/logo segura e identidade/carrinho de cliente.
- Fase 8: checkout/pedido. Revalidar preço, disponibilidade, zona e entrega no servidor, com idempotência e snapshot mínimo; não confiar em cálculos ou IDs de produto enviados pelo navegador.

## Invariantes de segurança e produto

- `anyRequest().denyAll()` continua ativo. Rotas de setup, catálogo e vitrine não podem ser abertas por conveniência durante integração visual. Só liberar após matriz endpoint × papel × capability × recurso revisada, CSRF e autorização por objeto/tenant comprovados.
- Senhas: não armazenar texto claro nem hash rápido; a baseline define algoritmo adaptativo. O código atual usa `DelegatingPasswordEncoder` com PBKDF2 versionado; a escolha final/parametrização ainda precisa ser reconciliada com o alvo documentado (Argon2id ou PBKDF2 sob requisito compatível), medida e testada antes de autenticação fora de dev. Não tratar regra documentada como implementação já concluída.
- Upload segue desligado. Não habilitar antes de storage privado operacional, validação/sniff/re-encode no servidor e endpoint autorizado.
- `peditto_app` sem `SUPERUSER`/`BYPASSRLS`; RLS é defesa adicional, não substitui queries tenant-scoped nem autorização por objeto.
- Tema é chave allow-listed, não CSS, HTML, classe, URL ou cor arbitrária. Cliente público recebe tema e dados da loja resolvidos pelo servidor para host/slug verificado.
- Preço, margem, disponibilidade, cobertura, endereço e permissões são validados/recalculados no servidor. Projeção pública de catálogo não expõe custo/margem.
- Geolocalização opcional e fallback CEP/endereço pertencem à Fase 7; revalidação no checkout e snapshot mínimo pertencem à Fase 8.

Os controles detalhados incluem credenciais e rotação de segredos, PostgreSQL least-privilege/RLS, CSRF/sessão, DTO allow-list e rejeição de campos extras, proteção contra enumeração/brute force, autorização multi-tenant/IDOR, privacidade e retenção, upload/SSRF, logging/auditoria, dependências, backups, rate limit, idempotência, webhooks e dinheiro. Consulte a baseline e o threat model para o que é política; consulte o relatório e as fases para saber o que já foi comprovado. Não presumir que toda técnica listada está implementada ou que os testes pendentes passaram.

## Código, arquitetura e algoritmos

- Aplicar [`PADROES_DE_CODIGO_E_ALGORITMOS.md`](PADROES_DE_CODIGO_E_ALGORITMOS.md): nomes claros, uma responsabilidade por unidade, fluxo e efeitos explícitos, DTOs estritos, domínio com invariantes, persistência por migrations, comentários que explicam “por quê” e abstrações proporcionais.
- A organização MVC/arquitetura modular é: controller HTTP fino → serviço/caso de uso → domínio/invariantes → repository scoped; adapters isolam providers/infraestrutura. Não expor CRUD de repository, não colocar regra de negócio pesada no React e não criar interfaces/padrões sem variação ou necessidade concreta.
- Aplicar os aprendizados de *Clean Code* (Robert C. Martin) como critérios de leitura e revisão, sem regras dogmáticas de tamanho: reduzir duplicação e acoplamento, manter nomes/intenção visíveis, funções coesas, erros explícitos e testes focados em comportamento.
- Aplicar *Entendendo Algoritmos* definindo problema, entradas, saídas, invariantes e escala; escolher estrutura apropriada; estimar tempo/memória e medir antes de otimizar. Preferir índices/paginação/constraints no PostgreSQL para dados persistidos. Não inventar grafos ou estruturas avançadas sem requisito e nunca implementar criptografia por conta própria.
- Aplicar os padrões de projeto com parcimônia: Strategy somente onde há variações reais (ex.: providers), Adapter para integrações, máquina de estados explícita para transições; evitar abstrações especulativas e “pattern por pattern”.

## Direção visual acordada

Preservar a identidade Peditto já aprovada e os três estilos próprios. Priorizar hierarquia, contraste, composição, tipografia expressiva porém legível, espaço e motion com propósito. Movimento deve respeitar `prefers-reduced-motion` e não atrasar leitura ou compra. Texto em português brasileiro, direto e humano; explicar valor antes de pressionar cadastro. Evitar IA slop, promessas não comprovadas, gradientes decorativos, fundos pretos/terminal, linhas decorativas genéricas, glassmorphism, neon e componentes visualmente clonados entre temas. Não iniciar um novo tema ou refazer os já aceitos sem o pedido do usuário.

## Próximo caminho recomendado

1. No próximo chat, revisar `git status --short` e o diff local; continuar preservando o trabalho não publicado.
2. Usar [`REVISAO_TECNICA_E_PENDENCIAS.md`](REVISAO_TECNICA_E_PENDENCIAS.md) e [`README_ROTEIRO_MVC_E_SAAS.md`](README_ROTEIRO_MVC_E_SAAS.md) como fontes detalhadas; resolver primeiro os gates pendentes da Fase 3.
3. Depois da segurança/autorização, conectar a Fase 7 aos dados reais sem mudar o esqueleto compartilhado ou liberar rotas prematuramente.
4. Fazer commit/push somente quando o usuário solicitar; antes disso conferir branch, diff e estado remoto.

## Documentos de referência

- [`README.md`](../README.md): visão geral e decisões principais.
- [`README_ROTEIRO_MVC_E_SAAS.md`](README_ROTEIRO_MVC_E_SAAS.md): fases, critérios e ordem planejada.
- [`REVISAO_TECNICA_E_PENDENCIAS.md`](REVISAO_TECNICA_E_PENDENCIAS.md): evidências, limitações e próximos passos técnicos.
- [`BASELINE_SEGURANCA.md`](security/BASELINE_SEGURANCA.md) e [`THREAT_MODEL_MVP.md`](security/THREAT_MODEL_MVP.md): regras de segurança.
- [`REUSO_DAS_DEFESAS_MANGATEOFILO.md`](security/REUSO_DAS_DEFESAS_MANGATEOFILO.md): técnicas analisadas e decisão de adaptar, substituir ou não portar.
- [`PADROES_DE_CODIGO_E_ALGORITMOS.md`](PADROES_DE_CODIGO_E_ALGORITMOS.md): Clean Code, camadas, uso comedido de design patterns e aplicação de *Entendendo Algoritmos*.
- [`0009-vitrine-identidade-e-imagens.md`](decisions/0009-vitrine-identidade-e-imagens.md): temas, conteúdo, destaques e imagem da loja.
- [`PITCH_E_IDENTIDADE_VISUAL.md`](brand/PITCH_E_IDENTIDADE_VISUAL.md): texto e direção visual pública.
- [`frontend/README.md`](../frontend/README.md) e [`docs/ux/README.md`](ux/README.md): arquitetura visual e materiais fora do bundle.
