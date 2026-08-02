window.OwnerDashboard = {

    async init() {

        console.log("Owner Dashboard Loaded");

        this.bindEvents();

        await this.loadDashboard();

    },

    async loadDashboard() {

        try {

            const dashboard =
                await OwnerService.getSummary();

            this.renderSummary(dashboard);

        } catch (e) {

            console.error(
                "Failed to load owner dashboard.",
                e
            );

        }

    },

    renderSummary(summary) {

        const employeeCount =
            document.getElementById(
                "employeeCount"
            );

        if (employeeCount) {

            employeeCount.textContent =
                summary.employeeCount ?? 0;

        }

        const customerCount =
            document.getElementById(
                "customerCount"
            );

        if (customerCount) {

            customerCount.textContent =
                summary.totalCustomers ?? 0;

        }

        const vehicleCount =
            document.getElementById(
                "vehicleCount"
            );

        if (vehicleCount) {

            vehicleCount.textContent =
                summary.totalVehicles ?? 0;

        }

        const jobCount =
            document.getElementById(
                "jobCount"
            );

        if (jobCount) {

            jobCount.textContent =
                summary.activeJobs ?? 0;

        }

        const pendingEmployeeCount =
            document.getElementById(
                "pendingEmployeeCount"
            );

        if (pendingEmployeeCount) {

            pendingEmployeeCount.textContent =
                summary.pendingEmployeeCount ?? 0;

        }

        const activity =
            document.getElementById(
                "recentActivity"
            );

        if (!activity) {

            return;

        }

        activity.innerHTML = `

            <div class="d-flex justify-content-between py-2 border-bottom">

                <span>

                    Active Jobs

                </span>

                <strong>

                    ${summary.activeJobs ?? 0}

                </strong>

            </div>

            <div class="d-flex justify-content-between py-2 border-bottom">

                <span>

                    Pending Estimates

                </span>

                <strong>

                    ${summary.pendingEstimates ?? 0}

                </strong>

            </div>

            <div class="d-flex justify-content-between py-2 border-bottom">

                <span>

                    Ready For Invoice

                </span>

                <strong>

                    ${summary.readyForInvoiceJobs ?? 0}

                </strong>

            </div>

            <div class="d-flex justify-content-between py-2 border-bottom">

                <span>

                    Ready For Delivery

                </span>

                <strong>

                    ${summary.readyForDelivery ?? 0}

                </strong>

            </div>

            <div class="d-flex justify-content-between py-2">

                <span>

                    Revenue Today

                </span>

                <strong>

                    ₹ ${Number(
                        summary.todayRevenue ?? 0
                    ).toLocaleString("en-IN")}

                </strong>

            </div>

        `;

    },

    bindEvents() {

        const employeesButton =
            document.getElementById(
                "manageEmployeesBtn"
            );

        if (employeesButton) {

            employeesButton.addEventListener(

                "click",

                () => OwnerRouter.navigate(
                    "employees"
                )

            );

        }

        const customersButton =
            document.getElementById(
                "manageCustomersBtn"
            );

        if (customersButton) {

            customersButton.addEventListener(

                "click",

                () => OwnerRouter.navigate(
                    "customers"
                )

            );

        }

        const vehiclesButton =
            document.getElementById(
                "manageVehiclesBtn"
            );

        if (vehiclesButton) {

            vehiclesButton.addEventListener(

                "click",

                () => OwnerRouter.navigate(
                    "vehicles"
                )

            );

        }

        const pendingButton =
            document.getElementById(
                "viewPendingBtn"
            );

        if (pendingButton) {

            pendingButton.addEventListener(

                "click",

                () => OwnerRouter.navigate(
                    "employees"
                )

            );

        }

    }

};