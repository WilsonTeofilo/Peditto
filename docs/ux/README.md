# Referências visuais do frontend

Materiais de direção de arte e arquivos-fonte usados para construir as lojas fictícias. Este diretório fica fora de `frontend/src/` e `frontend/public/`, então as imagens não entram no bundle nem são copiadas para o `dist/` do Vite.

## Lojas de demonstração

- `demo-stores/gelatto-totosu/`: logo-fonte e folha de personagem.
- `demo-stores/restaurante-dona-erza/`: logo original e referências do mascote/selo.
- `demo-stores/muru-muru-coffee/`: painéis de identidade, personagem, produtos, cardápio e embalagem; originais usados para produzir recortes otimizados.

Para imagens servidas na interface, use somente os assets importados de `frontend/src/assets/demo-stores/<slug>/`. Não copie referências para `src/` ou `public/`.
