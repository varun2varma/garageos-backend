window.CustomerDashboard = {

    async init() {

        try {

            CustomerApp.showLoading();

            await Promise.all([

                this.loadCustomer(),

                this.loadVehicles(),

                this.loadRepairs(),

                this.loadEstimates(),

                this.loadInvoices()

            ]);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    async loadCustomer() {

        try {

            if (!CustomerPortalService.getProfile) {

                return;

            }

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

    async loadVehicles() {

        try {

            if (!CustomerPortalService.getVehicles) {

                return;

            }

            const vehicles =
                await CustomerPortalService.getVehicles();

            document.getElementById(
                "vehicleCount"
            ).textContent =
                vehicles.length;

            this.renderVehicles(vehicles);

        } catch (e) {

            console.error(e);

        }

    },

    async loadRepairs() {

        try {

            if (!CustomerPortalService.getRepairTracking(jobCardNumber)) {

                return;

            }

            const jobs =
                await CustomerPortalService.getRepairTracking(jobCardNumber)();

            document.getElementById(
                "repairCount"
            ).textContent =
                jobs.length;

            this.renderRepairs(jobs);

        } catch (e) {

            console.error(e);

        }

    },

    async loadEstimates() {

        try {

            if (!CustomerPortalService.getEstimates) {

                return;

            }

            const estimates =
                await CustomerPortalService.getEstimates();

            document.getElementById(
                "estimateCount"
            ).textContent =
                estimates.length;

        } catch (e) {

            console.error(e);

        }

    },

    async loadInvoices() {

        try {

            if (!CustomerPortalService.getEstimates) {

                return;

            }

            const invoices =
                await CustomerPortalService.getEstimates();

            document.getElementById(
                "invoiceCount"
            ).textContent =
                invoices.length;

        } catch (e) {

            console.error(e);

        }

    },

    renderVehicles(vehicles) {

        const container =
            document.getElementById(
                "vehicleList"
            );

        if (!vehicles.length) {

            container.innerHTML = `

                <div class="empty-state">

                    <i class="bi bi-car-front"></i>

                    <h5>

                        No Vehicles

                    </h5>

                    <p>

                        No registered vehicles found.

                    </p>

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

                            <i class="bi bi-car-front"></i>

                        </div>

                        <div class="vehicle-details">

                            <h6>

                                ${vehicle.registrationNumber}

                            </h6>

                            <small>

                                ${vehicle.make}
                                ${vehicle.model}

                            </small>

                        </div>

                    </div>

                `)
                .join("");

    },

    renderRepairs(jobs) {

        const container =
            document.getElementById(
                "recentRepairs"
            );

        if (!jobs.length) {

            container.innerHTML = `

                <div class="empty-state">

                    <i class="bi bi-tools"></i>

                    <h5>

                        No Active Repairs

                    </h5>

                    <p>

                        Your repair jobs will appear here.

                    </p>

                </div>

            `;

            return;

        }

        container.innerHTML =
            jobs
                .slice(0, 5)
                .map(job => `

                    <div class="repair-item">

                        <div class="repair-info">

                            <h6>

                                ${job.jobCardNumber}

                            </h6>

                            <small>

                                ${job.vehicleRegistrationNumber}

                            </small>

                        </div>

                        <span class="status-badge status-${job.status.toLowerCase()}">

                            ${job.status}

                        </span>

                    </div>

                `)
                .join("");

    }

};

//document.addEventListener(
//
//    "DOMContentLoaded",
//
//    () => {
//
//        CustomerDashboard.init();
//
//    }
//
//);