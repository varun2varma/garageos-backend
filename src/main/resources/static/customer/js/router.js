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

        estimate: {
            title: "Estimate Approval",
            subtitle: "Review and approve service estimates.",
            page: "estimate"
        },

        invoice: {
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

        console.trace("NAVIGATE ->", page);

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

        console.log("SIDEBAR ACTIVE PAGE =", page);

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

            script.onload = async () => {

                try {

                    console.log("ROUTER 1", page);

                        const objectName = this.getModuleName(page);

                        console.log("ROUTER 2", objectName);

                        const module = window[objectName];

                        console.log("ROUTER 3", module);

                        if (module) {

                            console.log("ROUTER 4");

                            await module.init();

                            console.log("ROUTER 5");

                        }

                    resolve();

                } catch (e) {

                    reject(e);

                }

            };

            script.onerror = reject;

            document.body.appendChild(script);

        });

    },

    getModuleName(page) {

        const names = {

            dashboard: "CustomerDashboard",

            vehicle: "CustomerVehicle",

            repair: "CustomerRepair",

            estimate: "CustomerEstimate",

            invoice: "CustomerInvoice",

            profile: "CustomerProfile"

        };

        return names[page];

    },

    async initialize() {

        window.onpopstate = async (event) => {

            console.log("POPSTATE EVENT", event);
            console.log("POPSTATE STATE", event.state);

            const page =
                event.state?.page ??
                "dashboard";

            console.log("POPSTATE PAGE", page);

            await this.load(page);

        };

        const hash =
            window.location.hash.replace("#", "");

        if (
            hash &&
            this.routes[hash]
        ) {

            await this.navigate(hash);

        }
        else {

            await this.navigate("dashboard");

        }

    }

};