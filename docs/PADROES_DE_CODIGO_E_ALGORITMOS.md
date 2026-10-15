# Padrões de código e uso de algoritmos

Este documento complementa a especificação de segurança e o [roteiro de fases](README_ROTEIRO_MVC_E_SAAS.md). Vale para Java/Spring, React/TypeScript e SQL. O objetivo é deixar mudanças fáceis de ler, revisar, operar e evoluir por qualquer pessoa da equipe.

## Princípios de implementação

- **Nomes dizem o que o código representa.** Use nomes de domínio (`DeliveryZone`, `TrialPolicy`, `OrderTransition`) e verbos claros para ações. Evite siglas locais, nomes genéricos (`data`, `manager`, `helper`) e comentários que compensem nomes ruins.
- **Uma unidade, uma responsabilidade.** Controller traduz HTTP; application service coordena o caso de uso/transação; domínio aplica invariantes; repository consulta/persiste; adapter integra serviços externos. Não crie camadas ou interfaces sem necessidade concreta.
- **Fluxo explícito.** Prefira métodos pequenos com parâmetros e retornos tipados. Evite `Map<String,Object>`, estado global, reflexão, `BeanUtils.copyProperties`, abstrações mágicas e chamadas que escondem I/O.
- **Consistência de vocabulário.** Escolha verbos estáveis para operações equivalentes (`find`, `create`, `update`) e nomes pronunciáveis/pesquisáveis. Use cláusulas de guarda para evitar aninhamento; agrupe parâmetros relacionados num comando tipado quando a assinatura ficar difícil de entender. Não force limites artificiais de linhas ou parâmetros.
- **Tipos com invariantes.** Introduza value objects quando centralizarem validação e evitarem misturar conceitos (por exemplo `Money` ou `EmailAddress` usados em vários casos). Não crie tipos para cada primitivo sem benefício claro, nem duplique validação sem autoridade definida.
- **Efeitos explícitos.** Consultas retornam informação; comandos alteram estado. Nomes e limites transacionais devem tornar efeitos visíveis. Retornos opcionais/coleções tipados são preferíveis a `null`.
- **Falhe cedo e com contexto seguro.** Valide limites na entrada, mantenha invariantes no domínio e constraints no PostgreSQL. Erros externos são estáveis e sem PII, segredos ou detalhes internos.
- **Segurança no servidor.** DTO imutável por caso de uso, allow-list de campos, conversão manual, tenant/ator derivados do contexto autenticado e autorização por objeto em cada rota. A interface apenas melhora a experiência.
- **Persistência explícita.** Flyway altera schema; Hibernate valida. Dinheiro usa `BigDecimal`/`NUMERIC`; instantes persistidos usam UTC; mudanças de estado e efeitos externos são transacionais/idempotentes conforme o caso.
- **Dependências com propósito.** Antes de adicionar biblioteca, confirmar compatibilidade com as versões fixadas, manutenção, superfície de ataque e necessidade real. Integrações ficam atrás de adapters.
- **Abstrações proporcionais.** Use polimorfismo/Strategy quando houver variações reais e estáveis; não crie interface para cada classe nem framework interno antes do primeiro caso concreto.
- **Sem otimização prematura.** Primeiro uma solução simples e correta; otimizar quando volume, latência ou custo justificar. Registrar a medição e preservar a mesma regra de negócio.

## Clean Code e padrões de projeto

As ideias de *Clean Code*, de Robert C. Martin, são referência de revisão, não uma lista dogmática de limites. Aplicar nomes que revelem intenção, funções coesas, responsabilidades claras, tratamento explícito de erros, redução de duplicação e testes de comportamento. Não impor tamanho universal de função/arquivo, nem quebrar um fluxo simples em helpers só para cumprir uma regra estética.

Na arquitetura modular MVC, manter controller HTTP fino, caso de uso/application service responsável pela orquestração, domínio responsável por invariantes e repository responsável por consultas/persistência. Adapters isolam integrações externas. Dependências devem apontar para regras estáveis; não colocar regra de negócio no React ou no controller.

Design patterns entram quando resolvem uma variação ou acoplamento observável: Strategy para políticas/providers realmente intercambiáveis, Adapter para integrações externas, e transições explícitas para máquinas de estado. Não criar uma interface, factory, manager ou camada para cada classe por hábito. Escolher a solução mais simples que preserve segurança, legibilidade e evolução.

## Comentários

Comentários são para **por que**, invariantes, limitações externas ou decisões que o código não revela. Não narrar cada linha nem repetir o nome do método. Documentar contratos públicos e algoritmos cuja correção dependa de uma pré-condição. Manter comentário junto ao código que explica e atualizá-lo na mesma mudança.

Comentários úteis, em princípio:

- explicam por que o host encaminhado não é confiável;
- registram por que o trial só começa após os contatos verificados;
- documentam pré-condições de uma consulta espacial ou de concorrência;
- apontam comportamento específico de um provider/API externa.

Comentários dispensáveis:

- `// incrementa o contador` antes de `counter++`;
- blocos longos que repetem a documentação da feature;
- comentários TODO sem responsável/contexto ou que substituem uma tarefa rastreável;
- código comentado que deveria ter sido removido.

## Usar *Entendendo Algoritmos* com critério

Para cada problema com busca, ordenação, deduplicação, fila, rate limit ou cálculo espacial:

1. Definir entrada, saída, invariantes, escala esperada e comportamento de erro.
2. Escolher estrutura de dados adequada; explicar alternativas apenas quando a escolha impactar latência, memória, concorrência ou legibilidade.
3. Estimar tempo e memória (por exemplo, `O(1)`, `O(log n)`, `O(n)`, `O(n log n)`) no tamanho que cresce. Evitar trocar uma coleção simples por uma estrutura complexa sem evidência.
4. Para algoritmos não triviais, registrar a complexidade e pré-condições em comentário curto ou nota de design, e cobrir limites/casos extremos com testes.
5. Medir antes de otimizar. Considerar índices e plano de execução no banco; não carregar listas inteiras para fazer em memória o que PostgreSQL pode filtrar/paginar corretamente.

### Aplicações previstas no Peditto

| Problema | Abordagem inicial | Cuidado |
|---|---|---|
| Lookup de sessão, idempotência e identidade única | Índice/constraint no PostgreSQL; consulta por chave (`O(log n)` típico em B-tree) | Não buscar todos os registros e filtrar no Java; constraints são a última barreira contra corridas. |
| Listas e relatórios grandes | Paginação no banco; keyset/cursor para fluxo crescente | Limite máximo de página, ordem estável e filtro tenant-scoped; offset profundo pode ficar caro. |
| Deduplicação de eventos/itens em memória | `Set`/`Map` com chave explícita (`O(1)` médio) quando o conjunto cabe na memória | Para integridade distribuída, usar unique constraint/idempotency no PostgreSQL, não somente cache local. |
| Rate limiting | Janela/contador atômico persistente no MVP; avaliar token bucket/sliding window distribuído conforme escala | Chaves com fingerprint HMAC; operação concorrente atômica; limites por IP e identidade/destino. CAPTCHA complementa, não substitui. |
| Cobertura por raio | Pré-filtro espacial/index quando adotado; cálculo geodésico server-side de distância entre ponto e centro | Validar coordenadas e unidade; círculo é aproximação de cobertura, não rota/tempo de entrega. Aplicar de novo no checkout. |
| Estado de pedido | Máquina de estados com transições permitidas explícitas | Não modelar como grafo genérico se um conjunto de transições simples basta; concorrência exige versão/lock. |
| Busca em coleção já carregada e ordenada | Busca binária (`O(log n)`) apenas se a coleção for pequena, ordenada e permanecer em memória | Para dados persistidos, preferir índice/consulta SQL; ordenar a cada busca pode apagar o ganho. |
| Ordenação em memória | Ordenação da biblioteca da linguagem (`O(n log n)` típico) | Usar apenas para lotes limitados; paginação e `ORDER BY` pertencem ao banco para datasets grandes. |
| Entrega assíncrona de eventos | Fila/outbox com consumo limitado e idempotente | Retentativas precisam de backoff, limite e estado de falha; um `Queue` local não substitui persistência distribuída. |
| Exportação de dados | Streaming paginado e limitado | Evitar carregar dataset inteiro; autorização e tenant scope na consulta e no arquivo. |
| Texto de busca de catálogo | Filtros/índices do PostgreSQL inicialmente | Usar motor externo apenas quando requisitos e volume justificarem; manter autorização tenant no servidor. |

Algoritmos de grafos (BFS/DFS/caminho mínimo), programação dinâmica e estratégias gulosas só entram quando houver um requisito concreto que modele naturalmente o problema. Cálculo de rota/tempo de entrega será responsabilidade de um provider de mapas escolhido; o círculo de cobertura do MVP não é um problema de menor caminho.

**Criptografia não é exercício de implementação de algoritmo.** Para senha, HMAC, geração aleatória, comparação constante e AEAD, usar primitivas e bibliotecas maduras suportadas pela plataforma, com parâmetros e segredos administrados. Não implementar cifra, hash de senha, RNG ou protocolo “baseado no livro”.

## Revisão de código antes de concluir uma tarefa

- O caso de uso pode ser entendido pela leitura do fluxo principal sem saltar por uma cadeia de helpers genéricos?
- Os nomes e tipos impedem estados/payloads ambíguos? Os dados de cliente são validados e mapeados explicitamente?
- Toda consulta e mutação privada restringe tenant, ator, papel e propriedade no servidor?
- Concorrência, repetição de request, limites e falhas externas têm comportamento definido?
- Há consulta N+1, carregamento integral, ordenação não determinística ou trabalho quadrático evitável?
- O comentário explica motivo ou invariável não evidente? Código morto, TODO sem ação, segredo ou PII em log foram removidos?
- Mudança de schema é aditiva/compatível e possui migration reversível operacionalmente ou estratégia expand/contract?
- A solução é a menor que atende aos requisitos sem sacrificar segurança, legibilidade ou operação?

## Diretriz para ferramentas automáticas

Formatters, análise estática e scanners são apoio à revisão, não substitutos. Fixar configuração e versões antes de introduzir ferramentas. Não suprimir alertas sem justificativa curta e rastreável. Quando testes forem implementados, priorizar comportamento de domínio, limites algorítmicos, concorrência, contrato HTTP e isolamento tenant; não testar detalhes privados sem efeito observável.
