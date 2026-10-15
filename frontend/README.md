# Peditto Web

Interface pública e fluxos de autenticação da Peditto, feitos com React, TypeScript e CSS modular.

## Estrutura

- `src/pages/LandingPage.tsx`: apresentação pública e conteúdo de produto.
- `src/components/landing/`: seções reutilizáveis da landing.
- `src/components/RegistrationFlow.tsx`: cadastro e validação de e-mail/celular usados no hero e na rota de cadastro.
- `src/pages/AuthPage.tsx`: área de entrada e login com verificação MFA.
- `src/components/storefront/StorefrontPage.tsx`: vitrine pública de demonstração, separada da landing e da autenticação.
- `src/components/storefront/`: ilustrações e componentes da vitrine, incluindo cardápio, adicionais, sacola e endereço.
- `src/data/demoStore.ts`: conteúdo local e fictício usado para compor a prévia da loja.
- `src/services/authApi.ts`: chamadas HTTP de autenticação e CSRF.
- `src/styles/`: tokens e estilos separados por página, com motion e responsividade próprios.
- `public/favicon_io/`: favicons e manifesto da aplicação.

O formulário de cadastro do primeiro bloco e a rota `/criar-conta` usam o mesmo fluxo e os mesmos endpoints. A landing começa explicando o resultado para a loja e já permite iniciar o teste. `/entrar` mantém o login em duas etapas.

`/loja/demo` apresenta uma vitrine fictícia para avaliar a experiência de compra. Os produtos, preços, horários e mapa são dados ilustrativos. Busca, categorias, adicionais e sacola funcionam somente no navegador; localização e endereço não consultam a API, não confirmam cobertura e não criam pedidos. A rota é marcada como `noindex` enquanto usar conteúdo de demonstração. Não aponte uma loja real para esse caminho.

## Desenvolvimento

1. Inicie PostgreSQL e a API Spring Boot na porta `8080`.
2. Na pasta `frontend`, execute `npm install` e `npm run dev`.
3. O Vite encaminha `/api` para a API local. A landing pode ser visualizada sem a API; autenticação e cadastro precisam do backend.

Para abrir a vitrine visual: `http://localhost:5173/loja/demo`.

Segredos de SMTP, SMS e autenticação ficam no backend, fora do bundle e do armazenamento do navegador. O navegador recebe somente os dados necessários à sessão autenticada.
