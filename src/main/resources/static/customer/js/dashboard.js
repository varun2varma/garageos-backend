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

            document.getElementById(
                "dashboardCustomerName"
            ).textContent =
                `Welcome, ${customer.name}`;

        } catch (e) {

            console.error(e);

        }

    },

    async loadDashboard() {

        try {

            const dashboard =
                await CustomerPortalService.getDashboard();

            document.getElementById(
                "vehicleCount"
            ).textContent =
                dashboard.vehicleCount;

            document.getElementById(
                "repairCount"
            ).textContent =
                dashboard.activeJobCount;

            document.getElementById(
                "estimateCount"
            ).textContent =
                dashboard.pendingEstimateCount;

            document.getElementById(
                "invoiceCount"
            ).textContent =
                dashboard.pendingInvoiceCount;

            document.getElementById(
                "estimateCountFooter"
            ).textContent =
                dashboard.pendingEstimateCount;

            document.getElementById(
                "invoiceCountFooter"
            ).textContent =
                dashboard.pendingInvoiceCount;

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