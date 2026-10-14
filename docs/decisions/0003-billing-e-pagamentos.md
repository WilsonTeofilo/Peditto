# ADR 0003 — Trial, cobrança e pagamentos

- **Estado:** Regras de produto decididas; provider e revisão contratual/contábil seguem pendentes antes da cobrança real.
- **Data:** 14/10/2026.

## Decisão de produto

- Trial SaaS de 30 dias corridos começa quando organização, owner e loja foram provisionados e os contatos do owner foram verificados. Um trial por documento; não reiniciar por cancelamento, reabertura, troca de e-mail ou suporte sem exceção auditada.
- A cobrança começa somente com plano/preço apresentado e aceito e uma fatura válida. Trial expirado sem conversão não é classificado como fatura inadimplente.
- Criar fatura no fim do trial convertido; primeira notificação em D-6. Ciclos seguintes são mensais e pagos manualmente. Sem débito automático, cartão salvo, cartão de uso futuro ou credencial de pagamento no banco Peditto.
- Após o vencimento da fatura, há 15 dias corridos de tolerância. Não cobrar multa, juros ou qualquer encargo por atraso. Ao fim da tolerância, suspender pedidos públicos e operações de escrita; manter painel em leitura, cobrança, segurança da própria conta, logout e exportação autorizada. A vitrine informa somente que está temporariamente fechada.
- Cancelamento encerra renovações futuras, sem multa de cancelamento. Se o período atual estiver pago, o acesso segue até seu término. Depois, a conta fica inativa por até 90 dias, com exportação disponível; então os dados operacionais são eliminados ou anonimizados, ressalvados registros sujeitos a retenção legal/contábil.
- A retenção de 90 dias também se aplica após suspensão por inadimplência. O prazo e a data prevista de eliminação devem ser comunicados ao responsável.
- Pagamento confirmado e reconciliado reativa a loja automaticamente. O valor da fatura não recebe acréscimo por atraso.
- Receita de SaaS significa pagamentos de faturas Peditto confirmados no provedor, líquidos de estorno/reembolso; não confundir com GMV/faturamento das lojas.
- Pedidos de loja aceitam Pix, cartão processado pelo provedor ou dinheiro somente na entrega. Sem armazenar PAN, CVV, PIN ou cartão salvo. Usar checkout hospedado/tokenização de uso imediato do provider.

## Exportação durante bloqueio ou retenção

- Exportar produtos, pedidos e vendas/financeiro sem nome, telefone, endereço, coordenadas, identificador do cliente ou observações livres que possam conter dados pessoais.
- Exigir permissão específica e autorização no backend, escopo tenant, reautenticação quando aplicável, link temporário e registro de auditoria.
- Importação, criação/edição de produtos, alterações de configuração e gestão de membros ficam bloqueadas durante suspensão.
- Definir transições permitidas para pedidos aceitos antes da suspensão, para concluir ou cancelar sem deixar pedidos órfãos.

## Controles técnicos

Assinatura e replay defense em webhook, idempotency key, reconciliação de evento antes de atualizar invoice, referência de provedor única e trilha de pagamento. Não existe job, tabela ou cálculo de encargo por atraso. O estado de acesso é derivado no backend do trial, da fatura e do calendário de tolerância; toda exceção administrativa é auditada. A tela do CEO não edita pagamentos confirmados nem exclui fatura histórica.

## Pendente para ativar cobrança

- Definir se boleto será suportado (não para MVP), calendário exato, emissão fiscal/recibo e texto contratual do período de tolerância sem encargos.
- Comparar gateways e confirmar se hosted checkout pode operar sem vault/recorrência e se aplica tokenização transitória compatível.
- Validar com jurídico/contabilidade os termos de suspensão, cancelamento, retenção de 90 dias, eliminação e registros que precisam de guarda obrigatória.
- Definir papéis e campos exatos da exportação sem dados pessoais, além do tratamento de pedidos já aceitos.