window.ManagerDashboard = {

    navigationRequests: [],

    navigationDrivers: [],

    render() {

        return `

<div class="fade-in">

    <div class="container-fluid">

        <!-- Welcome -->

        <div class="row mb-4">

            <div class="col">

                <h2 class="fw-bold">

                    Welcome Back 👋

                </h2>

                <p class="text-secondary">

                    Manage today's garage operations from one place.

                </p>

            </div>

        </div>

        <!-- Hero Card -->

        <div class="row mb-4">

            <div class="col">

                <div class="card shadow-sm border-0">

                    <div class="card-body p-5">

                        <div class="row align-items-center">

                            <div class="col-lg-8">

                                <h3 class="fw-bold mb-3">

                                    🚗 Start New Service

                                </h3>

                                <p class="text-secondary mb-4">

                                    Receive a vehicle,
                                    inspect it,
                                    prepare estimate,
                                    generate invoice and
                                    complete delivery.

                                </p>

                                <button
                                    id="startServiceBtn"
                                    class="btn btn-primary btn-lg">

                                    Start Service

                                </button>

                            </div>

                            <div class="col-lg-4 text-center">

                                <i
                                    class="bi bi-car-front-fill"
                                    style="
                                        font-size:120px;
                                        color:#2563EB;
                                    ">
                                </i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- KPI Cards -->

        <div class="row g-4 mb-4">

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Active Jobs

                                </small>

                                <h2
                                    id="activeJobs"
                                    class="fw-bold">

                                    0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-primary bg-opacity-10 p-3">

                                <i class="bi bi-car-front-fill text-primary fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Pending Estimates

                                </small>

                                <h2
                                    id="pendingEstimates"
                                    class="fw-bold">

                                    0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-warning bg-opacity-10 p-3">

                                <i class="bi bi-file-earmark-text text-warning fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Ready Delivery

                                </small>

                                <h2
                                    id="readyDelivery"
                                    class="fw-bold">

                                    0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-success bg-opacity-10 p-3">

                                <i class="bi bi-check-circle-fill text-success fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Completed Today

                                </small>

                                <h2
                                    id="completedToday"
                                    class="fw-bold">

                                    0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-info bg-opacity-10 p-3">

                                <i class="bi bi-trophy-fill text-info fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Revenue Today

                                </small>

                                <h2
                                    id="todayRevenue"
                                    class="fw-bold">

                                    ₹0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-success bg-opacity-10 p-3">

                                <i class="bi bi-currency-rupee text-success fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <div class="col-lg-2">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-body">

                        <div class="d-flex justify-content-between">

                            <div>

                                <small class="text-secondary">

                                    Vehicles

                                </small>

                                <h2
                                    id="totalVehicles"
                                    class="fw-bold">

                                    0

                                </h2>

                            </div>

                            <div
                                class="rounded-circle bg-dark bg-opacity-10 p-3">

                                <i class="bi bi-truck text-dark fs-4"></i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- ==================================================
             PICKUP & DELIVERY
             ================================================== -->

        <div class="row mb-4">

            <div class="col-12">

                <div class="card shadow-sm border-0">

                    <div
                        class="card-header bg-white
                               d-flex
                               justify-content-between
                               align-items-center">

                        <div>

                            <h5 class="mb-1">

                                Pickup & Delivery

                            </h5>

                            <small class="text-muted">

                                Customer vehicle movement requests

                            </small>

                        </div>


                        <button
                            id="refreshNavigationRequests"
                            class="btn btn-sm
                                   btn-outline-primary">

                            <i class="bi bi-arrow-clockwise"></i>

                        </button>

                    </div>


                    <div class="card-body">

                        <div class="row g-3 mb-4">

                            <div class="col-md-4">

                                <div
                                    class="border
                                           rounded
                                           p-3">

                                    <small class="text-muted">

                                        Pickup Requests

                                    </small>

                                    <h3
                                        id="managerPickupRequests"
                                        class="fw-bold mb-0">

                                        0

                                    </h3>

                                </div>

                            </div>


                            <div class="col-md-4">

                                <div
                                    class="border
                                           rounded
                                           p-3">

                                    <small class="text-muted">

                                        Delivery Requests

                                    </small>

                                    <h3
                                        id="managerDeliveryRequests"
                                        class="fw-bold mb-0">

                                        0

                                    </h3>

                                </div>

                            </div>


                            <div class="col-md-4">

                                <div
                                    class="border
                                           rounded
                                           p-3">

                                    <small class="text-muted">

                                        Active Trips

                                    </small>

                                    <h3
                                        id="managerActiveTrips"
                                        class="fw-bold mb-0">

                                        0

                                    </h3>

                                </div>

                            </div>

                        </div>


                        <div id="managerNavigationRequests">

                            <div
                                class="text-center
                                       text-muted
                                       py-4">

                                Loading pickup &
                                delivery requests...

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- Workflow Summary -->

        <div class="row mb-4">

            <div class="col-lg-4">

                <div class="card shadow-sm border-0 h-100">

                    <div class="card-header bg-white">

                        <h5 class="mb-0">

                            Today's Workflow

                        </h5>

                    </div>

                    <div class="card-body">

                        <div class="d-flex justify-content-between py-2">

                            <span>Inspection</span>

                            <strong id="inspectionJobs">0</strong>

                        </div>

                        <div class="d-flex justify-content-between py-2">

                            <span>Estimate</span>

                            <strong id="estimateJobs">0</strong>

                        </div>

                        <div class="d-flex justify-content-between py-2">

                            <span>Repair</span>

                            <strong id="repairJobs">0</strong>

                        </div>

                        <div class="d-flex justify-content-between py-2">

                            <span>Quality Check</span>

                            <strong id="qualityCheckJobs">0</strong>

                        </div>

                        <div class="d-flex justify-content-between py-2">

                            <span>Ready For Invoice</span>

                            <strong id="readyForInvoiceJobs">0</strong>

                        </div>

                        <div class="d-flex justify-content-between py-2">

                            <span>Payment Pending</span>

                            <strong id="paymentPendingJobs">0</strong>

                        </div>

                    </div>

                </div>

            </div>

                        <div class="col-lg-8">

                            <div class="card shadow-sm border-0 h-100">

                                <div class="card-header bg-white d-flex justify-content-between align-items-center">

                                    <h5 class="mb-0">

                                        Recent Jobs

                                    </h5>

                                    <span
                                        class="badge bg-primary"
                                        id="recentJobCount">

                                        0

                                    </span>

                                </div>

                                <div class="table-responsive">

                                    <table class="table table-hover align-middle mb-0">

                                        <thead class="table-light">

                                            <tr>

                                                <th>Job Card</th>

                                                <th>Customer</th>

                                                <th>Vehicle</th>

                                                <th>Status</th>

                                                <th>ETA</th>

                                                <th width="110">

                                                    Action

                                                </th>

                                            </tr>

                                        </thead>

                                        <tbody id="recentJobsTable">

                                        </tbody>

                                    </table>

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

    `;

    },

    bindEvents() {

        document

            .getElementById("startServiceBtn")

            ?.addEventListener("click", () => {

                Router.navigate("workflow");

            });

        document.addEventListener("click", async (e) => {

            const btn = e.target.closest(".continue-job");

            if (!btn) {
                return;
            }

//                try {
//
//                    const response = await Api.get(
//                        "/jobcards/search?jobCardNumber=" +
//                        encodeURIComponent(btn.dataset.job)
//                    );
//
//                    WorkflowHelper.reset();
//
//                    WorkflowHelper.state.job = response;
//
//                    WorkflowHelper.state.jobCardNumber =
//                        response.jobCardNumber;
//
//                    WorkflowHelper.state.jobId =
//                        response.id;
//
//                    Router.navigate("workflow");
//
//                }
//                catch (err) {
//
//                    console.error(err);
//
//                    Toast.error("Unable to load job.");
//
//                }

            try {

                WorkflowHelper.reset();

                const workflow =
                    await WorkflowService.resumeWorkflow(
                        btn.dataset.job
                    );

                Router.navigate("workflow");

            } catch (err) {

                console.error(err);

                alert("Unable to resume workflow.");

            }

        });

        this.loadData();

        this.loadNavigationDrivers();

        this.loadNavigationRequests();

        document
            .getElementById(
                "refreshNavigationRequests"
            )
            ?.addEventListener(
                "click",
                () => this.loadNavigationRequests()
            );

    },

    async loadNavigationDrivers() {

        try {

            const drivers =
                await UserService.getDrivers();

            this.navigationDrivers =
                drivers ?? [];

            console.log(
                "Navigation drivers:",
                this.navigationDrivers
            );

        } catch (error) {

            console.error(
                "Unable to load drivers:",
                error
            );

            this.navigationDrivers = [];

        }

    },

    async loadData() {

        try {

            const summary =
                await DashboardService.getSummary();

            this.renderSummary(summary);

            const jobs =
                await DashboardService.getRecentJobs();

            this.renderRecentJobs(jobs);

        } catch (error) {

            console.error(
                "Failed to load dashboard",
                error
            );

        }

    },

    renderSummary(summary) {

        document.getElementById("activeJobs").textContent =
            summary.activeJobs;

        document.getElementById("pendingEstimates").textContent =
            summary.pendingEstimates;

        document.getElementById("readyDelivery").textContent =
            summary.readyForDelivery;

        document.getElementById("completedToday").textContent =
            summary.completedToday;

        document.getElementById("todayRevenue").textContent =
            "₹ " + Number(summary.todayRevenue ?? 0)
                .toLocaleString("en-IN", {

                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2

                });

        document.getElementById("totalVehicles").textContent =
            summary.totalVehicles;

        document.getElementById("inspectionJobs").textContent =
            summary.inspectionJobs;

        document.getElementById("estimateJobs").textContent =
            summary.estimateJobs;

        document.getElementById("repairJobs").textContent =
            summary.repairJobs;

        document.getElementById("qualityCheckJobs").textContent =
            summary.qualityCheckJobs;

        document.getElementById("readyForInvoiceJobs").textContent =
            summary.readyForInvoiceJobs;

        document.getElementById("paymentPendingJobs").textContent =
            summary.paymentPendingJobs;

    },

    renderRecentJobs(jobs) {

        const tbody =
            document.getElementById("recentJobsTable");

        tbody.innerHTML = "";

        document.getElementById("recentJobCount")
            .textContent = jobs.length;

        jobs.forEach(job => {

            tbody.innerHTML += `

<tr>

<td>

    <strong>

        ${job.jobCardNumber}

    </strong>

</td>

<td>

    <div>

        ${job.customerName}

    </div>

    <small class="text-secondary">

        ${job.mobileNumber}

    </small>

</td>

<td>

    <div>

        ${job.vehicleName}

    </div>

    <small class="text-secondary">

        ${job.registrationNumber}

    </small>

</td>

<td>

    <span class="${Dashboard.getStatusBadge(job.status)}">

        ${Dashboard.formatStatus(job.status)}

    </span>

</td>

<td>

    ${job.estimatedDeliveryDate ?? "-"}

</td>

<td>

    <button

        class="btn btn-sm btn-outline-primary continue-job"

        data-job="${job.jobCardNumber}">

        Continue

    </button>

</td>

</tr>

`;

        });

    },

    async loadNavigationRequests() {

        const garageId =
            EmployeeSession.garageId();

        if (!garageId) {

            console.error(
                "Garage ID is not available."
            );

            return;

        }


        const container =
            document.getElementById(
                "managerNavigationRequests"
            );

        try {

            const requests =
                await NavigationRequestService
                    .getGarageRequests(
                        garageId
                    );


            this.navigationRequests =
                requests ?? [];


            this.updateNavigationStatistics();

            this.renderNavigationRequests();

        } catch (error) {

            console.error(
                "Unable to load navigation requests:",
                error
            );


            if (container) {

                container.innerHTML = `

                    <div
                        class="alert alert-danger mb-0">

                        Unable to load
                        pickup and delivery requests.

                    </div>

                `;

            }

        }

    },


    updateNavigationStatistics() {

        const requests =
            this.navigationRequests ?? [];


        const pickups =
            requests.filter(
                request =>
                    request.requestType === "PICKUP"
            ).length;


        const deliveries =
            requests.filter(
                request =>
                    request.requestType === "DELIVERY"
            ).length;


        const activeTrips =
            requests.filter(
                request =>
                    [
                        "ASSIGNED",
                        "ACCEPTED",
                        "IN_PROGRESS"
                    ].includes(
                        request.status
                    )
            ).length;


        this.setNavigationValue(
            "managerPickupRequests",
            pickups
        );


        this.setNavigationValue(
            "managerDeliveryRequests",
            deliveries
        );


        this.setNavigationValue(
            "managerActiveTrips",
            activeTrips
        );

    },


    setNavigationValue(
        id,
        value
    ) {

        const element =
            document.getElementById(id);

        if (element) {

            element.textContent =
                value;

        }

    },


    renderNavigationRequests() {

        const container =
            document.getElementById(
                "managerNavigationRequests"
            );

        if (!container) {
            return;
        }


        if (!this.navigationRequests.length) {

            container.innerHTML = `

                <div
                    class="text-center
                           text-muted
                           py-4">

                    <i
                        class="bi bi-truck
                               display-6">
                    </i>

                    <div class="mt-2">

                        No pickup or delivery
                        requests.

                    </div>

                </div>

            `;

            return;

        }


        container.innerHTML =
            this.navigationRequests
                .map(
                    request =>
                        this.renderNavigationRequest(
                            request
                        )
                )
                .join("");


        this.bindNavigationEvents();

    },


    renderNavigationRequest(request) {

        const type =
            request.requestType;


        const title =
            type === "PICKUP"
                ? "🚗 Pickup"
                : "🚙 Delivery";


        return `

            <div
                class="border
                       rounded
                       p-3
                       mb-3">

                <div
                    class="d-flex
                           justify-content-between
                           align-items-start">

                    <div>

                        <h6 class="fw-bold mb-1">

                            ${title}

                        </h6>

                        <small class="text-muted">

                            Request #${request.id}

                        </small>

                    </div>


                    <span
                        class="${this.getNavigationBadge(
                            request.status
                        )}">

                        ${this.formatNavigationStatus(
                            request.status
                        )}

                    </span>

                </div>


                <div class="row g-3 mt-2">

                    <div class="col-md-4">

                        <small class="text-muted">

                            Vehicle

                        </small>

                        <div class="fw-semibold">

                            #${request.vehicleId ?? "-"}

                        </div>

                    </div>


                    <div class="col-md-4">

                        <small class="text-muted">

                            Scheduled

                        </small>

                        <div class="fw-semibold">

                            ${
                                request.scheduledAt
                                    ? this.formatNavigationDate(
                                        request.scheduledAt
                                    )
                                    : "-"
                            }

                        </div>

                    </div>


                    <div class="col-md-4">

                        <small class="text-muted">

                            Address

                        </small>

                        <div class="fw-semibold">

                            ${
                                type === "PICKUP"
                                    ? (
                                        request.pickupAddress
                                        ?? "-"
                                    )
                                    : (
                                        request.deliveryAddress
                                        ?? "-"
                                    )
                            }

                        </div>

                    </div>

                </div>


                <div class="mt-3">

                    ${
                        request.tripId

                            ? `

                                <span
                                    class="badge bg-success">

                                    Trip #${request.tripId}

                                </span>

                                <button
                                    class="btn
                                           btn-sm
                                           btn-outline-primary
                                           ms-2
                                           manager-track-trip"
                                    data-trip-id="${request.tripId}">

                                    <i
                                        class="bi bi-geo-alt me-1">
                                    </i>

                                    Track

                                </button>

                            `

                            : `

                                <button
                                    class="btn
                                           btn-sm
                                           btn-primary
                                           manager-assign-driver"
                                    data-request-id="${request.id}">

                                    <i
                                        class="bi bi-person-check me-1">
                                    </i>

                                    Assign Driver

                                </button>

                            `
                    }

                </div>

            </div>

        `;

    },


    bindNavigationEvents() {

        document
            .querySelectorAll(
                ".manager-assign-driver"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () => {

                        const requestId =
                            Number(
                                button.dataset.requestId
                            );

                        this.openAssignDriver(
                            requestId
                        );

                    }
                );

            });


        document
            .querySelectorAll(
                ".manager-track-trip"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () => {

                        const tripId =
                            Number(
                                button.dataset.tripId
                            );

                        this.trackTrip(
                            tripId
                        );

                    }
                );

            });

    },


    async openAssignDriver(requestId) {

        const request =
            this.navigationRequests.find(
                item =>
                    Number(item.id) ===
                    Number(requestId)
            );


        if (!request) {

            alert(
                "Navigation request not found."
            );

            return;

        }


        if (!this.navigationDrivers.length) {

            await this.loadNavigationDrivers();

        }


        if (!this.navigationDrivers.length) {

            alert(
                "No drivers are available."
            );

            return;

        }


        this.showAssignDriverModal(
            request
        );

    },

    showAssignDriverModal(request) {

        const existingModal =
            document.getElementById(
                "assignDriverModal"
            );


        if (existingModal) {

            existingModal.remove();

        }


        const drivers =
            this.navigationDrivers;


        const modal =
            document.createElement("div");


        modal.id =
            "assignDriverModal";


        modal.className =
            "modal fade";


        modal.tabIndex =
            -1;


        modal.innerHTML = `

            <div class="modal-dialog modal-dialog-centered">

                <div class="modal-content">

                    <div class="modal-header">

                        <div>

                            <h5 class="modal-title">

                                Assign Driver

                            </h5>

                            <small class="text-muted">

                                Request #${request.id}

                            </small>

                        </div>


                        <button
                            type="button"
                            class="btn-close"
                            data-bs-dismiss="modal">
                        </button>

                    </div>


                    <div class="modal-body">

                        <!-- Vehicle -->

                        <div class="mb-3">

                            <label class="form-label">

                                Vehicle

                            </label>

                            <div
                                class="border
                                       rounded
                                       p-3
                                       bg-light">

                                <div class="fw-semibold">

                                    Vehicle #${request.vehicleId ?? "-"}

                                </div>

                            </div>

                        </div>


                        <!-- Driver -->

                        <div class="mb-3">

                            <label
                                for="navigationDriverSelect"
                                class="form-label">

                                Select Driver

                            </label>


                            <select
                                id="navigationDriverSelect"
                                class="form-select">

                                <option value="">

                                    Select Driver

                                </option>


                                ${
                                    drivers
                                        .map(
                                            driver => `

                                                <option
                                                    value="${driver.id}"
                                                >

                                                    ${
                                                        this.getDriverDisplayName(
                                                            driver
                                                        )
                                                    }

                                                </option>

                                            `
                                        )
                                        .join("")
                                }

                            </select>

                        </div>

                    </div>


                    <div class="modal-footer">

                        <button
                            type="button"
                            class="btn btn-secondary"
                            data-bs-dismiss="modal">

                            Cancel

                        </button>


                        <button
                            type="button"
                            class="btn btn-primary"
                            id="confirmAssignDriver">

                            <i
                                class="bi bi-person-check me-1">
                            </i>

                            Assign Driver

                        </button>

                    </div>

                </div>

            </div>

        `;


        document.body.appendChild(
            modal
        );


        const modalInstance =
            new bootstrap.Modal(
                modal
            );


        modalInstance.show();


        document
            .getElementById(
                "confirmAssignDriver"
            )
            ?.addEventListener(
                "click",
                async () => {

                    const select =
                        document.getElementById(
                            "navigationDriverSelect"
                        );


                    const driverId =
                        Number(select.value);


                    if (!driverId) {

                        alert(
                            "Please select a driver."
                        );

                        return;

                    }


                    await this.assignDriver(
                        request.id,
                        driverId,
                        modalInstance
                    );

                }
            );


        modal.addEventListener(
            "hidden.bs.modal",
            () => {

                modal.remove();

            }
        );

    },

    getDriverDisplayName(driver) {

        const firstName =
            driver.firstName ?? "";


        const lastName =
            driver.lastName ?? "";


        const fullName =
            `${firstName} ${lastName}`.trim();


        if (fullName) {

            if (driver.employeeCode) {

                return `${fullName} (${driver.employeeCode})`;

            }

            return fullName;

        }


        return (
            driver.username ??
            `Driver #${driver.id}`
        );

    },

    async assignDriver(
        requestId,
        driverId,
        modalInstance
    ) {

        const button =
            document.getElementById(
                "confirmAssignDriver"
            );


        try {

            if (button) {

                button.disabled =
                    true;

                button.innerHTML = `

                    <span
                        class="spinner-border
                               spinner-border-sm
                               me-1">
                    </span>

                    Assigning...

                `;

            }


            await NavigationRequestService
                .assignDriver(
                    requestId,
                    driverId
                );


            modalInstance.hide();


            alert(
                "Driver assigned successfully."
            );


            await this.loadNavigationRequests();


        } catch (error) {

            console.error(
                "Unable to assign driver:",
                error
            );


            if (button) {

                button.disabled =
                    false;

                button.innerHTML = `

                    <i
                        class="bi bi-person-check me-1">
                    </i>

                    Assign Driver

                `;

            }


            alert(
                "Unable to assign driver."
            );

        }

    },


    async trackTrip(
        tripId
    ) {

        try {

            const trip =
                await NavigationTripService
                    .getTrip(
                        tripId
                    );


            sessionStorage.setItem(
                "navigationTripId",
                tripId
            );


            console.log(
                "Navigation trip:",
                trip
            );


            alert(
                `Trip #${tripId}\nStatus: ${
                    this.formatNavigationStatus(
                        trip.status
                    )
                }`
            );

        } catch (error) {

            console.error(
                "Unable to load trip:",
                error
            );

            alert(
                "Unable to load trip."
            );

        }

    },


    getNavigationBadge(status) {

        switch (status) {

            case "REQUESTED":
                return "badge bg-warning text-dark";

            case "ASSIGNED":
                return "badge bg-info text-dark";

            case "ACCEPTED":
                return "badge bg-primary";

            case "IN_PROGRESS":
                return "badge bg-primary";

            case "COMPLETED":
                return "badge bg-success";

            case "CANCELLED":
                return "badge bg-danger";

            default:
                return "badge bg-secondary";

        }

    },


    formatNavigationStatus(status) {

        return (
            status ?? ""
        )
            .replaceAll(
                "_",
                " "
            )
            .toLowerCase()
            .replace(
                /\b\w/g,
                c => c.toUpperCase()
            );

    },


    formatNavigationDate(value) {

        if (!value) {
            return "-";
        }

        return new Date(
            value
        ).toLocaleString();

    },

};