import { useEffect, useMemo, useRef, useState, type CSSProperties, type FormEvent, type RefObject } from "react";
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  Check,
  ChefHat,
  ChevronDown,
  CircleHelp,
  Clock3,
  MapPin,
  Minus,
  Plus,
  Search,
  ShoppingBag,
  X,
} from "lucide-react";
import { categories, demoStore, products, type StoreProduct } from "../../data/demoStore";
import "../../styles/storefront.css";
import "../../styles/storefront-motion.css";
import "../../styles/storefront-responsive.css";

type CartLine = {
  key: string;
  product: StoreProduct;
  quantity: number;
  addonIds: string[];
};

type PreviewAddress = {
  cep: string;
  street: string;
  number: string;
  neighborhood: string;
  complement: string;
};

type ModalState = "cart" | "address" | null;

const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

function useDialogFocus(ref: RefObject<HTMLElement | null>, onClose: () => void) {
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;

    const previousFocus = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closeRef.current();
        return;
      }
      if (event.key !== "Tab") return;

      const focusable = [...dialog!.querySelectorAll<HTMLElement>(
        'button:not(:disabled), input:not(:disabled), [href], [tabindex]:not([tabindex="-1"])',
      )];
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    dialog.addEventListener("keydown", handleKeyDown);
    dialog.querySelector<HTMLElement>("button, input")?.focus();
    return () => {
      dialog.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus();
    };
  }, [ref]);
}

function useRevealCards(content: unknown) {
  useEffect(() => {
    const items = document.querySelectorAll<HTMLElement>(".storefront [data-reveal]");
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || !("IntersectionObserver" in window)) {
      items.forEach((item) => item.classList.add("is-visible"));
      return;
    }

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      });
    }, { rootMargin: "0px 0px -32px 0px", threshold: 0.12 });

    items.forEach((item) => observer.observe(item));
    return () => observer.disconnect();
  }, [content]);
}

function useMotionVisibility() {
  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || !("IntersectionObserver" in window)) return;
    const targets = document.querySelectorAll<HTMLElement>(".storefront__motion-target");
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => entry.target.classList.toggle("is-in-view", entry.isIntersecting));
    }, { rootMargin: "80px 0px", threshold: 0.01 });
    targets.forEach((target) => observer.observe(target));
    return () => observer.disconnect();
  }, []);
}

function StorefrontPage() {
  const [activeCategory, setActiveCategory] = useState("all");
  const [query, setQuery] = useState("");
  const [selectedProduct, setSelectedProduct] = useState<StoreProduct | null>(null);
  const [modal, setModal] = useState<ModalState>(null);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [address, setAddress] = useState<PreviewAddress>({ cep: "", street: "", number: "", neighborhood: "", complement: "" });
  const [locationMessage, setLocationMessage] = useState("");
  const [isLocating, setIsLocating] = useState(false);

  useEffect(() => {
    const previousTitle = document.title;
    const description = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    const previousDescription = description?.content;
    const robots = document.querySelector<HTMLMetaElement>('meta[name="robots"]');
    const previousRobots = robots?.content;
    const canonical = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    const previousCanonical = canonical?.href;
    document.title = `${demoStore.name} | Cardápio no Peditto`;
    if (description) description.content = `Cardápio de demonstração do ${demoStore.name} no Peditto.`;
    if (robots) robots.content = "noindex, nofollow";
    canonical?.removeAttribute("href");
    return () => {
      document.title = previousTitle;
      if (description && previousDescription) description.content = previousDescription;
      if (robots && previousRobots) robots.content = previousRobots;
      if (canonical && previousCanonical) canonical.href = previousCanonical;
    };
  }, []);

  const visibleProducts = useMemo(() => {
    const term = query.trim().toLocaleLowerCase("pt-BR");
    return products.filter((product) => {
      const matchesCategory = activeCategory === "all" || product.categoryId === activeCategory;
      const matchesTerm = !term || `${product.name} ${product.description}`.toLocaleLowerCase("pt-BR").includes(term);
      return matchesCategory && matchesTerm;
    });
  }, [activeCategory, query]);
  const highlightedProduct = products.find((product) => product.id === demoStore.highlight.productId);
  const bestSellerProduct = products.find((product) => product.id === demoStore.bestSellerProductId);

  useRevealCards(visibleProducts);
  useMotionVisibility();

  const lineCount = cart.reduce((total, line) => total + line.quantity, 0);
  const addressLabel = address.street
    ? `${address.street}, ${address.number}${address.complement ? ` · ${address.complement}` : ""} · ${address.neighborhood}`
    : "";
  const cartTotal = cart.reduce((total, line) => {
    const extras = line.product.addons
      .filter((addon) => line.addonIds.includes(addon.id))
      .reduce((sum, addon) => sum + addon.price, 0);
    return total + (line.product.price + extras) * line.quantity;
  }, 0);

  function addToCart(product: StoreProduct, quantity: number, addonIds: string[] = []) {
    const normalizedAddons = [...addonIds].sort();
    const key = `${product.id}:${normalizedAddons.join(",")}`;
    setCart((current) => {
      const existing = current.find((line) => line.key === key);
      if (existing) return current.map((line) => line.key === key ? { ...line, quantity: line.quantity + quantity } : line);
      return [...current, { key, product, quantity, addonIds: normalizedAddons }];
    });
    setSelectedProduct(null);
  }

  function changeLineQuantity(key: string, change: number) {
    setCart((current) => current
      .map((line) => line.key === key ? { ...line, quantity: line.quantity + change } : line)
      .filter((line) => line.quantity > 0));
  }

  function requestLocation() {
    if (!navigator.geolocation) {
      setLocationMessage("Seu navegador não oferece localização. Digite o CEP para continuar.");
      return;
    }
    setIsLocating(true);
    setLocationMessage("");
    navigator.geolocation.getCurrentPosition(
      () => {
        setIsLocating(false);
        setLocationMessage("Localização recebida nesta prévia. A disponibilidade real é confirmada pela loja no servidor.");
      },
      () => {
        setIsLocating(false);
        setLocationMessage("Não conseguimos acessar sua localização. Você pode informar o CEP manualmente.");
      },
      { enableHighAccuracy: false, timeout: 9000, maximumAge: 60_000 },
    );
  }

  function saveAddress(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const street = String(form.get("street") ?? "").trim();
    const number = String(form.get("number") ?? "").trim();
    const neighborhood = String(form.get("neighborhood") ?? "").trim();
    if (!street || !number || !neighborhood) return;
    setAddress({
      cep: String(form.get("cep") ?? "").trim(),
      street,
      number,
      neighborhood,
      complement: String(form.get("complement") ?? "").trim(),
    });
    setLocationMessage("Endereço salvo só para esta prévia. A cobertura precisa ser confirmada pela loja no servidor.");
    setModal(null);
  }

  return (
    <main className="storefront">
      <div className="storefront__topline">
        <span><span className="storefront__status-dot" /> Cozinha aberta para pedidos</span>
        <span className="storefront__demo-label">Vitrine de demonstração</span>
      </div>

      <header className="store-header">
        <a className="store-header__brand" href="#inicio" aria-label={`${demoStore.name}, início`}>
          {demoStore.logoUrl
            ? <img className="store-header__logo" src={demoStore.logoUrl} alt={`Logo da ${demoStore.name}`} width="48" height="48" decoding="async" />
            : <span className="store-header__mark"><ChefHat size={23} strokeWidth={1.8} /></span>}
          <span><strong>{demoStore.name}</strong><small>{demoStore.descriptor}</small></span>
        </a>
        <div className="store-header__hours"><Clock3 size={15} /><span>Hoje, até 22h</span><ChevronDown size={14} /></div>
        <button className="store-header__cart" type="button" onClick={() => setModal("cart")} aria-label={`Abrir sacola, ${lineCount} itens`}>
          <ShoppingBag size={18} /><span>Sacola</span>{lineCount > 0 && <b>{lineCount}</b>}
        </button>
      </header>

      <section className="store-hero" id="inicio" aria-labelledby="store-title">
        <div className="store-hero__copy">
          <span className="eyebrow"><span /> {demoStore.introduction.eyebrow}</span>
          <h1 id="store-title">{demoStore.introduction.titleLineOne}<br /><em>{demoStore.introduction.titleLineTwo}</em></h1>
          <p>{demoStore.introduction.description}</p>
          <div className="store-hero__meta">
            <span><Clock3 size={16} /> 30–45 min</span>
            <span><MapPin size={16} /> {demoStore.neighborhood}</span>
          </div>
          <button className="button button--primary store-hero__cta" type="button" onClick={() => document.querySelector("#cardapio")?.scrollIntoView({ behavior: "smooth" })}>
            Ver o cardápio <ArrowDownRight size={17} />
          </button>
        </div>
        <div className="store-hero__art storefront__motion-target" aria-hidden="true">
          <div className="store-hero__sun" />
          {bestSellerProduct && <figure className="store-hero__photo-frame"><img className="store-hero__food" src={bestSellerProduct.image} alt={bestSellerProduct.imageAlt} width="800" height="800" fetchPriority="high" decoding="async" /><figcaption className="store-hero__photo-label">Mais pedido</figcaption></figure>}
          
        </div>
        <div className="store-hero__bottomline"><span>{demoStore.descriptor}</span><span>desde {demoStore.foundedYear}</span><span>{demoStore.neighborhood}</span></div>
      </section>

      <section className="store-promo" aria-label="Destaque da casa" data-reveal>
        <div className="store-promo__copy"><span>{demoStore.highlight.eyebrow}</span><h2>{demoStore.highlight.title}</h2><p>{demoStore.highlight.description}</p><button type="button" onClick={() => highlightedProduct && setSelectedProduct(highlightedProduct)}>{demoStore.highlight.actionLabel} <ArrowRight size={15} /></button></div>
        {highlightedProduct && <img className="store-promo__art" src={highlightedProduct.image} alt={highlightedProduct.imageAlt} width="672" height="1200" loading="lazy" decoding="async" />}
        <span className="store-promo__seal">feito<br />com<br />calma</span>
      </section>

      <section className="menu-section" id="cardapio" aria-labelledby="menu-title">
        <div className="menu-section__heading" data-reveal>
          <div><span className="eyebrow"><span /> {demoStore.name.toLocaleUpperCase("pt-BR")}</span><h2 id="menu-title">Escolha com calma.</h2></div>
          <label className="store-search"><Search size={18} /><span className="sr-only">Buscar no cardápio</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="O que deu vontade?" /></label>
        </div>

        <div className="category-rail" role="group" aria-label="Categorias do cardápio">
          {categories.map((category) => (
            <button key={category.id} className={`category-chip ${activeCategory === category.id ? "is-active" : ""}`} type="button" aria-pressed={activeCategory === category.id} onClick={() => setActiveCategory(category.id)}>
              <span className="category-chip__icon" aria-hidden="true">{category.icon}</span>{category.label}
            </button>
          ))}
        </div>

        <div className="product-grid" aria-live="polite">
          {visibleProducts.map((product, index) => (
            <article className="product-card" key={product.id} data-reveal style={{ "--card-order": index } as CSSProperties}>
              <button className="product-card__open" type="button" onClick={() => setSelectedProduct(product)} aria-label={`Ver detalhes de ${product.name}`}>
                <div className="product-card__art-wrap">
                  {product.badge && <span className="product-card__badge">{product.badge}</span>}
                  <img className="product-card__photo" src={product.image} alt={product.imageAlt} width="800" height="1200" loading="lazy" decoding="async" />
                  <span className="product-card__quick-add" aria-hidden="true"><Plus size={18} /></span>
                </div>
                <div className="product-card__body"><span className="product-card__category">{categories.find((category) => category.id === product.categoryId)?.label}</span><h3>{product.name}</h3><p>{product.description}</p><strong>{money.format(product.price)}</strong></div>
              </button>
            </article>
          ))}
          {visibleProducts.length === 0 && <div className="menu-empty"><CircleHelp size={22} /><strong>Não achamos esse prato.</strong><span>Tente outra busca ou escolha uma categoria.</span></div>}
        </div>
      </section>

      <section className="delivery-section" aria-labelledby="delivery-title" data-reveal>
        <div className="delivery-section__copy">
          <span className="eyebrow"><span /> ENTREGA DA CASA</span>
          <h2 id="delivery-title">Até onde a Dona Erza entrega?</h2>
          <p>Veja a área aproximada no mapa. A confirmação do endereço e da entrega acontece no servidor antes do pedido.</p>
          <div className="delivery-section__address">
            <MapPin size={18} /><span>{addressLabel || "Informe seu endereço para consultar"}</span>
            <button type="button" onClick={() => setModal("address")}>{addressLabel ? "Editar" : "Informar"}</button>
          </div>
          <button className="delivery-section__location" type="button" onClick={requestLocation} disabled={isLocating}>
            <MapPin size={16} />{isLocating ? "Localizando…" : "Usar minha localização"}<ArrowUpRight size={15} />
          </button>
          {locationMessage && <p className="delivery-section__notice" role="status">{locationMessage}</p>}
        </div>
        <div className="delivery-map storefront__motion-target" role="img" aria-label="Ilustração de uma área aproximada de entrega ao redor da loja">
          <div className="delivery-map__grid" />
          <div className="delivery-map__road delivery-map__road--one" /><div className="delivery-map__road delivery-map__road--two" /><div className="delivery-map__road delivery-map__road--three" />
          <div className="delivery-map__zone"><span>área aproximada<br />de entrega</span></div>
          <div className="delivery-map__store"><ChefHat size={17} /></div><span className="delivery-map__you"><MapPin size={18} /> você</span>
          <span className="delivery-map__caption">representação ilustrativa · não calcula cobertura</span>
        </div>
      </section>

      <footer className="store-footer"><a href="#inicio" className="store-footer__brand"><span className="store-header__mark"><ChefHat size={19} /></span>{demoStore.name}</a><span>Comida feita por gente, pedida pelo Peditto.</span><a href="#cardapio">Voltar ao cardápio ↑</a></footer>

      {lineCount > 0 && <button className="cart-dock" type="button" onClick={() => setModal("cart")}><span><ShoppingBag size={18} /><b>{lineCount}</b></span><span>Ver sacola</span><strong>{money.format(cartTotal)}</strong></button>}

      {selectedProduct && <ProductDialog product={selectedProduct} onClose={() => setSelectedProduct(null)} onAdd={addToCart} />}
      {modal === "cart" && <CartDrawer lines={cart} total={cartTotal} onClose={() => setModal(null)} onChange={changeLineQuantity} onAddress={() => setModal("address")} addressLabel={addressLabel} />}
      {modal === "address" && <AddressDialog onClose={() => setModal(null)} onSave={saveAddress} initialAddress={address} />}
    </main>
  );
}

type ProductDialogProps = {
  product: StoreProduct;
  onClose: () => void;
  onAdd: (product: StoreProduct, quantity: number, addonIds: string[]) => void;
};

function ProductDialog({ product, onClose, onAdd }: ProductDialogProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [addonIds, setAddonIds] = useState<string[]>([]);
  const extras = product.addons.filter((addon) => addonIds.includes(addon.id)).reduce((sum, addon) => sum + addon.price, 0);
  useDialogFocus(dialogRef, onClose);

  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section ref={dialogRef} className="product-dialog" role="dialog" aria-modal="true" aria-labelledby="product-dialog-title" tabIndex={-1}>
      <button className="icon-button product-dialog__close" type="button" onClick={onClose} aria-label="Fechar"><X size={20} /></button>
      <div className="product-dialog__art"><img src={product.image} alt={product.imageAlt} width="800" height="1200" decoding="async" /></div>
      <div className="product-dialog__content"><span className="eyebrow"><span /> FEITO NA COZINHA DA DONA ERZA</span><h2 id="product-dialog-title">{product.name}</h2><p>{product.description}</p>
        {product.addons.length > 0 && <fieldset className="addon-list"><legend>Quer completar?</legend>{product.addons.map((addon) => <label key={addon.id}><input type="checkbox" checked={addonIds.includes(addon.id)} onChange={(event) => setAddonIds((current) => event.target.checked ? [...current, addon.id] : current.filter((id) => id !== addon.id))} /><span>{addon.name}</span><b>+ {money.format(addon.price)}</b></label>)}</fieldset>}
        <div className="product-dialog__actions"><div className="quantity-stepper" aria-label="Quantidade"><button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} aria-label="Diminuir"><Minus size={15} /></button><span>{quantity}</span><button type="button" onClick={() => setQuantity((value) => value + 1)} aria-label="Aumentar"><Plus size={15} /></button></div><button className="button button--primary" type="button" onClick={() => onAdd(product, quantity, addonIds)}>Adicionar · {money.format((product.price + extras) * quantity)}</button></div>
      </div>
    </section>
  </div>;
}

type CartDrawerProps = {
  lines: CartLine[];
  total: number;
  addressLabel: string;
  onClose: () => void;
  onChange: (key: string, change: number) => void;
  onAddress: () => void;
};

function CartDrawer({ lines, total, addressLabel, onClose, onChange, onAddress }: CartDrawerProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  useDialogFocus(dialogRef, onClose);

  return <div className="modal-backdrop modal-backdrop--drawer" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <aside ref={dialogRef} className="cart-drawer" role="dialog" aria-modal="true" aria-labelledby="cart-title" tabIndex={-1}>
      <header className="cart-drawer__header"><div><span className="eyebrow"><span /> UM PEDIDO GOSTOSO</span><h2 id="cart-title">Sua sacola</h2></div><button className="icon-button" type="button" onClick={onClose} aria-label="Fechar sacola"><X size={20} /></button></header>
      {lines.length === 0 ? <div className="cart-empty"><ShoppingBag size={30} /><strong>Sua sacola está esperando.</strong><span>Escolha alguma coisa boa do cardápio.</span></div> : <>
        <div className="cart-drawer__lines">{lines.map((line) => {
          const extras = line.product.addons.filter((addon) => line.addonIds.includes(addon.id));
          const unitPrice = line.product.price + extras.reduce((sum, addon) => sum + addon.price, 0);
          return <article className="cart-line" key={line.key}><div className="cart-line__art"><img src={line.product.image} alt={line.product.imageAlt} width="800" height="1200" loading="lazy" decoding="async" /></div><div className="cart-line__main"><h3>{line.product.name}</h3>{extras.map((addon) => <span key={addon.id}>{addon.name}</span>)}<strong>{money.format(unitPrice * line.quantity)}</strong><div className="quantity-stepper quantity-stepper--small"><button type="button" onClick={() => onChange(line.key, -1)} aria-label={`Remover uma unidade de ${line.product.name}`}><Minus size={13} /></button><span>{line.quantity}</span><button type="button" onClick={() => onChange(line.key, 1)} aria-label={`Adicionar uma unidade de ${line.product.name}`}><Plus size={13} /></button></div></div></article>;
        })}</div>
        <div className="cart-drawer__bottom"><button className="cart-address" type="button" onClick={onAddress}><MapPin size={17} /><span><small>ENTREGAR EM</small><strong>{addressLabel || "Escolha seu endereço"}</strong></span><ArrowRight size={16} /></button><div className="cart-total"><span>Total dos itens</span><strong>{money.format(total)}</strong></div><button className="button button--primary cart-drawer__continue" type="button" onClick={onAddress}>Continuar com endereço <ArrowRight size={17} /></button><p>Esta é uma prévia. A loja confirma entrega, disponibilidade e valores pelo servidor antes do pedido.</p></div>
      </>}
    </aside>
  </div>;
}

type AddressDialogProps = { onClose: () => void; onSave: (event: FormEvent<HTMLFormElement>) => void; initialAddress: PreviewAddress };

function AddressDialog({ onClose, onSave, initialAddress }: AddressDialogProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  useDialogFocus(dialogRef, onClose);

  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section ref={dialogRef} className="address-dialog" role="dialog" aria-modal="true" aria-labelledby="address-title" tabIndex={-1}>
      <header className="address-dialog__header"><div><span className="eyebrow"><span /> ENTREGA</span><h2 id="address-title">Onde você está?</h2></div><button className="icon-button" type="button" onClick={onClose} aria-label="Fechar"><X size={20} /></button></header>
      <p>Use a localização do aparelho ou informe o endereço. A cobertura será confirmada pela loja antes do pedido.</p>
      <form onSubmit={onSave}>
        <label className="field-label">CEP<input name="cep" inputMode="numeric" autoComplete="postal-code" placeholder="00000-000" maxLength={9} defaultValue={initialAddress.cep} /></label>
        <div className="address-dialog__row"><label className="field-label">Rua ou avenida<input name="street" required autoComplete="address-line1" defaultValue={initialAddress.street} placeholder="Rua das Flores" /></label><label className="field-label address-dialog__number">Número<input name="number" required inputMode="numeric" defaultValue={initialAddress.number} placeholder="123" /></label></div>
        <label className="field-label">Bairro<input name="neighborhood" required autoComplete="address-level3" defaultValue={initialAddress.neighborhood} placeholder="Seu bairro" /></label>
        <label className="address-dialog__complement">Complemento (opcional)<input name="complement" autoComplete="address-line2" defaultValue={initialAddress.complement} placeholder="Apto, bloco, referência" /></label>
        <button className="button button--primary address-dialog__save" type="submit"><Check size={17} /> Salvar endereço nesta prévia</button>
      </form>
    </section>
  </div>;
}

export default StorefrontPage;
