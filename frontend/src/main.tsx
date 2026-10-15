import React from "react";
import ReactDOM from "react-dom/client";
import { lazy, Suspense } from "react";
import AuthPage from "./pages/AuthPage";
import LandingPage from "./pages/LandingPage";
import "./styles.css";

const StorefrontPage = lazy(() => import("./components/storefront/StorefrontPage"));
const pathname = window.location.pathname.replace(/\/$/, "") || "/";
const page = pathname === "/"
  ? <LandingPage />
  : pathname === "/loja/demo"
    ? <Suspense fallback={null}><StorefrontPage /></Suspense>
    : <AuthPage />;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>{page}</React.StrictMode>,
);
