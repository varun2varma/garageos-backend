window.CustomerDashboard = {

    async init() {

        try {

            CustomerApp.showLoading();

            await Promise.all([

                this.loadProfile(),

                this.loadDashboard(),

                this.loadVehicles()

            ]);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    async loadProfile() {

        try {

            const customer =
                await CustomerPortalService.getProfile();

            if (!customer) {

                return;

            }

            if (CustomerRouter.currentPage !== "dashboard") {

                return;

            }

            const element =
                document.getElementById(
                    "dashboardCustomerName"
                );

            if (!element) {

                return;

            }

            element.textContent =
                `Welcome, ${customer.name}`;

        } catch (e) {

            console.error(e);

        }

    },

    async loadDashboard() {

        try {

            const dashboard =
                await CustomerPortalService.getDashboard();

            // User navigated to another page while API was loading
            if (CustomerRouter.currentPage !== "dashboard") {

                return;

            }

            const vehicleCount =
                document.getElementById("vehicleCount");

            const repairCount =
                document.getElementById("repairCount");

            const estimateCount =
                document.getElementById("estimateCount");

            const invoiceCount =
                document.getElementById("invoiceCount");

            const estimateCountFooter =
                document.getElementById("estimateCountFooter");

            const invoiceCountFooter =
                document.getElementById("invoiceCountFooter");

            if (vehicleCount) {

                vehicleCount.textContent =
                    dashboard.vehicleCount;

            }

            if (repairCount) {

                repairCount.textContent =
                    dashboard.activeJobCount;

            }

            if (estimateCount) {

                estimateCount.textContent =
                    dashboard.pendingEstimateCount;

            }

            if (invoiceCount) {

                invoiceCount.textContent =
                    dashboard.pendingInvoiceCount;

            }

            if (estimateCountFooter) {

                estimateCountFooter.textContent =
                    dashboard.pendingEstimateCount;

            }

            if (invoiceCountFooter) {

                invoiceCountFooter.textContent =
                    dashboard.pendingInvoiceCount;

            }

        } catch (e) {

            console.error(e);

        }

    },

    async loadVehicles() {

        try {

            const vehicles =
                await CustomerPortalService.getVehicles();

            this.renderVehicles(vehicles);

        } catch (e) {

            console.error(e);

        }

    },

    renderVehicles(vehicles) {

        const container =
            document.getElementById(
                "vehicleList"
            );

        if (!container) {

            return;

        }

        if (!vehicles.length) {

            container.innerHTML = `

                <div class="empty-state">

                    <i class="bi bi-car-front"></i>

                    <h5>No Vehicles</h5>

                    <p>No vehicles found.</p>

                </div>

            `;

            return;

        }

        container.innerHTML =

            vehicles

                .slice(0, 5)

                .map(vehicle => `

                    <div class="vehicle-item">

                        <div class="vehicle-icon">

                            <i class="bi bi-car-front-fill"></i>

                        </div>

                        <div class="vehicle-details">

                            <h6>

                                ${vehicle.registrationNumber}

                            </h6>

                            <small>

                                ${vehicle.brand}
                                ${vehicle.model}
                                ${vehicle.variant ?? ""}

                            </small>

                        </div>

                    </div>

                `)

                .join("");

    }

};