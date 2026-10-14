# Peditto Web

Frontend React + TypeScript da Peditto. A página pública apresenta o serviço, a mascote oficial e o CTA; o fluxo de cadastro e autenticação em duas etapas conecta aos contratos da API Spring.

## Executar

1. Inicie PostgreSQL e backend na porta 8080.
2. Configure SMTP/Twilio e habilite OTP_DELIVERY_ENABLED=true no ambiente protegido do backend.
3. Na pasta frontend, execute npm install e npm run dev.

O Vite encaminha /api para o backend local. Em produção, sirva frontend e API pela mesma origem. Senhas, códigos OTP e token de pré-autenticação não são persistidos no navegador.

## Primeiro recorte

Landing page com pitch, avatar/favicons da mascote e metadados básicos. Cadastro por CPF/CNPJ, confirmação de e-mail e celular, login com escolha de canal MFA e sessão em cookie seguro. Painel, catálogo, pedidos e checkout serão construídos depois. O site ainda não tem domínio/canonical, sitemap, prerender ou structured data; essas tarefas estão na Fase 13 do roteiro.
