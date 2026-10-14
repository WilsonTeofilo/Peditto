import { useEffect } from "react";
import LandingHeader from "../components/landing/LandingHeader";
import TickerRibbon from "../components/landing/TickerRibbon";
import HeroSection from "../components/landing/HeroSection";
import PainSection from "../components/landing/PainSection";
import OrderFlowSection from "../components/landing/OrderFlowSection";
import CalcSection from "../components/landing/CalcSection";
import CompareSection from "../components/landing/CompareSection";
import DeliveryAreaSection from "../components/landing/DeliveryAreaSection";
import FaqSection from "../components/landing/FaqSection";
import ClosingSection from "../components/landing/ClosingSection";
import LandingFooter from "../components/landing/LandingFooter";

const pageTitle = "Cardápio digital e pedidos para delivery | Peditto";
const pageDescription = "Crie o cardápio online da sua loja, organize os pedidos e defina sua área de entrega. Conheça o Peditto com 30 dias de teste.";

function LandingPage() {
  useEffect(() => {
    document.title = pageTitle;
    const description = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    if (description) description.content = pageDescription;

    const targets = document.querySelectorAll<HTMLElement>("[data-reveal]");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (!("IntersectionObserver" in window) || reducedMotion) {
      targets.forEach((target) => target.classList.add("is-visible"));
      return;
    }

    document.documentElement.classList.add("motion-enabled");
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        });
      },
      { rootMargin: "0px 0px -40px 0px", threshold: 0.08 }
    );

    targets.forEach((target) => observer.observe(target));
    return () => {
      observer.disconnect();
      document.documentElement.classList.remove("motion-enabled");
    };
  }, []);

  return (
    <div className="landing-page">
      <LandingHeader />
      <main id="conteudo">
        <HeroSection />
        <TickerRibbon />
        <PainSection />
        <OrderFlowSection />
        <DeliveryAreaSection />
        <CalcSection />
        <CompareSection />
        <FaqSection />
        <ClosingSection />
      </main>
      <LandingFooter />
    </div>
  );
}

export default LandingPage;
