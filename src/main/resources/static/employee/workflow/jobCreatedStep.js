window.JobCreatedStep = {

    render() {

        const customer = WorkflowHelper.state.customer || {};
        const vehicle = WorkflowHelper.state.vehicle || {};
        const job = WorkflowHelper.state.job || {};

        return `

    <div class="card shadow-sm border-0">

        <div class="card-body">

            <div class="text-center mb-4">

                <div class="display-1 text-success">

                    <i class="bi bi-check-circle-fill"></i>

                </div>

                <h2 class="fw-bold mt-3">

                    Job Card Created Successfully

                </h2>

                <p class="text-muted">

                    The vehicle has been registered successfully and is now waiting for inspection.

                </p>

            </div>

            <div class="row">

                <div class="col-lg-4">

                    <div class="card border-0 bg-light h-100">

                        <div class="card-body">

                            <h6 class="text-muted">

                                CUSTOMER

                            </h6>

                            <h5>

                                ${customer.firstName || "-"}

                            </h5>

                            <div>

                                ${customer.mobileNumber || "-"}

                            </div>

                            <div class="mt-2">

                                <small class="text-muted">

                                    Email

                                </small>

                                <div>

                                    ${customer.email || "-"}

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

                <div class="col-lg-4">

                    <div class="card border-0 bg-light h-100">

                        <div class="card-body">

                            <h6 class="text-muted">

                                VEHICLE

                            </h6>

                            <h5>

                                ${vehicle.registrationNumber || "-"}

                            </h5>

                            <div>

                                ${vehicle.brand || ""}

                                ${vehicle.model || ""}

                            </div>

                            <div class="mt-2">

                                <small class="text-muted">

                                    Variant

                                </small>

                                <div>

                                    ${vehicle.variant || "-"}

                                </div>

                            </div>

                            <div class="mt-2">

                                <small class="text-muted">

                                    Fuel

                                </small>

                                <div>

                                    ${vehicle.fuelType || "-"}

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

                <div class="col-lg-4">

                    <div class="card border-0 bg-light h-100">

                        <div class="card-body">

                            <h6 class="text-muted">

                                JOB CARD

                            </h6>

                            <h5>

                                ${job.jobCardNumber || "-"}

                            </h5>

                            <span class="badge bg-warning text-dark">

                                Waiting For Inspection

                            </span>

                            <div class="mt-3">

                                <div class="small text-muted">

                                    Created

                                </div>

                                <div>

                                    ${job.serviceDate || "-"}

                                </div>

                            </div>

                            <div class="mt-2">

                                <div class="small text-muted">

                                    Delivery

                                </div>

                                <div>

                                    ${job.estimatedDeliveryDate || "-"}

                                </div>

                            </div>

                            <div class="mt-2">

                                <div class="small text-muted">

                                    Odometer

                                </div>

                                <div>

                                    ${job.odometerReading || 0} KM

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

            <hr class="my-4">

            <div id="workflowTimeline"></div>

            <div class="text-center mt-4">

                <button
                    id="printJobBtn"
                    class="btn btn-outline-primary me-2">

                    <i class="bi bi-printer"></i>

                    Print Job Card

                </button>

                <button
                    id="newJobBtn"
                    class="btn btn-primary">

                    <i class="bi bi-plus-circle"></i>

                    Create New Job Card

                </button>

            </div>

        </div>

    </div>

    `;

    },

    bindEvents() {

        document
            .getElementById("workflowTimeline")
            .innerHTML =
            WorkflowTimeline.render(3);

        document
            .getElementById("newJobBtn")
            ?.addEventListener("click", () => {

                Workflow.reset();

            });

        document
            .getElementById("printJobBtn")
            ?.addEventListener("click", () => {

                window.print();

            });

    },

};