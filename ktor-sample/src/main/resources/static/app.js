async function scanLivres() {
    const input = document.getElementById("scan-input").value;
    const titres = input.split(",").map(t => t.trim());

    const response = await fetch("/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ livres_detectes: titres })
    });

    const result = await response.json();
    const list = document.getElementById("resultats");
    list.innerHTML = "";
    result.livres_trouves.forEach(livre => {
        const item = document.createElement("li");
        item.textContent = `${livre.titre} — ${livre.auteur}`;
        list.appendChild(item);
    });
}
