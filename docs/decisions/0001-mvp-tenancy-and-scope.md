# ADR 0001 — Escopo MVP e tenancy

- **Estado:** Aceita para o MVP, revisável por ADR.
- **Data:** 14/10/2026.
- **Responsáveis:** Produto/Founder (aprovação de produto); engenharia (implementação).

## Contexto

O sistema legado atende uma única confeitaria. O SaaS precisa servir várias lojas sem cruzar catálogo, clientes, pedidos, equipe ou endereços. CPF/CNPJ deve limitar duplicação de conta e trial.

## Decisão

- No MVP, uma organização/documento (CPF ou CNPJ) corresponde a uma loja. O schema conserva `organization_id` para permitir planos multi-loja depois.
- CPF/CNPJ normalizado é único entre organizações elegíveis e recebe no máximo um trial público de 30 dias. Criar outro usuário, telefone, e-mail ou slug não concede trial novo. Qualquer exceção exige revisão humana e evento de auditoria; nunca endpoint público.
- Loja começa vazia, `SETUP_REQUIRED`/`DRAFT`; não semear produtos, clientes, pedidos, equipe fictícia ou usuário/senha padrão. Publicação é ação explícita após completar perfil, catálogo, horário e área/método de atendimento.
- A resolução do subdomínio seleciona o tenant público, mas não autoriza operação. Cada ação privada precisa de usuário, membership ativo, capability e propriedade do recurso.
- Cliente e dados de compra são escopados por loja no MVP. Nenhum histórico ou endereço de uma loja aparece em outra.
- URLs: `slug.peditto.com` para vitrine/contexto da loja; `app.peditto.com` para console da organização; `platform.peditto.com` para equipe/CEO Peditto. Cookies são host-only, nunca compartilhados por `.peditto.com`.

## Consequências

- O MVP não vende plano multi-loja. É possível expandir sem fundir tenants.
- Unicidade em banco é barreira de concorrência, não prova de identidade real. Verificação de documento/contato e antifraude continuam necessários.
- Todas as entidades operacionais futuras têm `tenant_id/store_id`, FK composta onde existe referência entre tabelas da loja e queries tenant-scoped.
- RLS é defesa extra após testar role e connection pool; não substitui authorization.

## Aceite

Nenhum request muda o tenant autorizado por `tenantId`, ID ou Host arbitrário. Loja A não lê/escreve dados de B. Um mesmo CPF/CNPJ não cria segundo trial.