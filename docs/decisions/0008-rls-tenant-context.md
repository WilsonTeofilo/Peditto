# ADR 0008 — RLS após identidade e contexto de tenant

- **Status:** Aceita
- **Data:** 2026-10-14
- **Contexto:** A Fase 1 prepara o schema multi-tenant antes dos endpoints. Uma policy RLS só é correta se o tenant vier de membership autenticada no servidor e for definido com `SET LOCAL` na mesma transação que usa a conexão. Definir o tenant via header/host do browser, ou usar `SET` de sessão, abre bypass ou vazamento entre conexões Hikari.
- **Decisão:** RLS será ativado antes de liberar rotas privadas. A V10 habilita e força RLS em 14 tabelas operacionais; o transaction manager define `peditto.organization_id` com `set_config(..., true)` em cada transação. Contexto vazio nega acesso e o valor local deve desaparecer no commit/rollback antes da conexão voltar ao pool. O usuário aprovou explicitamente a migration e a configuração em 14/10/2026.
- **Privilégio:** O papel runtime `peditto_app` tem `NOBYPASSRLS`, não é owner das tabelas e não pode fazer DDL; o papel migrator é separado e também não tem `BYPASSRLS`.
- **Limite:** Policies baseadas em GUC são defesa adicional contra queries sem filtro tenant. Como o papel da aplicação consegue definir GUC, isso não protege contra SQL injection executado como o próprio papel; autorização por objeto e queries tenant-scoped continuam obrigatórias.
- **Consequência:** Código da migration e do transaction manager está presente, mas V10 ainda depende de execução em PostgreSQL e teste de pool alternando tenants. Nenhum endpoint privado pode ser liberado até provar isolamento e reset automático do contexto. Rotas globais de autenticação, outbox e rate limit precisam de escopo/papel próprio, não de tenant inventado.
