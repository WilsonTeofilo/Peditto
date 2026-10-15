# Peditto — pitch e identidade visual

**Atualizado em:** 15/10/2026
**Propósito:** orientar a página pública, os textos de produto e o uso da mascote.

## Pitch da página inicial

### Título

**Pedido no lugar certo. Cabeça livre pra tocar a loja.**

### Texto

Pedido anotado num canto, endereço perdido na conversa, cliente perguntando se já saiu. O Peditto organiza cardápio, pedidos e entrega no mesmo balcão.

O cliente encontra o cardápio da sua loja e faz o pedido. Você acompanha o que entrou, o que está em preparo e o que precisa sair. Cadastre seus produtos, defina sua área de entrega e cuide da rotina sem procurar informação em várias conversas.

### Chamada

**Teste por 30 dias. Crie uma loja e monte seu cardápio no seu tempo.**

Botão principal: **Testar por 30 dias**  
Alternativa: **Ver como funciona**

### Blocos curtos

- **Cardápio sob seu cuidado:** cadastre produtos, adicionais, preços e disponibilidade.
- **Pedido com começo e fim:** acompanhe o andamento sem refazer anotações.
- **Entrega no seu alcance:** defina a área que atende e confira o endereço antes de aceitar.

O texto fala da rotina real descrita no TCC Chokko Melt: pedido manual se perde, atendimento atrasa e o controle do que foi vendido fica difícil. Não promete comissão zero, aumento garantido de vendas, integração com marketplace ou recursos ainda sem endpoint.

## Leitura da mascote oficial

A personagem enviada tem leitura de cozinheira/entregadora em estilo chibi com linguagem de anime: cabeça expressiva e maior que o tronco, silhueta compacta, cabelo ameixa, avental, camisa clara, laço e detalhes amarelos. A sacola de delivery liga a personagem ao serviço sem precisar de ícones genéricos. Contorno castanho escuro e sombra em blocos deixam a arte parecida com adesivo impresso, acolhedora e legível em tamanho pequeno. As expressões e poses são parte útil da identidade; não precisam ser reduzidas a uma única pose sorridente.

### Paleta retirada do próprio guia oficial

| Uso | Cor |
|---|---|
| Ameixa | `#54245F` |
| Amarelo | `#FFC33C` |
| Branco quente | `#F7EEE2` |
| Terracota | `#9E5A35` |
| Tinta e contorno | `#38251F` |

O produto combina essa identidade com superfícies de papel, bordas visíveis, sombras curtas e controles que lembram janelas antigas. Sem neon, fundo de terminal escuro, gradiente decorativo ou blur plástico. A tipografia usa três funções: display sans robusta, texto de leitura serifado e mono para etiquetas/estados retrô; não carregar fontes de terceiros sem necessidade de desempenho/privacidade.

## Uso de marca e movimento

- Usar o avatar oficial recortado para favicon e assinatura; reservar a pose completa para apresentação e momentos de onboarding.
- Preservar cabelo, roupa, proporção chibi, paleta, contorno e desenho do guia. Novas poses exigem origem/arte aprovada, não redesenho automático que altere a identidade.
- Motion curto e com propósito: personagem respirando/flutuando levemente, pedidos entrando na janela e transições ao rolar. Respeitar `prefers-reduced-motion`; nada deve atrasar o CTA nem exigir movimento para entender a página.
- Emojis não substituem ícones. Texto fica direto e específico; não usar promessas de marketing que o produto não consegue provar.

## Vitrines de demonstração e presets

A vitrine pública de demonstração apresenta três lojas fictícias com um esqueleto React compartilhado e identidades completas próprias. O tema muda direção artística, tipografia, composição, componentes visuais e movimento; não é somente uma troca de paleta.

| Preset | Loja fictícia | Direção visual |
|---|---|---|
| Peditto original | Gelatto Totosu | Ameixa, amarelo quente e creme; identidade acolhedora da Peditto. |
| Fast food | Restaurante Dona Erza | Vermelho, amarelo e creme; ritmo de lanchonete, confeitaria e delivery. |
| Café natural | Muru Muru Coffee | Verde floresta, sálvia, marfim e marrom café; cafeteria artesanal e familiar. |

Muru Muru foi fundada em 1988 por Alberto José e fica no bairro Pilar, em Ouro Preto–MG. Hoje Alberto Júnior gerencia o negócio e Wilson Teófilo cuida da cozinha e aprimora as receitas antigas. Esse contexto é específico da fixture fictícia e não deve virar texto genérico do produto.

As rotas `/loja/gelatto-totosu`, `/loja/restaurante-dona-erza` e `/loja/muru-muru-coffee` são demonstrações pré-cadastro, com dados ilustrativos, sem API, persistência ou pedidos reais. A galeria está em `/#temas`; `/loja/demo` é um alias legado para Dona Erza. A estratégia de implementação e segurança está em [`docs/decisions/0009-vitrine-identidade-e-imagens.md`](../decisions/0009-vitrine-identidade-e-imagens.md), e o mapa de assets está em [`docs/ux/README.md`](../ux/README.md).

## SEO e aquisição orgânica

Público inicial: pequenos restaurantes, lanchonetes, confeitarias e lojas de delivery que buscam organizar cardápio/pedidos ou ter canal próprio de pedido. Pesquisar intenção e linguagem do público antes de publicar novas páginas, por exemplo “sistema de delivery para restaurante”, “cardápio digital para delivery”, “sistema para receber pedidos online”, “delivery próprio para lanchonete” e “alternativa a marketplace de delivery”. Consultas com nomes como iFood, Keeta, 99 e Anota Aí podem entrar na pesquisa de demanda; qualquer página comparativa precisa ser factual, útil e revisada, sem insinuar parceria/afiliação. Não repetir palavras-chave sem utilidade.

Metas de descrição, rota pública indexável, favicon e texto inicial foram preparados. A missão completa de SEO está na Fase 13 do roteiro. Domínio final ainda precisa ser definido antes de publicar canonical, sitemap absoluto, redes sociais e dados estruturados.

Referências oficiais: [SEO Starter Guide](https://developers.google.com/search/docs/fundamentals/seo-starter-guide), [guia técnico para desenvolvedores](https://developers.google.com/search/docs/fundamentals/get-started-developers), [favicon no Google Search](https://developers.google.com/search/docs/appearance/favicon-in-search), [dados estruturados Organization](https://developers.google.com/search/docs/appearance/structured-data/organization) e [Core Web Vitals](https://developers.google.com/search/docs/appearance/core-web-vitals).
