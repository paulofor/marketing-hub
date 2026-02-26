const menuButton = document.querySelector(".menu");
const menuItems = document.getElementById("menu-items");

if (menuButton && menuItems) {
  menuButton.addEventListener("click", () => {
    const expanded = menuButton.getAttribute("aria-expanded") === "true";
    menuButton.setAttribute("aria-expanded", String(!expanded));
    menuItems.classList.toggle("open");
  });
}

const counters = document.querySelectorAll("[data-counter]");
if (counters.length) {
  const observer = new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        const el = entry.target;
        const target = Number(el.getAttribute("data-counter"));
        let start = 0;
        const increment = target / 60;
        const update = () => {
          start += increment;
          if (start >= target) {
            el.textContent = target.toLocaleString("pt-BR");
            return;
          }
          el.textContent = Math.round(start).toLocaleString("pt-BR");
          requestAnimationFrame(update);
        };
        update();
        obs.unobserve(el);
      });
    },
    { threshold: 0.4 }
  );

  counters.forEach((counter) => observer.observe(counter));
}

const year = document.getElementById("current-year");
if (year) {
  year.textContent = new Date().getFullYear();
}
