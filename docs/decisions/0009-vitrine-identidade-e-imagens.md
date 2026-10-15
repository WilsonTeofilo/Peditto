# ADR 0009 — Personalização da vitrine e otimização de imagens

- **Estado:** requisitos aceitos para planejamento; migrations e upload continuam pendentes.
- **Data:** 15/10/2026.

## Conteúdo e identidade da loja

A vitrine pública `/loja/demo` é uma demonstração antes do cadastro e não exige login. Depois do cadastro, o lojista administra os dados em painel autenticado; clientes acessam a vitrine pública real da loja por sua URL própria. A mesma apresentação pode ser reutilizada, sempre com dados e tema resolvidos no servidor dentro do tenant correto. A demonstração nunca representa uma loja operacional nem compartilha catálogo entre tenants.

### Temas por loja

Planejar três presets fechados e selecionáveis no painel: `PEDITTO_PURPLE_GOLD` (roxo e amarelo), `DELIVERY_RED_YELLOW` (vermelho, branco e amarelo) e `WARM_BROWN` (tons terrosos para cafés e restaurantes). Cada preset mapeia para tokens CSS definidos pelo produto. O tema fica associado à loja, é retornado apenas na projeção pública daquela loja e não aceita CSS, HTML, cores arbitrárias ou URLs fornecidos pelo usuário. A seleção exige autorização tenant-scoped no backend; nunca pode alterar a vitrine de outra loja.

A vitrine poderá mostrar um logotipo quadrado e textos curtos definidos pelo lojista. O conteúdo é texto simples, com tamanho validado no servidor; o React faz escape normal. Não aceitar HTML, CSS, scripts, URLs de imagem externas ou temas arbitrários.

Limites iniciais para preservar o layout:

| Campo | Limite rígido | Orientação de escrita |
|---|---:|---:|
| Linha curta acima do título | 55 caracteres | Até 35 para evitar quebras em celulares |
| Cada linha do título | 28 caracteres | Preferir até 20 e uma ideia curta por linha |
| Descrição da loja | 150 caracteres | Preferir até 120 |
| Selo do destaque | 32 caracteres | Ex.: “feito hoje” |
| Título do destaque | 48 caracteres | Preferir até 38 |
| Descrição do destaque | 120 caracteres | Preferir até 90 |
| Texto do botão do destaque | 22 caracteres | Manter a ação curta |

O painel deve mostrar contagem de caracteres e prévia em larguras móveis e desktop. O servidor volta a validar cada limite; contador e `maxLength` do navegador são apenas ajuda de edição.

## Destaques

- **Mais pedido:** escolher automaticamente o produto ativo que mais vendeu em pedidos concluídos. Pedidos cancelados, recusados ou reembolsados não entram no cálculo; a consulta é sempre tenant-scoped e agrega apenas unidades, sem expor dados de clientes. Recomenda-se janela móvel de 30 dias para refletir o momento, mas o período e o fallback para lojas novas ainda precisam ser confirmados antes de implementar essa regra.
- **Destaque da loja:** o comerciante seleciona um segundo produto ativo e personaliza selo, título e descrição. O servidor confere que o produto pertence à loja e é publicável. Se ele coincidir com o mais pedido, a vitrine não repete o card; o painel pede ao lojista outro destaque.
- Até existir histórico de pedidos concluídos, a vitrine não deve inventar popularidade. Deve usar o destaque manual ou ocultar o selo de “mais pedido”.

## Logotipo e fluxo de imagem

O logotipo é um asset da loja, separado do catálogo de produtos e referenciado por chave/ID privado. O banco guarda metadados e a associação tenant-scoped, nunca o binário. O asset atual usa storage privado e a tabela `store_assets`; criar suporte a logo sem enfraquecer a FK de tenant, o RLS ou as regras de autorização.

Fluxo recomendado no navegador:

1. Aceitar seleção de PNG ou JPEG até **2 MiB**; rejeitar SVG e formatos animados.
2. Mostrar prévia com recorte quadrado ajustável antes do envio. Ao confirmar, redimensionar para **512 × 512 px** (teto de **800 × 800 px**) e gerar PNG otimizado, preservando transparência quando houver.
3. Se a saída passar de **512 KiB**, reduzir gradualmente para 384 e 256 px; se ainda exceder o limite, informar ao usuário e não enviar. Limpar a imagem em memória e URLs `blob:` ao concluir/cancelar.
4. Exibir progresso real das etapas “preparando” e “enviando”; não enviar a imagem original nem manter uma cópia no armazenamento do navegador.

Esses limites do cliente melhoram UX, largura de banda e custo de storage, mas podem ser adulterados. A API deve impor seus próprios limites, validar magic bytes e formato decodificado, dimensões e pixels, rejeitar conteúdo inconsistente e re-encodar em formato canônico antes de gravar sob chave aleatória no storage privado. A API retorna apenas referência/URL autorizada curta; não confiar em MIME, nome, dimensões ou compressão informados pelo navegador. A validação genérica atual só aceita PNG/JPEG, limita entrada a 5 MiB e re-encoda em PNG; criar limites específicos para logo sem alterar a política das fotos de produto. Upload segue desligado até rota, capability, CSRF, RLS e retenção serem revisados.

## Leveza da vitrine

- Preferir CSS e SVGs pequenos no bundle; fotos devem ter dimensões declaradas e `loading="lazy"` abaixo da primeira dobra. Na demonstração, manter originais separados dos derivados WebP reduzidos e dimensionados. Em produção, gerar variantes otimizadas no storage/CDN quando configurado.
- Manter animações em `transform`/`opacity`, pausar ciclos fora da viewport e respeitar `prefers-reduced-motion`. Evitar vídeos/autoplay e bibliotecas de animação pesadas sem necessidade medida.
- Ativar skeleton/loading somente quando uma requisição real demorar; a prévia estática não simula espera.

**Lembrete solicitado pelo dono do produto:** depois que a animação principal da apresentação estiver concluída, retomar o loading para primeira entrada ou carregamentos reais lentos de celular/rede. O indicador deve aparecer só após um atraso curto e não deve substituir conteúdo instantâneo por uma tela de espera.
