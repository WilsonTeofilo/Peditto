# Linha de base de segurança do Peditto

**Escopo:** controles básicos que precisam existir antes de expor endpoints privados ou usar dados reais. Este documento complementa o [threat model](THREAT_MODEL_MVP.md), as [defesas adaptadas do MangaTeofilo](REUSO_DAS_DEFESAS_MANGATEOFILO.md) e as regras de fase; não substitui revisão de risco por recurso.

**Adicionado ao plano:** 15/10/2026.

## 1. Senhas e credenciais de usuário

- Senha nunca é armazenada em texto claro, criptografada de forma reversível, nem com SHA-256/MD5 simples. Usar `PasswordEncoder` do Spring Security com algoritmo adaptativo, salt único gerado pela implementação, formato versionado e comparação via biblioteca.
- **Política alvo:** Argon2id, inicialmente ao menos 19 MiB de memória, 2 iterações e paralelismo 1, conforme OWASP; medir o custo no ambiente de execução para impedir tanto verificação barata quanto negação de serviço por configuração excessiva. Se houver requisito de FIPS, considerar PBKDF2-HMAC-SHA-256 com pelo menos 600.000 iterações e confirmar a implementação/versão usada. Não implementar algoritmo criptográfico próprio.
- **Estado atual a reconciliar:** o código usa `DelegatingPasswordEncoder` com PBKDF2 (`defaultsForSpringSecurity_v5_8`). Antes de habilitar autenticação em ambiente compartilhado, conferir os parâmetros efetivos dessa versão, medir custo, definir o padrão desejado e alinhar código, testes e ADR. A presença de `DelegatingPasswordEncoder` por si só não prova que os parâmetros estão adequados.
- Usar uma lista curta e mantida de senhas comuns, esperadas e comprometidas ao criar ou redefinir senha. Não exigir trocas periódicas sem sinal de comprometimento; não impor regras de composição previsíveis. Permitir frases-senha longas e definir limite máximo explícito em bytes/caracteres para evitar truncamento e abuso computacional.
- No login bem-sucedido, permitir migração gradual de hash/parametrização (`upgradeEncoding`) sem reset em massa. O reset usa token/OTP aleatório, limitado, de uso único, curto e vinculado ao propósito; senha nunca vai para logs, analytics, audit ou mensagens de erro.
- Mensagens de autenticação e recuperação não confirmam se e-mail, telefone ou documento existe. Rate limit e MFA continuam necessários mesmo com hash forte.

## 2. Segredos da aplicação e do ambiente

- Segredos ficam fora do Git, frontend, imagem de container, logs e documentação. `.env.example` contém apenas nomes e valores fictícios; `.env` real deve ser ignorado pelo Git e ter ACL restrita.
- Produção usa secret manager/injeção protegida, credenciais distintas por ambiente e por função. Não reutilizar credenciais local, CI, staging e produção.
- Nunca imprimir valor de variável, exception de configuração, URL JDBC com senha, header `Authorization`, cookie, OTP, token ou chave. Diagnóstico informa somente o nome da configuração ausente e o request/correlation ID.
- Definir rotação e revogação para senha de banco, chave de sessão/MFA, HMAC, AES-GCM, SMTP, Twilio e providers futuros. Chaves com versão precisam permitir migração planejada; chave antiga não fica ativa indefinidamente.
- Se um segredo aparecer em arquivo, commit, issue, saída de terminal ou log, tratar como comprometido: revogar/rotacionar primeiro, investigar uso e então limpar o repositório conforme procedimento. Apagar o arquivo não invalida o segredo.

## 3. PostgreSQL e dados pessoais

- Runtime e Flyway usam roles distintas. `peditto_app` não é owner, superuser nem `BYPASSRLS`; não tem `CREATEDB`/`CREATEROLE`/DDL. `peditto_migrator` só é usado pela execução controlada de migrations. Credenciais fortes, exclusivas e não expostas à rede pública.
- Desenvolvimento limita a porta do PostgreSQL a loopback. Produção restringe entrada ao serviço autorizado e exige TLS com verificação de certificado entre aplicação e banco.
- Queries usam parâmetros; concatenação de dados em SQL é proibida. Sort, coluna, tabela e direção dinâmicos vêm de allow-list fechada. Validação de input não substitui queries parametrizadas nem autorização.
- Constraints, FKs, transações e índices garantem invariantes críticas além da validação Java. Schema muda por Flyway; Hibernate continua `validate`. Backups são criptografados, têm acesso restrito e restore é ensaiado.
- CPF/CNPJ normalizado hoje existe nas tabelas de organização/usuário. Antes de dado real, definir acesso mínimo e retenção; avaliar criptografia em nível de aplicação mais índice de busca por HMAC (blind index), com chave separada e versionada. HMAC não anonimiza o documento. Não escrever documentos em logs, métricas ou mensagens de exceção.
- E-mail, telefone, endereço e coordenadas são dados pessoais: coletar só o necessário, limitar quem consulta, não retornar em DTO público e aplicar retenção/expurgo documentados. Dados de teste devem ser sintéticos.

## 4. Sessões, navegador e transporte

- Sessão opaca de alta entropia; banco guarda somente digest, token não vai para `localStorage`, URL, analytics ou log. Cookie de sessão `__Host-`, `Secure`, `HttpOnly`, `SameSite`, `Path=/`; não compartilhar sessão entre subdomínios de lojas.
- CSRF permanece habilitado para autenticação por cookie. O token CSRF pode ser lido pelo frontend conforme o contrato, mas não é token de sessão. Validar Origin/Referer como defesa adicional, não como autorização. CORS fica desligado até haver origem diferente e allow-list exata.
- Login, reset, mudança de senha/fator e alteração de privilégios invalidam ou revogam as sessões conforme a política. Rotação/reuso de sessão é validada no backend.
- HTTPS obrigatório fora do desenvolvimento; HSTS só é enviado na borda HTTPS configurada. Só confiar em `Forwarded`/`X-Forwarded-*` quando o proxy conhecido sobrescrever os headers e estiver declarado como confiável.
- Não colocar segredo em query string. Respostas de erro de produção não incluem stack trace, SQL, caminho local, configuração, hash nem informação para enumeração.

## 5. Entrada, saída e endpoints

- Cada endpoint recebe DTO específico, com validação sintática e semântica, limites de tamanho/quantidade e allow-list positiva. Rejeitar campos desconhecidos; não fazer binding de JSON diretamente em entidade JPA e não copiar propriedades genericamente.
- Todo endpoint começa negado. Liberar somente método/rota explicitamente necessários, com autorização de função **e** propriedade/tenant no servidor. UUID, slug, host e botão oculto não são autorização.
- Resposta é DTO mínimo para aquele ator/finalidade; excluir hash, OTP, tokens, segredo MFA, documento/contato desnecessários, custo/margem interna e dados de outros tenants.
- Parâmetros de paginação/filtro/ordenação têm limites e allow-list. Rate limit cobre login, cadastro, verificação, recuperação e envio de OTP; limite de request é aplicado antes de consumir corpo ilimitado.
- Saída HTML é escapada conforme contexto; texto de usuário não vira HTML confiável. Uploads continuam fechados até validação real de tipo/conteúdo, limites, reprocessamento e storage privado.

## 6. Logs, erros e dependências

- Registrar eventos de segurança úteis (login aceito/negado, recuperação, alteração de fator/papel, exportação, bloqueio, falha de provider), com ator interno, horário, ação, resultado e request ID. Nunca registrar senha, OTP, cookie, token completo, chave, documento, endereço ou corpo integral do pedido.
- Sanitizar valores não confiáveis para evitar log injection. Logs têm acesso mínimo, retenção definida, proteção contra alteração e alerta para padrões relevantes; log não é repositório de auditoria financeira.
- Erros externos são estáveis e sem detalhes internos; detalhe técnico fica em log restrito e redigido, correlacionado por request ID.
- Dependências têm versões fixadas e atualizações acompanhadas. Revisar advisories, licença e manutenção antes de adicionar biblioteca; scanner de dependências, scanner de segredos e SBOM têm resultados separados.
- Segredos e dependências são verificados em CI antes de merge; resultado do scanner não pode revelar o valor encontrado.

## 7. Verificação obrigatória por mudança

Antes de concluir qualquer endpoint ou migration, confirmar:

1. DTO aceita apenas os campos previstos e valores válidos; payload com campo privilegiado ou desconhecido é rejeitado.
2. Rota sem autenticação, papel, membership, tenant ou ownership corretos falha sem expor a existência do recurso.
3. Valores financeiros/estado/tenant/fator são derivados ou recalculados pelo servidor.
4. Erro, log, métrica, audit e response não contêm segredo ou PII fora da finalidade.
5. Corrida, repetição, timeout e falha externa têm comportamento definido; constraints e idempotência protegem a persistência quando necessário.
6. Migration tem estratégia de compatibilidade/rollback operacional, não apaga histórico silenciosamente e foi verificada contra a role/runtime real.

Testes automatizados devem ser acrescentados junto à mudança que introduz o comportamento. A fase final de lançamento reúne as provas e a revisão independente; ela não substitui a verificação contínua.

## Ordem de adoção no Peditto

1. Fechar agora a política/parametrização do `PasswordEncoder`, tratamento de CPF/CNPJ, configuração segura de `.env`/segredos e privilégios efetivos das roles PostgreSQL.
2. Confirmar proteção HTTP, erros/logs, limites e sessão/CSRF antes de ativar os fluxos da Fase 3 em ambiente compartilhado.
3. Aplicar os gates de autorização, tenant e RLS antes de criar endpoints privados de loja.
4. Repetir a lista por feature: catálogo, upload, carrinho, pedidos, billing, exportação e console de plataforma.
5. Antes de piloto/produção, concluir TLS, backup/restore, retenção, scanner/CI, monitoramento e revisão independente conforme o roteiro.

## Referências primárias

- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Spring Security — Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [NIST SP 800-63B-4 — Authentication and Authenticator Management](https://pages.nist.gov/800-63-4/sp800-63b.html)
- [OWASP Database Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Database_Security_Cheat_Sheet.html)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [OWASP SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
