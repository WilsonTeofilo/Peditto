import React from "react";
import ReactDOM from "react-dom/client";
import AuthPage from "./pages/AuthPage";
import LandingPage from "./pages/LandingPage";
import "./styles.css";
const page = window.location.pathname === "/" ? <LandingPage /> : <AuthPage />;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>{page}</React.StrictMode>,
);
