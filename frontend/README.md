# Peditto Web

Interface pública e fluxos de autenticação da Peditto, feitos com React, TypeScript e CSS modular.

## Estrutura

- `src/pages/LandingPage.tsx`: apresentação pública e conteúdo de produto.
- `src/components/landing/`: seções reutilizáveis da landing.
- `src/components/RegistrationFlow.tsx`: cadastro e validação de e-mail/celular usados no hero e na rota de cadastro.
- `src/pages/AuthPage.tsx`: área de entrada e login com verificação MFA.
- `src/components/storefront/StorefrontPage.tsx`: esqueleto compartilhado das três lojas de demonstração; não duplicar esse componente por tema.
- `src/components/storefront/`: estrutura e interações compartilhadas da vitrine, incluindo cardápio, adicionais, sacola e endereço.
- `src/data/demo-stores/<loja>.ts`: fixture de cada loja fictícia (identidade, textos, categorias e produtos).
- `src/data/demo-stores/themePreviews.ts`: configuração visual dos cards da galeria, separada do JSX.
- `src/data/demo-stores/index.ts` e `slugs.ts`: registro e allow-list das lojas de demonstração.
- `src/assets/demo-stores/<slug>/{brand,catalog}/`: imagens usadas pelo frontend em runtime.
- `src/services/authApi.ts`: chamadas HTTP de autenticação e CSRF.
- `src/styles/storefront.css`, `storefront-motion.css` e `storefront-responsive.css`: base compartilhada, movimento e responsividade da vitrine.
- `src/styles/storefront/themes/`: skins dos presets `original/`, `fast-food/` e `natural/`, escopadas às classes raiz de cada tema.
- `src/styles/`: tokens globais e estilos da landing, autenticação e vitrine.
- `public/favicon_io/`: favicons e manifesto da aplicação.

Referências de arte, logos-fonte e imagens originais que não entram no frontend ficam em `../docs/ux/demo-stores/`. Não guardar esse material em `src/` ou `public/`: Vite empacota imports de `src/` e copia o conteúdo de `public/` para `dist/`.

O formulário de cadastro do primeiro bloco e a rota `/criar-conta` usam o mesmo fluxo e os mesmos endpoints. A landing começa explicando o resultado para a loja e já permite iniciar o teste. `/entrar` mantém o login em duas etapas.

`/#temas` apresenta os três presets e cada card abre uma fixture em `/loja/<slug>`: Gelatto Totosu (`original`), Restaurante Dona Erza (`fast-food`) e Muru Muru Coffee (`natural`). `/loja/demo` é um alias legado para a Dona Erza. Produtos, preços, horários e mapa são dados ilustrativos. Busca, categorias, adicionais e sacola funcionam somente no navegador; localização e endereço não consultam a API, não confirmam cobertura e não criam pedidos. As rotas são marcadas como `noindex`; não aponte uma loja real para elas.

## Desenvolvimento

1. Inicie PostgreSQL e a API Spring Boot na porta `8080`.
2. Na pasta `frontend`, execute `npm install` e `npm run dev`.
3. O Vite encaminha `/api` para a API local. A landing pode ser visualizada sem a API; autenticação e cadastro precisam do backend.

Para abrir a galeria: `http://localhost:5173/#temas`. Para abrir uma loja: `/loja/gelatto-totosu`, `/loja/restaurante-dona-erza` ou `/loja/muru-muru-coffee`.

Segredos de SMTP, SMS e autenticação ficam no backend, fora do bundle e do armazenamento do navegador. O navegador recebe somente os dados necessários à sessão autenticada.
