# ADR 0005 — Console CEO da plataforma

- **Estado:** Aceita como requisito, implementar somente após MVP operacional e controles de plataforma.
- **Data:** 14/10/2026.

## Decisão de segurança

- Console em `platform.peditto.com`, contas de operador separadas de `AppUser/Membership` da loja e roles `PLATFORM_OWNER`, `PLATFORM_BILLING`, `PLATFORM_CAMPAIGNS`, `PLATFORM_ANALYTICS`, `PLATFORM_SUPPORT` com menor privilégio.
- MFA obrigatório, sessão host-only, step-up recente para ação financeira, campanha, export, suspensão ou acesso excepcional. Restrição de rede pode complementar, nunca substituir MFA.
- Toda ação tem ator, motivo, request id, before/after redigido e resultado em audit log append-only. Conta CEO não ganha autorização implícita para CRUD arbitrário de tenant.
- Agregados de performance por padrão. Acesso a detalhe privado de loja exige capacidade específica, finalidade, tempo limitado e audit; impersonation desativada no MVP.

## Preço e receita

- Planos e preços são versionados com vigência futura. Alterar preço mostra impacto/preview e exige step-up. Nunca modificar fatura emitida/paga retroativamente.
- Receita mostra MRR/faturas emitidas/recebidas/atrasadas/estornadas separadamente, reconcilia eventos de provider e separa receita Peditto de vendas/GMV da loja.
- Permissão de billing não permite ler senha, OTP, cartão, localização residencial ou dados privados desnecessários.

## Marketing

- Promoção por e-mail somente para quem deu opt-in de marketing separado, não pré-marcado; transactional e OTP não usam consentimento promocional.
- Guardar versão do consentimento, data/origem/canal e opt-out; lista suppression impede envio após descadastro. Nenhuma tela de CEO exibe/exporta lista irrestrita de emails.
- Campanhas são assíncronas, segmentação consentida, preview/audiência, e-mail de teste, limites, deduplicação, pausa/cancelamento, bounce/complaint e audit.
- Disparo em massa requer step-up, confirmação explícita e janela cancelável; aprovação por segunda pessoa será adicionada quando a equipe tiver outro operador com privilégio autorizado.

## Pendente

Domínio de envio/reputação, provedor de campanha, política de retenção de eventos, consentimento e revisão jurídica das mensagens/cadência ainda precisam de definição.