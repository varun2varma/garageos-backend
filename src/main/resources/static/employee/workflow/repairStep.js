window.RepairStep = {

    render() {

        return `

<div class="card shadow-sm border-0">

    <div class="card-header bg-white">

        <div class="d-flex justify-content-between align-items-center">

            <div>

                <h4 class="mb-1">

                    <i class="bi bi-tools me-2"></i>

                    Repair Tasks

                </h4>

                <small class="text-muted">

                    Complete all repair activities before Quality Check.

                </small>

            </div>

            <div class="text-end">

                <div
                    id="repairProgressText"
                    class="fw-bold">

                    0 / 0 Completed

                </div>

            </div>

        </div>

        <div class="progress mt-3"
             style="height:10px;">

            <div
                id="repairProgressBar"
                class="progress-bar bg-success"
                style="width:0%">

            </div>

        </div>

    </div>

    <div class="card-body">

        <div id="repairTaskContainer">

            ${this.renderTasks()}

        </div>

        <hr>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                <i class="bi bi-arrow-left"></i>

                Previous

            </button>

            <button
                id="nextRepairBtn"
                class="btn btn-success"
                disabled>

                Proceed to Quality Check

                <i class="bi bi-arrow-right"></i>

            </button>

        </div>

    </div>

</div>

${this.renderAssignModal()}

`;

    },

    renderTasks() {

        const tasks =
            WorkflowHelper.state.repairTasks || [];

        if (tasks.length === 0) {

            return `

<div class="alert alert-warning">

    No Repair Tasks Available.

</div>

`;

        }

        return tasks.map(task => `

<div class="card mb-3 shadow-sm">

    <div class="card-body">

        <div class="d-flex justify-content-between">

            <div>

                <h5 class="mb-2">

                    <i class="bi bi-wrench-adjustable-circle me-2 text-primary"></i>

                    ${task.description}

                </h5>

                <div class="mb-2">

                    ${this.renderStatus(task.status)}

                </div>

                <div class="small text-muted">

                    <strong>Technician :</strong>

                    ${task.technicianName || "Not Assigned"}

                </div>

                ${task.startedAt ? `

<div class="small text-muted">

    <strong>Started :</strong>

    ${WorkflowHelper.formatDateTime(task.startedAt)}

</div>

` : ""}

                ${task.completedAt ? `

<div class="small text-muted">

    <strong>Completed :</strong>

    ${WorkflowHelper.formatDateTime(task.completedAt)}

</div>

` : ""}

            </div>

            <div class="text-end">

                ${this.renderActions(task)}

            </div>

        </div>

    </div>

</div>

`).join("");

    },

    renderStatus(status) {

        let badge = "secondary";

        switch (status) {

            case "PENDING":
                badge = "secondary";
                break;

            case "ASSIGNED":
                badge = "info";
                break;

            case "IN_PROGRESS":
                badge = "warning";
                break;

            case "COMPLETED":
                badge = "success";
                break;

            case "ON_HOLD":
                badge = "danger";
                break;

            default:
                badge = "secondary";

        }

        return `

<span class="badge bg-${badge} fs-6">

    ${status.replaceAll("_"," ")}

</span>

`;

    },

        renderActions(task) {

            switch (task.status) {

                case "PENDING":

                    return `

    <button
        class="btn btn-sm btn-primary assign-btn"
        data-id="${task.id}">

        <i class="bi bi-person-plus"></i>

        Assign Technician

    </button>

    `;

                case "ASSIGNED":

                    return `

    <div class="mb-2">

    <button
        class="btn btn-warning btn-sm start-btn"
        data-id="${task.id}">

        <i class="bi bi-play-fill"></i>

        Start Repair

    </button>

    </div>

    `;

                case "IN_PROGRESS":

                    return `

    <div class="mb-2">

    <button
        class="btn btn-success btn-sm complete-btn"
        data-id="${task.id}">

        <i class="bi bi-check-circle"></i>

        Complete Repair

    </button>

    </div>

    `;

                case "COMPLETED":

                    return `

    <span class="badge bg-success p-2">

    <i class="bi bi-check-circle-fill"></i>

    Completed

    </span>

    `;

                default:

                    return "";

            }

        },

        renderAssignModal() {

            return `

    <div
        class="modal fade"
        id="assignTechnicianModal"
        tabindex="-1">

        <div class="modal-dialog">

            <div class="modal-content">

                <div class="modal-header">

                    <h5 class="modal-title">

                        Assign Technician

                    </h5>

                    <button
                        type="button"
                        class="btn-close"
                        data-bs-dismiss="modal">

                    </button>

                </div>

                <div class="modal-body">

                    <input
                        id="technicianName"
                        class="form-control"
                        placeholder="Enter Technician Name"/>

                    <input
                        id="repairTaskId"
                        type="hidden"/>

                </div>

                <div class="modal-footer">

                    <button
                        class="btn btn-secondary"
                        data-bs-dismiss="modal">

                        Cancel

                    </button>

                    <button
                        id="saveTechnicianBtn"
                        class="btn btn-primary">

                        Assign

                    </button>

                </div>

            </div>

        </div>

    </div>

    `;

        },

        async refresh() {

        console.table(
            WorkflowHelper.state.repairTasks.map(t => ({
                id: t.id,
                status: t.status
            }))
        );

            WorkflowHelper.state.repairTasks;

//            await WorkflowService.loadRepairTasks();

            const container =
                document.getElementById(
                    "repairTaskContainer");

            if (container) {

                container.innerHTML =
                    this.renderTasks();

            }

            const tasks =
                WorkflowHelper.state.repairTasks || [];

            const total =
                tasks.length;

            const completed =
                tasks.filter(
                    x => x.status === "COMPLETED"
                ).length;

            const percent =
                total === 0
                    ? 0
                    : Math.round(
                        completed * 100 / total);

            const progressBar =
                document.getElementById(
                    "repairProgressBar");

            if (progressBar) {

                progressBar.style.width =
                    percent + "%";

                progressBar.innerHTML =
                    percent + "%";

            }

            const progressText =
                document.getElementById(
                    "repairProgressText");

            if (progressText) {

                progressText.innerHTML =
                    `${completed} / ${total} Completed`;

            }

            const nextBtn =
                document.getElementById(
                    "nextRepairBtn");

            if (nextBtn) {

                nextBtn.disabled =
                    !(total > 0 && completed === total);

            }

        },

            bindEvents() {

                this.refresh();

                document
                    .getElementById("previousBtn")
                    ?.addEventListener(
                        "click",
                        () => Workflow.previousStep()
                    );

                document
                    .getElementById("nextRepairBtn")
                    ?.addEventListener(
                        "click",
                        () => Workflow.nextStep()
                    );

                document.addEventListener(
                    "click",
                    async (e) => {

                        /*
                         * Assign Technician
                         */
                        if (e.target.closest(".assign-btn")) {

                            const btn =
                                e.target.closest(".assign-btn");

                            document.getElementById(
                                "repairTaskId"
                            ).value = btn.dataset.id;

                            document.getElementById(
                                "technicianName"
                            ).value = "";

                            const modal =
                                new bootstrap.Modal(
                                    document.getElementById(
                                        "assignTechnicianModal"
                                    )
                                );

                            modal.show();

                            return;

                        }

                        /*
                         * Start Repair
                         */
                        if (e.target.closest(".start-btn")) {

                            try {

                                const btn =
                                    e.target.closest(".start-btn");

                                await WorkflowService.startRepair(
                                    Number(btn.dataset.id)
                                );

                                await this.refresh();

                                alert(
                                    "Repair started successfully."
                                );

                            } catch (error) {

                                Toast.error(
                                    error.message ||
                                    "Unable to start repair."
                                );

                            }

                            return;

                        }

                        /*
                         * Complete Repair
                         */
                        if (e.target.closest(".complete-btn")) {

                            try {

                                const btn =
                                    e.target.closest(".complete-btn");

                                await WorkflowService.completeRepair(
                                    Number(btn.dataset.id)
                                );

                                await this.refresh();

                                alert(
                                    "Repair completed successfully."
                                );

                            } catch (error) {

                                Toast.error(
                                    error.message ||
                                    "Unable to complete repair."
                                );

                            }

                        }

                    });

                document
                    .getElementById("saveTechnicianBtn")
                    ?.addEventListener(
                        "click",
                        async () => {

                            const repairTaskId =
                                Number(
                                    document.getElementById(
                                        "repairTaskId"
                                    ).value
                                );

                            const technicianName =
                                document.getElementById(
                                    "technicianName"
                                ).value
                                .trim();

                            if (!technicianName) {

                                alert("Please enter technician name.");

                                return;

                            }

                            try {

                                await WorkflowService.assignTechnician(
                                    repairTaskId,
                                    technicianName
                                );

                                bootstrap.Modal
                                    .getInstance(
                                        document.getElementById(
                                            "assignTechnicianModal"
                                        )
                                    )
                                    ?.hide();

                                await this.refresh();

                                alert("Technician assigned successfully.");

                            } catch (error) {

                                alert(
                                    error.message ||
                                    "Unable to assign technician."
                                );

                            }

                        });

            }

        };