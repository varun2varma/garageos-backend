window.CustomerRouter = {

    routes: {

        dashboard: {
            title: "Dashboard",
            subtitle: "Welcome to your GarageOS customer portal.",
            page: "dashboard"
        },

        vehicles: {
            title: "My Vehicles",
            subtitle: "View and manage your registered vehicles.",
            page: "vehicle"
        },

        repair: {
            title: "Repair Workflow",
            subtitle: "Track your vehicle repair status in real time.",
            page: "repair"
        },

        estimates: {
            title: "Estimate Approval",
            subtitle: "Review and approve service estimates.",
            page: "estimate"
        },

        invoices: {
            title: "Invoices",
            subtitle: "Download invoices and payment receipts.",
            page: "invoice"
        },

        profile: {
            title: "Profile",
            subtitle: "Manage your account information.",
            page: "profile"
        }

    },

    currentPage: "dashboard",

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

        const route = this.routes[page];

        if (!route) {

            return;

        }

        CustomerSidebar.render(page);

        document.getElementById("pageTitle").textContent =
            route.title;

        document.getElementById("pageSubTitle").textContent =
            route.subtitle;

        CustomerApp.showLoading();

        try {

            const response =
                await fetch(
                    `pages/${route.page}.html`
                );

            const html =
                await response.text();

            document.getElementById(
                "pageContainer"
            ).innerHTML = html;

            await this.loadScript(route.page);

        } catch (e) {

            console.error(e);

            document.getElementById(
                "pageContainer"
            ).innerHTML = `

                <div class="customer-card">

                    <h4>

                        Unable to load page

                    </h4>

                    <p>

                        ${e.message}

                    </p>

                </div>

            `;

        } finally {

            CustomerApp.hideLoading();

        }

    },

    async loadScript(page) {

        return new Promise((resolve, reject) => {

            const oldScript =
                document.getElementById(
                    "customerPageScript"
                );

            if (oldScript) {

                oldScript.remove();

            }

            const script =
                document.createElement("script");

            script.id =
                "customerPageScript";

            script.src =
                `js/${page}.js?v=${Date.now()}`;

            script.onload = () => {

                const objectName =
                    this.getModuleName(page);

                const module =
                    window[objectName];

                if (
                    module &&
                    typeof module.init === "function"
                ) {

                    module.init();

                }

                resolve();

            };

            script.onerror = reject;

            document.body.appendChild(script);

        });

    },

    getModuleName(page) {

        const names = {

            dashboard: "CustomerDashboard",

            vehicles: "CustomerVehicle",

            repair: "CustomerRepair",

            estimates: "CustomerEstimate",

            invoices: "CustomerInvoice",

            profile: "CustomerProfile"

        };

        return names[page];

    },

    initialize() {

        window.onpopstate = async (event) => {

            const page =
                event.state?.page ||
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

            this.load(hash);

        } else {

            this.load("dashboard");

        }

    }

};