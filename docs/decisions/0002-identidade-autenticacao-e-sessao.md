# ADR 0002 — Identidade, 2FA e sessão

- **Estado:** Aceita para o MVP, revisável após teste de UX.
- **Data:** 14/10/2026.

## Decisão

- Login aceita e-mail normalizado **ou** CPF verificado do usuário **ou** CNPJ verificado da organização, mais senha. CNPJ de organização resolve somente para o owner ativo único; funcionário autentica com seu e-mail ou CPF pessoal, nunca com CNPJ compartilhado. Respostas iguais para identificador inexistente, senha errada, documento inválido ou conta bloqueada, sem enumeração.
- Após validar a senha, o usuário escolhe receber OTP por e-mail ou celular **já verificado**. Para que a escolha esteja disponível, onboarding de dono exige ambos contatos verificados; se um canal estiver temporariamente indisponível, recuperação autenticada deve ser auditada. Para comprador, oferecer todos os canais que ele verificou.
- OTP de uso único e vida curta, segredo gerado por CSPRNG, armazenado como HMAC/hash com chave fora do banco, vinculado ao propósito, conta e tentativa. Tentativas/reenvios têm limites distribuídos; enviar via outbox/provider depois do commit. Nunca logar ou retornar OTP, nunca enviar para destino informado livremente pelo browser.
- Sessão web same-origin usa cookie host-only `HttpOnly`, `Secure`, `SameSite=Lax` ou mais estrito compatível, proteção CSRF, expiração/rotação/revogação. Não compartilhar cookies entre subdomínios de lojas. Avaliar JWT somente se houver requisito de cliente nativo/integração; não implementar JWT só por preferência.
- Papéis e membership são consultados no servidor; claims de papel/tenant no cliente não são autoridade. Login exige 2FA em todas as áreas. Operadores da plataforma têm MFA obrigatório e step-up para preço, billing, campanhas, suporte e exportação.
- Troca de contato/2FA, senha, recuperação, novo dispositivo e revogação de sessão exigem reautenticação, verificação e audit log.

## Consequências

- OTP de SMS/e-mail é vulnerável a phishing, SIM swap e caixa comprometida; suportar limitação, alertas, recuperação forte e avaliar TOTP/passkeys como segundo fator mais resistente para operadores.
- Nenhum segredo de OTP é persistido em texto claro; se outbox precisar transportar o corpo, proteger payload com envelope encryption e apagá-lo após entrega/expiração.
- Dados de login CPF/CNPJ precisam de minimização, proteção em repouso e exclusão/retenção definidos.

## Aceite

Senha correta sem OTP não cria sessão. OTP expirado, reutilizado, de outro propósito ou excedido falha. Canal não verificado não pode ser escolhido.