# Temas da vitrine

As lojas usam o mesmo componente React, a mesma ordem de seções e os mesmos fluxos. A configuração de cada loja fornece textos, logo, categorias, produtos e imagens. A classe raiz `storefront--<tema>` escolhe uma pele visual distinta.

## Temas

- `original.css`: identidade Original Peditto, demonstrada pela Gelatto Totosu.
- `fast-food.css` + `fast-food/`: pele de delivery vermelho e amarelo, aplicada à Dona Erza.
- `natural.css`: paleta demonstrativa da Muru Muru; a identidade visual verde final ainda será definida separadamente.

## Organização

- `../storefront.css` e `../storefront-responsive.css` mantêm estrutura, semântica e componentes compartilhados.
- `fast-food/` agrupa tokens, aparência, movimento, responsividade e decisões de design do tema vermelho.
- As regras de cada pele devem ficar escopadas à classe raiz do tema.
- Não duplicar `StorefrontPage` para criar outro tema ou loja.
- Não criar arquivos vazios por camada; separar CSS quando isso tornar a manutenção mais clara.