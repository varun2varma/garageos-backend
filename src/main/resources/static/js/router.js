window.Router = {

    currentPage: "dashboard",

    pages: {
        dashboard: () => Dashboard.render(),
        workflow: () => Workflow.render()
    },

    render(page) {

        this.currentPage = page;

        // Render Navbar
        document.getElementById("navbar").innerHTML =
            Navbar.render(
                page === "dashboard"
                    ? "Today's Garage"
                    : "New Vehicle Service"
            );

        // Render Page
        document.getElementById("page-container").innerHTML =
            this.pages[page]();

        // Bind page events
        switch (page) {

            case "dashboard":
                Dashboard.bindEvents();
                break;

            case "workflow":
                Workflow.bindEvents();
                break;

        }

        // Highlight sidebar menu
        this.highlightMenu(page);

    },

    navigate(page) {

        if (!this.pages[page]) {
            console.error("Unknown page : " + page);
            return;
        }

        this.render(page);

    },

    highlightMenu(page) {

        document
            .querySelectorAll(".menu-item")
            .forEach(item => item.classList.remove("active"));

        const active = document.querySelector(
            `.menu-item[data-page="${page}"]`
        );

        if (active) {
            active.classList.add("active");
        }

    }

};