# ADR 0004 — Área de entrega e localização

- **Estado:** Aceita para MVP; provider e tratamento de dados pendentes.
- **Data:** 14/10/2026.

## Decisão

- A loja define ponto de origem, raio circular e ativo/inativo; ambos loja e cliente veem a mesma geometria publicada.
- Cliente pode acionar geolocalização do navegador; explicar finalidade antes do prompt. Se negar/não suportar, aceitar CEP/endereço digitado. Não rastrear em background.
- Validar cobertura no servidor usando distância geodésica ponto-a-centro <= raio, além de validar endereço/cidade/serviço. Repetir no checkout e guardar snapshot do endereço/taxa do pedido.
- Não “capturar todas as ruas”: o GPS fornece um ponto aproximado e geocodificação resolve o endereço. A V1 não promete tempo de rota nem cobertura por bairro. Polígonos, ruas excluídas e tempo de rota ficam posteriores.
- Guardar somente coordenada necessária e pelo menor prazo justificado; não publicar ponto de residência do cliente. A permissão do browser não substitui base legal, aviso de privacidade ou avaliação do provedor.

## Pendente

Escolher provedor de mapa/geocoder após avaliar cobertura local, custo, licença de tiles, limites, retenção e termos. Definir precisão, prazo de exclusão e fallback se provider indisponível.