document.addEventListener("DOMContentLoaded", () => {

    Auth.requireLogin();

    window.CurrentUser = Auth.getUser();

    document.getElementById("sidebar").innerHTML =
        Sidebar.render();

    Sidebar.bindEvents();

    Router.render("dashboard");

});