import React from "react";
import ReactDOM from "react-dom/client";
import { lazy, Suspense } from "react";
import AuthPage from "./pages/AuthPage";
import LandingPage from "./pages/LandingPage";
import { demoStoreSlugs } from "./data/demo-stores/slugs";
import "./styles.css";

const StorefrontPage = lazy(() => import("./components/storefront/StorefrontPage"));
const pathname = window.location.pathname.replace(/\/$/, "") || "/";
const legacyDemoSlug = pathname === "/loja/demo" ? "restaurante-dona-erza" : undefined;
const storeSlug = pathname.match(/^\/loja\/([^/]+)$/)?.[1] ?? legacyDemoSlug;
const isStorefrontRoute = storeSlug !== undefined && demoStoreSlugs.some((slug) => slug === storeSlug);
const page = pathname === "/"
  ? <LandingPage />
  : isStorefrontRoute && storeSlug
    ? <Suspense fallback={null}><StorefrontPage storeSlug={storeSlug} /></Suspense>
    : <AuthPage />;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>{page}</React.StrictMode>,
);
