# ADR 0007 — Adiamento de escolha de provedores externos

- **Estado:** Aceita com atualização parcial em 14/10/2026; SMTP e Twilio foram escolhidos para OTP após autorização explícita do fundador. Pagamentos, mapas, arquivos e observabilidade continuam adiados.
- **Data:** 14/10/2026.
- **Responsável:** fundador/produto Peditto; revisão técnica do responsável pelo backend.

## Decisão

A integração de OTP usa SMTP para e-mail e a API HTTPS do Twilio para SMS. O domínio depende de `OtpMessageGateway`; a implementação concreta limita o egress ao host fixo `api.twilio.com`, aplica timeout e não registra corpo, código, destino ou resposta do provider. SMTP exige STARTTLS e timeouts. O worker permanece desabilitado por padrão. Credenciais são injetadas somente pelo ambiente/secret manager, e o aplicativo falha ao iniciar se a entrega for habilitada com configuração incompleta.

Pagamentos, mapas/geocoding, armazenamento de arquivos e observabilidade continuam sem fornecedor escolhido. Cada integração futura terá porta (`interface`) e adapter, com timeout, validação de resposta, política de dados mínimos, idempotência quando aplicável e fallback definido por caso de uso.

A escolha será feita antes da implementação executável de cada integração e antes de transmitir dados pessoais em ambiente real. A decisão comparará: operação e suporte no Brasil, custo e limites, SLA/continuidade, localização e retenção de dados, contrato/DPA, segurança/autenticação de webhooks, política de exportação/exclusão, consentimento e adequação ao fluxo sem cartão salvo. A escolha e os riscos residuais serão registrados em ADR próprio ou atualização deste ADR.

## Gates obrigatórios antes de produção

- E-mail/SMS: validar SPF/DKIM/DMARC e domínio remetente, configurar conta e sender ID Twilio, testar entregabilidade e custos, manter monitoramento de fila e separar marketing de transações; campanha só após consentimento.
- Pagamentos: checkout hospedado, confirmação reconciliada por webhook autenticado, sem vault/cartão reutilizável; jurídico-contábil valida termos de cobrança, tolerância e suspensão antes de cobrar.
- Mapas/geocoding: termos permitem a finalidade e retenção mínima; API server-side com domínio/IP de saída allow-listed; localização do navegador somente após ação do usuário e fallback manual.
- Storage/observabilidade: acesso mínimo, retenção configurada, redaction e procedimento de exportação/exclusão.

Nenhuma conta ou chave foi criada/configurada nesta decisão. Ambiente local usa dados sintéticos; o envio só inicia quando `OTP_DELIVERY_ENABLED=true` e todas as configurações obrigatórias existem.

## Consequências

A Fase 0 fica concluída quanto a premissas, arquitetura e riscos. A implementação de OTP tem providers definidos, mas a preparação para produção ainda depende de contas configuradas e validação de entrega. O item jurídico-contábil do ADR 0003 é gate para cobrança real; política de retenção pós-suspensão continua sendo decisão de produto antes de piloto.
