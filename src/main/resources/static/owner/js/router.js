window.OwnerRouter = {

    routes: {

        dashboard: {

            title: "Dashboard",

            subtitle: "Garage Overview",

            page: "dashboard"

        },

        employees: {

            title: "Employees",

            subtitle: "Manage your garage employees.",

            page: "employees"

        }

    },

    currentPage: "dashboard",

    async initialize() {

        window.onpopstate = async (event) => {

            const page =

                event.state?.page ??

                "dashboard";

            await this.load(page);

        };

        const hash =

            window.location.hash

                .replace("#", "");

        if (

            hash &&

            this.routes[hash]

        ) {

            await this.navigate(hash);

        } else {

            await this.navigate("dashboard");

        }

    },

    async navigate(page) {

        if (!this.routes[page]) {

            page = "dashboard";

        }

        this.currentPage = page;

        history.pushState(

            { page },

            "",

            "#" + page

        );

        await this.load(page);

    },

    async load(page) {

        const route =

            this.routes[page];

        OwnerSidebar.render(page);

        document

            .getElementById("pageTitle")

            .textContent =

            route.title;

        document

            .getElementById("pageSubTitle")

            .textContent =

            route.subtitle;

        OwnerApp.showLoading();

        try {

            const response =

                await fetch(

                    `pages/${route.page}.html`

                );

            const html =

                await response.text();

            document

                .getElementById("pageContainer")

                .innerHTML = html;

            await this.loadScript(route.page);

        }

        finally {

            OwnerApp.hideLoading();

        }

    },

    async loadScript(page) {

        return new Promise((resolve, reject) => {

            const oldScript =
                document.getElementById("ownerPageScript");

            if (oldScript) {

                oldScript.remove();

            }

            const script =
                document.createElement("script");

            script.id =
                "ownerPageScript";

            script.src =
                `js/${page}.js?v=${Date.now()}`;

            script.onload = async () => {

                const module =
                    window[this.getModuleName(page)];

                if (module) {

                    await module.init();

                }

                resolve();

            };

            script.onerror = reject;

            document.body.appendChild(script);

        });

    },

    getModuleName(page) {

        const names = {

            dashboard: "OwnerDashboard",

            employees: "OwnerEmployees"

        };

        return names[page];

    }

};