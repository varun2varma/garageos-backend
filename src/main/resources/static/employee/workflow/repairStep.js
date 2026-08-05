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

                    Assign technicians and complete all repair activities before Quality Check.

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

        <div
            class="progress mt-3"
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

<div class="text-center py-5">

    <i
        class="bi bi-tools"
        style="font-size:60px;color:#9CA3AF;">

    </i>

    <h5 class="mt-3">

        No Repair Tasks

    </h5>

    <p class="text-muted mb-0">

        No approved estimate items are available.

    </p>

</div>

`;

        }

        return tasks.map(task => `

<div class="card mb-3 shadow-sm border-0">

    <div class="card-body">

        <div class="row align-items-center">

            <div class="col-lg-7">

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

                ${task.assignedAt ? `

<div class="small text-muted">

    <strong>Assigned :</strong>

    ${WorkflowHelper.formatDateTime(task.assignedAt)}

</div>

` : ""}

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

            <div class="col-lg-5 text-end">

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

            case "ACCEPTED":
                badge = "primary";
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

    ${status.replaceAll("_", " ")}

</span>

`;

    },

    renderActions(task) {

        /*
        -----------------------------------------
        No Assignment Yet
        -----------------------------------------
        */

        if (!task.assignmentId) {

            return `

    <button
        class="btn btn-primary btn-sm assign-btn"
        data-id="${task.id}">

        <i class="bi bi-person-plus"></i>

        Assign Technician

    </button>

    `;

        }

        /*
        -----------------------------------------
        Already Assigned
        -----------------------------------------
        */

        switch (task.status) {

            case "ASSIGNED":

                return `

    <button
        class="btn btn-outline-primary btn-sm assign-btn"
        data-id="${task.id}">

        Reassign

    </button>

    `;

            case "ACCEPTED":

                return `

    <span class="badge bg-primary">

    Accepted By Technician

    </span>

    `;

            case "IN_PROGRESS":

                return `

    <span class="badge bg-warning">

    Repair In Progress

    </span>

    `;

            case "COMPLETED":

                return `

    <span class="badge bg-success">

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

                    <i class="bi bi-person-plus me-2"></i>

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
                    id="repairTaskId"
                    type="hidden">

                <div class="mb-3">

                    <label class="form-label">

                        Technician

                    </label>

                    <select
                        id="technicianId"
                        class="form-select">

                        <option value="">

                            Select Technician

                        </option>

                    </select>

                </div>

                <div class="mb-3">

                    <label class="form-label">

                        Estimated Hours

                    </label>

                    <input
                        id="estimatedHours"
                        type="number"
                        step="0.5"
                        class="form-control"
                        placeholder="Estimated Hours">

                </div>

                <div class="mb-3">

                    <label class="form-label">

                        Remarks

                    </label>

                    <textarea
                        id="assignmentRemarks"
                        class="form-control"
                        rows="3"
                        placeholder="Optional Remarks">

                    </textarea>

                </div>

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

                    <i class="bi bi-check-circle me-1"></i>

                    Assign Technician

                </button>

            </div>

        </div>

    </div>

</div>

`;

    },


    async refresh() {

        /*
        -----------------------------------------
        Load Existing Assignments
        -----------------------------------------
        */

        const assignments =
            await JobAssignmentService.getByJobCard(
                WorkflowHelper.state.jobCardId
            );

        WorkflowHelper.state.assignments = assignments;

        /*
        -----------------------------------------
        Merge Assignment Data Into Repair Tasks
        -----------------------------------------
        */

        const tasks =
            WorkflowHelper.state.repairTasks || [];

        tasks.forEach(task => {

            const assignment =
                assignments.find(
                    x => x.estimateItemId === task.id
                );

            if (!assignment) {
                return;
            }

            task.assignmentId =
                assignment.id;

            task.status =
                assignment.status;

            task.technicianId =
                assignment.employeeId;

            task.technicianName =
                assignment.employeeName;

            task.assignedAt =
                assignment.assignedAt;

            task.startedAt =
                assignment.startedAt;

            task.completedAt =
                assignment.completedAt;

            task.estimatedHours =
                assignment.estimatedHours;

            task.remarks =
                assignment.remarks;

        });

        /*
        -----------------------------------------
        Render Repair Tasks
        -----------------------------------------
        */

        const container =
            document.getElementById(
                "repairTaskContainer"
            );

        if (container) {

            container.innerHTML =
                this.renderTasks();

        }

        /*
        -----------------------------------------
        Calculate Progress
        -----------------------------------------
        */

        const total =
            tasks.length;

        const completed =
            tasks.filter(
                task =>
                    task.status === "COMPLETED"
            ).length;

        const percentage =
            total === 0
                ? 0
                : Math.round(
                    (completed / total) * 100
                );

        /*
        -----------------------------------------
        Progress Bar
        -----------------------------------------
        */

        const progressBar =
            document.getElementById(
                "repairProgressBar"
            );

        if (progressBar) {

            progressBar.style.width =
                percentage + "%";

            progressBar.innerHTML =
                percentage + "%";

        }

        /*
        -----------------------------------------
        Progress Text
        -----------------------------------------
        */

        const progressText =
            document.getElementById(
                "repairProgressText"
            );

        if (progressText) {

            progressText.innerHTML =
                `${completed} / ${total} Completed`;

        }

        /*
        -----------------------------------------
        Enable Next Step
        -----------------------------------------
        */

        const nextButton =
            document.getElementById(
                "nextRepairBtn"
            );

        if (nextButton) {

            nextButton.disabled =
                !(total > 0 && completed === total);

        }

    },

    bindEvents() {

        this.refresh();

        /*
        -----------------------------------------
        Previous
        -----------------------------------------
        */

        document
            .getElementById("previousBtn")
            ?.addEventListener(
                "click",
                () => Workflow.previousStep()
            );

        /*
        -----------------------------------------
        Next
        -----------------------------------------
        */

        document
            .getElementById("nextRepairBtn")
            ?.addEventListener(
                "click",
                () => Workflow.nextStep()
            );

        /*
        -----------------------------------------
        Repair Actions
        -----------------------------------------
        */

        document.addEventListener(

            "click",

            async (e) => {

                /*
                =============================
                Assign Technician
                =============================
                */

                const assignBtn =
                    e.target.closest(".assign-btn");

                if (assignBtn) {

                    const repairTaskId =
                        Number(assignBtn.dataset.id);

                    document.getElementById(
                        "repairTaskId"
                    ).value = repairTaskId;

                    const technicianSelect =
                        document.getElementById(
                            "technicianId"
                        );

                    technicianSelect.innerHTML = `

<option value="">

    Loading...

</option>

`;

                    try {

                        console.log("Loading technicians...");

                        const response =
                            await UserService.getTechnicians();

                        console.log("Technician API Response:", response);

                        const technicians =
                            response.data || response;

                        console.log("Technicians:", technicians);

                        technicianSelect.innerHTML = `

<option value="">

    Select Technician

</option>

`;

                        technicians.forEach(user => {

                            technicianSelect.innerHTML += `

<option value="${user.id}">

    ${user.firstName} ${user.lastName}

</option>

`;

                        });

                    }
                    catch (error) {

                        console.error(error);

                        alert(error.message);

                        return;

                    }

                    document.getElementById(
                        "estimatedHours"
                    ).value = "";

                    document.getElementById(
                        "assignmentRemarks"
                    ).value = "";

                    new bootstrap.Modal(

                        document.getElementById(
                            "assignTechnicianModal"
                        )

                    ).show();

                    return;

                }

                /*
                =============================
                Start Repair
                =============================
                */

                const startBtn =
                    e.target.closest(".start-btn");

                if (startBtn) {

                    try {

                        const repairTaskId =
                            Number(startBtn.dataset.id);

                        await WorkflowService.startRepair(
                            repairTaskId
                        );

                        const task =
                            WorkflowHelper.state.repairTasks.find(
                                x => x.id === repairTaskId
                            );

                        if (task) {

                            task.status =
                                "IN_PROGRESS";

                            task.startedAt =
                                new Date().toISOString();

                        }

                        await this.refresh();

                        alert(
                            "Repair started successfully."
                        );

                    }
                    catch (error) {

                        alert(

                            error.message ||

                            "Unable to start repair."

                        );

                    }

                    return;

                }

                /*
                =============================
                Complete Repair
                =============================
                */

                const completeBtn =
                    e.target.closest(".complete-btn");

                if (completeBtn) {

                    try {

                        const repairTaskId =
                            Number(
                                completeBtn.dataset.id
                            );

                        await WorkflowService.completeRepair(
                            repairTaskId
                        );

                        const task =
                            WorkflowHelper.state.repairTasks.find(
                                x => x.id === repairTaskId
                            );

                        if (task) {

                            task.status =
                                "COMPLETED";

                            task.completedAt =
                                new Date().toISOString();

                        }

                        await this.refresh();

                        alert(
                            "Repair completed successfully."
                        );

                    }
                    catch (error) {

                        alert(

                            error.message ||

                            "Unable to complete repair."

                        );

                    }

                    return;

                }

            }

        );

                /*
                -----------------------------------------
                Save Technician Assignment
                -----------------------------------------
                */

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

                            const technicianId =
                                Number(
                                    document.getElementById(
                                        "technicianId"
                                    ).value
                                );

                            const estimatedHours =
                                Number(
                                    document.getElementById(
                                        "estimatedHours"
                                    ).value
                                );

                            const remarks =
                                document.getElementById(
                                    "assignmentRemarks"
                                ).value;

                            if (!technicianId) {

                                alert(
                                    "Please select a technician."
                                );

                                return;

                            }

                            try {

                                /*
                                -------------------------------------
                                Save Job Assignment
                                -------------------------------------
                                */

                                const task =
                                    WorkflowHelper.state.repairTasks.find(
                                        x => x.id === repairTaskId
                                    );

                                if (task.assignmentId) {

                                    await JobAssignmentService.reassign(

                                        task.assignmentId,

                                        {
                                            employeeId: technicianId,
                                            estimatedHours,
                                            remarks
                                        }

                                    );

                                }
                                else {

                                    await JobAssignmentService.assign({

                                        jobCardId:
                                            WorkflowHelper.state.jobCardId,

                                        estimateItemId:
                                            repairTaskId,

                                        employeeId:
                                            technicianId,

                                        estimatedHours,

                                        remarks

                                    });

                                }

                                /*
                                -------------------------------------
                                Update UI
                                -------------------------------------
                                */

                                bootstrap.Modal
                                    .getInstance(

                                        document.getElementById(
                                            "assignTechnicianModal"
                                        )

                                    )
                                    ?.hide();

                                await this.refresh();

                                alert(
                                    "Technician assigned successfully."
                                );

                            }
                            catch (error) {

                                console.error(error);

                                alert(

                                    error.message ||

                                    "Unable to assign technician."

                                );

                            }

                        }

                    );

            }

        };