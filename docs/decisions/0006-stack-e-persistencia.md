# ADR 0006 — Stack e persistência

- **Estado:** Aceita para MVP.
- **Data:** 14/10/2026.

## Decisão

- Java 21 + Spring Boot 4.1.x, Spring MVC, Spring Security, Spring Data JPA/Hibernate e Bean Validation.
- PostgreSQL como fonte relacional, Flyway como única forma de migration e Hibernate `ddl-auto=validate`. `NUMERIC`/`BigDecimal` para moeda; UUID; timestamps UTC (`timestamptz`).
- React + TypeScript + Vite + Tailwind para app. API REST versionada JSON. MVC é controller HTTP fino, services/casos de uso, domínio e repositories; não expor repository CRUD automaticamente.
- Monólito modular inicialmente; não distribuir serviços até throughput/isolamento operacional justificarem o custo.
- Docker Compose apenas para dev local, banco bind em loopback, senha por variável, sem dado real/seed de credenciais.

## Consequências

Boot 4.x é major version recente: antes de adicionar lib, verificar compatibilidade e suporte. Pin estável atual no POM, atualização de patch acompanhada. Não declarar Spring Data REST automático; controllers explicitamente autorizados. Retenção de SQL logs desligada em produção.