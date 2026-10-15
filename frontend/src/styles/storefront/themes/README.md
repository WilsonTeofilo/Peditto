# Temas da vitrine

As lojas usam o mesmo componente React, a mesma ordem de seções e os mesmos fluxos. A configuração de cada loja fornece textos, logo, categorias, produtos e imagens. A classe raiz `storefront--<tema>` escolhe uma pele visual distinta.

## Temas

- `original.css` + `original/`: identidade Original Peditto, demonstrada pela Gelatto Totosu.
- `fast-food.css` + `fast-food/`: pele de delivery vermelho e amarelo, aplicada à Dona Erza.
- `natural.css` + `natural/`: identidade verde editorial da Muru Muru Coffee, com verde-floresta, creme natural, sálvia e marrom-café.

## Organização

- `../storefront.css` e `../storefront-responsive.css` mantêm estrutura, semântica e componentes compartilhados.
- Cada pasta de preset agrupa tokens, aparência, movimento, responsividade e decisões de design quando essas camadas existem.
- As regras de cada pele devem ficar escopadas à classe raiz do tema.
- Não duplicar `StorefrontPage` para criar outro tema ou loja.
- Não criar arquivos vazios por camada; separar CSS quando isso tornar a manutenção mais clara.

## Como ampliar a demonstração

1. Para uma nova loja com tema existente, adicione a fixture em `frontend/src/data/demo-stores/`, registre o slug em `slugs.ts` e `index.ts` e cadastre os dados do card em `themePreviews.ts`.
2. Para um tema novo, crie `frontend/src/styles/storefront/themes/<tema>/` e mantenha nele tokens, regras visuais, movimento, breakpoints e decisões próprias. O agregador `<tema>.css` é importado uma vez por `StorefrontPage`.
3. Use a classe raiz `storefront--<tema>` para isolar a skin. A ordem e os componentes continuam no `StorefrontPage` compartilhado.
4. Importe em `src/assets/demo-stores/` apenas imagens efetivamente exibidas. Originais e referências ficam em `docs/ux/demo-stores/`, fora da build do Vite.

O padrão é data-driven: fixtures descrevem lojas, metadados descrevem os cards, e classes CSS escolhem skins fechadas. A seleção real por tenant e sua persistência continuam sendo responsabilidade de integração backend futura.
