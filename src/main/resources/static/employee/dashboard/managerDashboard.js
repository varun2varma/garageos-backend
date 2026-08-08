window.ManagerDashboard = {

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

                Toast.error("Unable to resume workflow.");

            }

        });

        this.loadData();

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

};