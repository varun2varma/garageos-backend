document.addEventListener("DOMContentLoaded", () => {

    document.getElementById("sidebar").innerHTML =
        Sidebar.render();

    Sidebar.bindEvents();

    Router.render("dashboard");

});