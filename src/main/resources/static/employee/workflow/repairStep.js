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

                    Assign technicians and monitor repair progress.

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
                style="width:0%;">

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

                    ${task.description || "-"}

                </h5>


                <div class="mb-2">

                    ${this.renderStatus(task.status)}

                </div>


                <div class="small text-muted">

                    <strong>Technician :</strong>

                    ${task.technicianName || "Not Assigned"}

                </div>


                ${
                    task.assignedAt
                        ? `

<div class="small text-muted">

    <strong>Assigned :</strong>

    ${WorkflowHelper.formatDateTime(task.assignedAt)}

</div>

`
                        : ""
                }


                ${
                    task.acceptedAt
                        ? `

<div class="small text-muted">

    <strong>Accepted :</strong>

    ${WorkflowHelper.formatDateTime(task.acceptedAt)}

</div>

`
                        : ""
                }


                ${
                    task.startedAt
                        ? `

<div class="small text-muted">

    <strong>Started :</strong>

    ${WorkflowHelper.formatDateTime(task.startedAt)}

</div>

`
                        : ""
                }


                ${
                    task.completedAt
                        ? `

<div class="small text-muted">

    <strong>Completed :</strong>

    ${WorkflowHelper.formatDateTime(task.completedAt)}

</div>

`
                        : ""
                }

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


            case "QC_PENDING":

                badge = "dark";

                break;


            case "QC_FAILED":

                badge = "danger";

                break;


            case "REWORK":

                badge = "warning";

                break;


            case "CANCELLED":

                badge = "secondary";

                break;


            default:

                badge = "secondary";

        }


        return `

<span class="badge bg-${badge} fs-6">

    ${(status || "UNASSIGNED")
        .replaceAll("_", " ")}

</span>

`;

    },


    renderActions(task) {

        /*
        -----------------------------------------
        No Assignment
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
        Existing Assignment
        -----------------------------------------
        */

        switch (task.status) {

            case "ASSIGNED":

                return `

<button
    class="btn btn-outline-primary btn-sm assign-btn"
    data-id="${task.id}">

    <i class="bi bi-arrow-repeat"></i>

    Reassign

</button>

`;

            case "ACCEPTED":

                return `

<div>

    <span class="badge bg-primary">

        Accepted By Technician

    </span>

</div>

<button
    class="btn btn-outline-primary btn-sm mt-2 assign-btn"
    data-id="${task.id}">

    Reassign

</button>

`;

            case "IN_PROGRESS":

                return `

<div>

    <span class="badge bg-warning text-dark">

        Repair In Progress

    </span>

</div>

`;

            case "COMPLETED":

                return `

<span class="badge bg-success">

    <i class="bi bi-check-circle me-1"></i>

    Completed

</span>

`;

            case "ON_HOLD":

                return `

<span class="badge bg-danger">

    On Hold

</span>

`;

            case "REWORK":

                return `

<span class="badge bg-warning text-dark">

    Rework Required

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
                        min="0"
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
                    type="button"
                    class="btn btn-secondary"
                    data-bs-dismiss="modal">

                    Cancel

                </button>


                <button
                    type="button"
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

        try {

            /*
            -----------------------------------------
            Load assignments
            -----------------------------------------
            */

            const assignments =
                await JobAssignmentService.getByJobCard(
                    WorkflowHelper.state.jobCardId
                );


            WorkflowHelper.state.assignments =
                assignments || [];


            /*
            -----------------------------------------
            Repair tasks
            -----------------------------------------
            */

            const tasks =
                WorkflowHelper.state.repairTasks || [];


            /*
            -----------------------------------------
            Merge assignment information
            -----------------------------------------
            */

            tasks.forEach(task => {

                const assignment =
                    assignments.find(
                        x =>
                            x.estimateItemId ===
                            task.estimateItemId
                    );


                /*
                No assignment
                */

                if (!assignment) {

                    task.assignmentId = null;

                    task.status = null;

                    task.technicianId = null;

                    task.technicianName = null;

                    task.assignedAt = null;

                    task.acceptedAt = null;

                    task.startedAt = null;

                    task.completedAt = null;

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


                task.acceptedAt =
                    assignment.acceptedAt;


                task.startedAt =
                    assignment.startedAt;


                task.completedAt =
                    assignment.completedAt;


                task.estimatedHours =
                    assignment.estimatedHours;


                task.actualHours =
                    assignment.actualHours;


                task.remarks =
                    assignment.remarks;

            });


            /*
            -----------------------------------------
            Render
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
            Progress
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
                        completed / total * 100
                    );


            const progressBar =
                document.getElementById(
                    "repairProgressBar"
                );


            if (progressBar) {

                progressBar.style.width =
                    `${percentage}%`;

                progressBar.innerHTML =
                    `${percentage}%`;

            }


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
            Enable QC
            -----------------------------------------
            */

            const nextButton =
                document.getElementById(
                    "nextRepairBtn"
                );


            if (nextButton) {

                nextButton.disabled =
                    !(
                        total > 0 &&
                        completed === total
                    );

            }

        } catch (error) {

            console.error(
                "Unable to refresh repair tasks",
                error
            );

            const container =
                document.getElementById(
                    "repairTaskContainer"
                );


            if (container) {

                container.innerHTML = `

<div class="alert alert-danger">

    Unable to load repair assignments.

</div>

`;

            }

        }

    },


    async openAssignModal(repairTaskId) {

        const task =
            (WorkflowHelper.state.repairTasks || [])
                .find(
                    x =>
                        x.id === repairTaskId
                );


        if (!task) {

            alert("Repair task not found.");

            return;

        }


        document.getElementById(
            "repairTaskId"
        ).value = repairTaskId;


        const technicianSelect =
            document.getElementById(
                "technicianId"
            );


        technicianSelect.innerHTML = `

<option value="">

    Loading technicians...

</option>

`;


        try {

            const response =
                await UserService.getTechnicians();


            const technicians =
                response?.data || response || [];


            technicianSelect.innerHTML = `

<option value="">

    Select Technician

</option>

`;


            technicians.forEach(user => {

                const selected =
                    task.technicianId === user.id
                        ? "selected"
                        : "";


                technicianSelect.innerHTML += `

<option
    value="${user.id}"
    ${selected}>

    ${user.firstName || ""}
    ${user.lastName || ""}

</option>

`;

            });


        } catch (error) {

            console.error(error);

            technicianSelect.innerHTML = `

<option value="">

    Unable to load technicians

</option>

`;

            alert(
                error.message ||
                "Unable to load technicians."
            );

            return;

        }


        document.getElementById(
            "estimatedHours"
        ).value =
            task.estimatedHours || "";


        document.getElementById(
            "assignmentRemarks"
        ).value =
            task.remarks || "";


        const modalElement =
            document.getElementById(
                "assignTechnicianModal"
            );


        new bootstrap.Modal(
            modalElement
        ).show();

    },


    async saveAssignment() {

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


        const estimatedHoursValue =
            document.getElementById(
                "estimatedHours"
            ).value;


        const estimatedHours =
            estimatedHoursValue
                ? Number(estimatedHoursValue)
                : null;


        const remarks =
            document.getElementById(
                "assignmentRemarks"
            ).value.trim();


        if (!technicianId) {

            alert(
                "Please select a technician."
            );

            return;

        }


        const task =
            (WorkflowHelper.state.repairTasks || [])
                .find(
                    x =>
                        x.id === repairTaskId
                );


        if (!task) {

            alert(
                "Repair task not found."
            );

            return;

        }


        try {

            /*
            -----------------------------------------
            Existing assignment -> REASSIGN
            -----------------------------------------
            */

            if (task.assignmentId) {

                await JobAssignmentService.reassign(

                    task.assignmentId,

                    {

                        employeeId:
                            technicianId,

                        assignmentType:
                            "TECHNICIAN",

                        estimatedHours,

                        remarks

                    }

                );

            }


            /*
            -----------------------------------------
            No assignment -> ASSIGN
            -----------------------------------------
            */

            else {

                await JobAssignmentService.assign({

                    jobCardId:
                        WorkflowHelper.state.jobCardId,

                    estimateItemId:
                        task.estimateItemId,

                    employeeId:
                        technicianId,

                    assignmentType:
                        "TECHNICIAN",

                    estimatedHours,

                    remarks

                });

            }


            /*
            -----------------------------------------
            Close modal
            -----------------------------------------
            */

            const modalElement =
                document.getElementById(
                    "assignTechnicianModal"
                );


            bootstrap.Modal
                .getInstance(modalElement)
                ?.hide();


            /*
            -----------------------------------------
            Reload assignments
            -----------------------------------------
            */

            await this.refresh();


            alert(
                "Technician assignment saved successfully."
            );


        } catch (error) {

            console.error(
                "Assignment error",
                error
            );


            alert(
                error.message ||
                "Unable to save technician assignment."
            );

        }

    },


    bindEvents() {

        /*
        -----------------------------------------
        Initial load
        -----------------------------------------
        */

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
                () =>
                    Workflow.previousStep()
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
                () =>
                    Workflow.nextStep()
            );


        /*
        -----------------------------------------
        Assign / Reassign
        -----------------------------------------
        */

        document
            .getElementById("repairTaskContainer")
            ?.addEventListener(
                "click",
                async (event) => {

                    const button =
                        event.target.closest(
                            ".assign-btn"
                        );


                    if (!button) {

                        return;

                    }


                    const repairTaskId =
                        Number(
                            button.dataset.id
                        );


                    await this.openAssignModal(
                        repairTaskId
                    );

                }
            );


        /*
        -----------------------------------------
        Save Assignment
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

                        const task =
                            WorkflowHelper.state.repairTasks.find(
                                x => x.id === repairTaskId
                            );


                        if (!task) {

                            throw new Error(
                                "Repair task not found."
                            );

                        }


                        /*
                        =========================================
                        REASSIGN EXISTING ASSIGNMENT
                        =========================================
                        */

                        if (task.assignmentId) {

                            await JobAssignmentService.reassign(

                                task.assignmentId,

                                {

                                    employeeId:
                                        technicianId,

                                    assignmentType:
                                        "TECHNICIAN",

                                    estimatedHours,

                                    remarks

                                }

                            );

                        }


                        /*
                        =========================================
                        CREATE NEW ASSIGNMENT
                        =========================================
                        */

                        else {

                            await JobAssignmentService.assign({

                                jobCardId:
                                    WorkflowHelper.state.jobCardId,

                                estimateItemId:
                                    task.estimateItemId,

                                employeeId:
                                    technicianId,

                                assignmentType:
                                    "TECHNICIAN",

                                estimatedHours,

                                remarks

                            });

                        }


                        /*
                        =========================================
                        CLOSE MODAL
                        =========================================
                        */

                        bootstrap.Modal
                            .getInstance(

                                document.getElementById(
                                    "assignTechnicianModal"
                                )

                            )
                            ?.hide();


                        /*
                        =========================================
                        REFRESH
                        =========================================
                        */

                        await this.refresh();


                        alert(
                            task.assignmentId
                                ? "Technician reassigned successfully."
                                : "Technician assigned successfully."
                        );


                    }
                    catch (error) {

                        console.error(
                            "Technician assignment error:",
                            error
                        );

                        alert(

                            error.message ||

                            "Unable to assign technician."

                        );

                    }

                }

            );

    }

};