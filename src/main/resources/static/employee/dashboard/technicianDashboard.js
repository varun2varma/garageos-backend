window.TechnicianDashboard = {

    isDriver() {

        return EmployeeSession
            .roles()
            .includes(Roles.DRIVER);

    },

    isTechnician() {

        return EmployeeSession
            .roles()
            .includes(Roles.TECHNICIAN);

    },

    render() {

        const driver = this.isDriver();

        return `

<div class="fade-in">

<div class="container-fluid">

    <div class="mb-4">

        <h2 class="fw-bold">

            ${
                driver
                    ? "Ready for today's trips 🚗"
                    : "Good Morning 👋"
            }

        </h2>

        <p class="text-secondary">

            ${
                driver
                    ? "Manage your assigned trips."
                    : "Manage your assigned repair jobs."
            }

        </p>

    </div>


    <!-- ================================================= -->
    <!-- SUMMARY -->
    <!-- ================================================= -->

    <div class="row g-3 mb-4">

        <div class="col-md-3">

            <div class="card shadow-sm border-0 h-100">

                <div class="card-body">

                    <small class="text-muted">
                        Pending Acceptance
                    </small>

                    <h2
                        id="assignedJobs"
                        class="fw-bold mt-2">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0 h-100">

                <div class="card-body">

                    <small class="text-muted">
                        In Progress
                    </small>

                    <h2
                        id="inProgressJobs"
                        class="fw-bold mt-2">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0 h-100">

                <div class="card-body">

                    <small class="text-muted">
                        Completed
                    </small>

                    <h2
                        id="completedJobs"
                        class="fw-bold mt-2">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0 h-100">

                <div class="card-body">

                    <small class="text-muted">

                        ${
                            driver
                                ? "Ready to Start"
                                : "Awaiting QC"
                        }

                    </small>

                    <h2
                        id="pendingQc"
                        class="fw-bold mt-2">

                        0

                    </h2>

                </div>

            </div>

        </div>

    </div>


    <!-- ================================================= -->
    <!-- ASSIGNMENTS -->
    <!-- ================================================= -->

    <div class="card shadow-sm border-0">

        <div class="card-header bg-white">

            <div class="d-flex justify-content-between align-items-center">

                <div>

                    <h5 class="mb-1">

                        ${
                            driver
                                ? "Today's Assigned Trips"
                                : "Today's Assigned Jobs"
                        }

                    </h5>

                    <small class="text-muted">

                        ${
                            driver
                                ? "Accept and complete your assigned trips."
                                : "Accept and complete your assigned repair work."
                        }

                    </small>

                </div>

                <button
                    id="refreshAssignmentsBtn"
                    class="btn btn-outline-secondary btn-sm">

                    <i class="bi bi-arrow-clockwise"></i>

                    Refresh

                </button>

            </div>

        </div>


        <div
            class="card-body"
            id="technicianAssignments">

            <div class="text-center py-4">

                <div class="spinner-border"></div>

                <div class="mt-2 text-muted">

                    Loading assignments...

                </div>

            </div>

        </div>

    </div>

</div>

</div>


<!-- ===================================================== -->
<!-- COMPLETE JOB MODAL -->
<!-- ===================================================== -->

<div
    class="modal fade"
    id="completeAssignmentModal"
    tabindex="-1">

    <div class="modal-dialog">

        <div class="modal-content">

            <div class="modal-header">

                <h5 class="modal-title">

                    <i class="bi bi-check-circle me-2"></i>

                    Complete Job

                </h5>

                <button
                    type="button"
                    class="btn-close"
                    data-bs-dismiss="modal">
                </button>

            </div>


            <div class="modal-body">

                <input
                    type="hidden"
                    id="completeAssignmentId">


                <div class="mb-3">

                    <label class="form-label">

                        Actual Hours

                    </label>

                    <input
                        type="number"
                        id="actualHours"
                        class="form-control"
                        min="0"
                        step="0.5"
                        placeholder="Example: 2.5">

                </div>


                <div class="mb-3">

                    <label class="form-label">

                        Remarks

                    </label>

                    <textarea
                        id="completionRemarks"
                        class="form-control"
                        rows="4"
                        placeholder="Enter completion remarks...">
                    </textarea>

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
                    id="confirmCompleteAssignmentBtn"
                    class="btn btn-success">

                    <i class="bi bi-check-circle me-1"></i>

                    Complete Job

                </button>

            </div>

        </div>

    </div>

</div>

`;

    },


    bindEvents() {

        this.loadAssignments();


        /*
        =========================================
        REFRESH
        =========================================
        */

        document
            .getElementById(
                "refreshAssignmentsBtn"
            )
            ?.addEventListener(
                "click",
                () => this.loadAssignments()
            );


        /*
        =========================================
        ACCEPT JOB
        =========================================
        */

        document.addEventListener(
            "click",
            async (e) => {

                const button =
                    e.target.closest(
                        ".acceptJob"
                    );

                if (!button) {

                    return;

                }

                const assignmentId =
                    Number(
                        button.dataset.id
                    );

                try {

                    button.disabled = true;

                    await JobAssignmentService.accept(
                        assignmentId
                    );

                    await this.loadAssignments();

                }
                catch (error) {

                    console.error(error);

                    alert(
                        error.message ||
                        "Unable to accept job."
                    );

                    button.disabled = false;

                }

            }
        );


        /*
        =========================================
        START JOB
        =========================================
        */

        document.addEventListener(
            "click",
            async (e) => {

                const button =
                    e.target.closest(
                        ".startJob"
                    );

                if (!button) {

                    return;

                }

                const assignmentId =
                    Number(
                        button.dataset.id
                    );

                try {

                    button.disabled = true;

                    await JobAssignmentService.start(
                        assignmentId,
                        {
                            remarks: ""
                        }
                    );

                    await this.loadAssignments();

                }
                catch (error) {

                    console.error(error);

                    alert(
                        error.message ||
                        "Unable to start job."
                    );

                    button.disabled = false;

                }

            }
        );


        /*
        =========================================
        OPEN COMPLETE MODAL
        =========================================
        */

        document.addEventListener(
            "click",
            (e) => {

                const button =
                    e.target.closest(
                        ".completeJob"
                    );

                if (!button) {

                    return;

                }

                const assignmentId =
                    Number(
                        button.dataset.id
                    );


                document.getElementById(
                    "completeAssignmentId"
                ).value =
                    assignmentId;


                document.getElementById(
                    "actualHours"
                ).value = "";


                document.getElementById(
                    "completionRemarks"
                ).value = "";


                const modal =
                    new bootstrap.Modal(
                        document.getElementById(
                            "completeAssignmentModal"
                        )
                    );


                modal.show();

            }
        );


        /*
        =========================================
        CONFIRM COMPLETE
        =========================================
        */

        document
            .getElementById(
                "confirmCompleteAssignmentBtn"
            )
            ?.addEventListener(
                "click",
                async () => {

                    const assignmentId =
                        Number(
                            document.getElementById(
                                "completeAssignmentId"
                            ).value
                        );


                    const actualHours =
                        Number(
                            document.getElementById(
                                "actualHours"
                            ).value
                        );


                    const remarks =
                        document.getElementById(
                            "completionRemarks"
                        ).value
                        .trim();


                    if (!actualHours || actualHours <= 0) {

                        alert(
                            "Please enter actual hours."
                        );

                        return;

                    }


                    const button =
                        document.getElementById(
                            "confirmCompleteAssignmentBtn"
                        );


                    try {

                        button.disabled = true;

                        button.innerHTML = `

<span
    class="spinner-border spinner-border-sm me-1">
</span>

Completing...

`;


                        await JobAssignmentService.complete(

                            assignmentId,

                            {
                                actualHours,
                                remarks
                            }

                        );


                        bootstrap.Modal
                            .getInstance(
                                document.getElementById(
                                    "completeAssignmentModal"
                                )
                            )
                            ?.hide();


                        await this.loadAssignments();

                    }
                    catch (error) {

                        console.error(error);

                        alert(
                            error.message ||
                            "Unable to complete job."
                        );

                    }
                    finally {

                        button.disabled = false;

                        button.innerHTML = `

<i class="bi bi-check-circle me-1"></i>

Complete Job

`;

                    }

                }
            );

    },


    async loadAssignments() {

        try {

            const jobs =
                await JobAssignmentService
                    .myAssignments();


            const assigned =
                jobs.filter(
                    x =>
                        x.status === "ASSIGNED"
                ).length;


            const inProgress =
                jobs.filter(
                    x =>
                        x.status === "IN_PROGRESS"
                ).length;


            const completed =
                jobs.filter(
                    x =>
                        x.status === "COMPLETED"
                ).length;


            const accepted =
                jobs.filter(
                    x =>
                        x.status === "ACCEPTED"
                ).length;


            const assignedElement =
                document.getElementById(
                    "assignedJobs"
                );


            const inProgressElement =
                document.getElementById(
                    "inProgressJobs"
                );


            const completedElement =
                document.getElementById(
                    "completedJobs"
                );


            const pendingQcElement =
                document.getElementById(
                    "pendingQc"
                );


            if (assignedElement) {

                assignedElement.innerHTML =
                    assigned;

            }


            if (inProgressElement) {

                inProgressElement.innerHTML =
                    inProgress;

            }


            if (completedElement) {

                completedElement.innerHTML =
                    completed;

            }


            if (pendingQcElement) {

                pendingQcElement.innerHTML =
                    this.isDriver()
                        ? accepted
                        : completed;

            }


            this.renderAssignments(
                jobs
            );

        }
        catch (error) {

            console.error(error);

            const container =
                document.getElementById(
                    "technicianAssignments"
                );


            if (container) {

                container.innerHTML = `

<div class="alert alert-danger">

    <i class="bi bi-exclamation-triangle me-2"></i>

    Unable to load assignments.

    <button
        class="btn btn-sm btn-outline-danger ms-2"
        onclick="TechnicianDashboard.loadAssignments()">

        Retry

    </button>

</div>

`;

            }

        }

    },


    renderAssignments(jobs) {

        const container =
            document.getElementById(
                "technicianAssignments"
            );


        if (!container) {

            return;

        }


        if (!jobs || jobs.length === 0) {

            container.innerHTML = `

<div class="text-center py-5">

    <i
        class="bi bi-check-circle text-success"
        style="font-size:48px;">
    </i>

    <h5 class="mt-3">

        ${
            this.isDriver()
                ? "No Trips Assigned"
                : "No Jobs Assigned"
        }

    </h5>

    <p class="text-muted mb-0">

        You're all caught up.

    </p>

</div>

`;

            return;

        }


        container.innerHTML = jobs
            .map(
                job =>
                    this.renderAssignmentCard(
                        job
                    )
            )
            .join("");

    },


    renderAssignmentCard(job) {

        const status =
            job.status || "UNKNOWN";


        let badgeClass =
            "bg-secondary";


        switch (status) {

            case "ASSIGNED":

                badgeClass =
                    "bg-info";

                break;

            case "ACCEPTED":

                badgeClass =
                    "bg-primary";

                break;

            case "IN_PROGRESS":

                badgeClass =
                    "bg-warning text-dark";

                break;

            case "COMPLETED":

                badgeClass =
                    "bg-success";

                break;

            case "ON_HOLD":

                badgeClass =
                    "bg-danger";

                break;

        }


        return `

<div class="card mb-3 border-0 shadow-sm">

    <div class="card-body">

        <div class="row align-items-center g-3">


            <!-- ================================= -->
            <!-- JOB DETAILS -->
            <!-- ================================= -->

            <div class="col-md-8">

                <div class="d-flex align-items-center gap-2 mb-2">

                    <h5 class="mb-0">

                        ${job.jobCardNumber ?? "-"}

                    </h5>

                    <span
                        class="badge ${badgeClass}">

                        ${status.replaceAll(
                            "_",
                            " "
                        )}

                    </span>

                </div>


                <div class="mb-1">

                    <i class="bi bi-car-front me-1"></i>

                    ${job.vehicleName ?? "-"}

                </div>


                ${
                    job.registrationNumber
                        ? `

<div class="small text-muted">

    Registration:
    <strong>
        ${job.registrationNumber}
    </strong>

</div>

`
                        : ""
                }


                <div class="small text-muted mt-1">

                    <i class="bi bi-tools me-1"></i>

                    ${job.serviceName ?? "-"}

                </div>


                ${
                    job.customerName
                        ? `

<div class="small text-muted mt-1">

    <i class="bi bi-person me-1"></i>

    ${job.customerName}

</div>

`
                        : ""
                }


                ${
                    job.estimatedHours != null
                        ? `

<div class="small text-muted mt-1">

    Estimated:
    <strong>
        ${job.estimatedHours} hrs
    </strong>

</div>

`
                        : ""
                }


                ${
                    job.startedAt
                        ? `

<div class="small text-muted mt-1">

    Started:
    ${WorkflowHelper.formatDateTime
        ? WorkflowHelper.formatDateTime(
            job.startedAt
        )
        : job.startedAt}

</div>

`
                        : ""
                }


                ${
                    job.completedAt
                        ? `

<div class="small text-muted mt-1">

    Completed:
    ${WorkflowHelper.formatDateTime
        ? WorkflowHelper.formatDateTime(
            job.completedAt
        )
        : job.completedAt}

</div>

`
                        : ""
                }

            </div>


            <!-- ================================= -->
            <!-- ACTION -->
            <!-- ================================= -->

            <div class="col-md-4 text-md-end">

                ${this.renderButton(job)}

            </div>

        </div>

    </div>

</div>

`;

    },


    renderButton(job) {

        const driver =
            this.isDriver();


        switch (job.status) {

            case "ASSIGNED":

                return `

<button
    class="btn btn-primary acceptJob"
    data-id="${job.assignmentId}">

    <i class="bi bi-hand-thumbs-up me-1"></i>

    ${
        driver
            ? "Take Job"
            : "Accept Job"
    }

</button>

`;


            case "ACCEPTED":

                return `

<button
    class="btn btn-warning startJob"
    data-id="${job.assignmentId}">

    <i class="bi bi-play-fill me-1"></i>

    ${
        driver
            ? "Start Trip"
            : "Start Work"
    }

</button>

`;


            case "IN_PROGRESS":

                return `

<button
    class="btn btn-success completeJob"
    data-id="${job.assignmentId}">

    <i class="bi bi-check-circle me-1"></i>

    ${
        driver
            ? "Complete Trip"
            : "Complete Job"
    }

</button>

`;


            case "COMPLETED":

                return `

<div>

    <span class="badge bg-success fs-6">

        <i class="bi bi-check-circle me-1"></i>

        Completed

    </span>

</div>

`;


            default:

                return `

<span class="badge bg-secondary">

    ${job.status ?? "Unknown"}

</span>

`;

        }

    }

};