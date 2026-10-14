# Peditto Web

Interface pública e fluxos de autenticação da Peditto, feitos com React, TypeScript e CSS modular.

## Estrutura

- `src/pages/LandingPage.tsx`: apresentação pública e conteúdo de produto.
- `src/components/landing/`: seções reutilizáveis da landing.
- `src/components/RegistrationFlow.tsx`: cadastro e validação de e-mail/celular usados no hero e na rota de cadastro.
- `src/pages/AuthPage.tsx`: área de entrada e login com verificação MFA.
- `src/services/authApi.ts`: chamadas HTTP de autenticação e CSRF.
- `src/styles/`: tokens, estilos da landing, motion, responsividade e autenticação.
- `public/favicon_io/`: favicons e manifesto da aplicação.

O formulário de cadastro do primeiro bloco e a rota `/criar-conta` usam o mesmo fluxo e os mesmos endpoints. A landing começa explicando o resultado para a loja e já permite iniciar o teste. `/entrar` mantém o login em duas etapas.

## Desenvolvimento

1. Inicie PostgreSQL e a API Spring Boot na porta `8080`.
2. Na pasta `frontend`, execute `npm install` e `npm run dev`.
3. O Vite encaminha `/api` para a API local. A landing pode ser visualizada sem a API; autenticação e cadastro precisam do backend.

Segredos de SMTP, SMS e autenticação ficam no backend, fora do bundle e do armazenamento do navegador. O navegador recebe somente os dados necessários à sessão autenticada.
