window.RepairStep = {

    render() {

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Repair Tasks

        </h4>

        <small class="text-muted">

            Complete all repair activities before Quality Check.

        </small>

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

                Previous

            </button>

            <div>

                <button
                    id="startRepairBtn"
                    class="btn btn-warning">

                    Start Repair

                </button>

                <button
                    id="completeRepairBtn"
                    class="btn btn-success">

                    Complete Repair

                </button>

            </div>

        </div>

    </div>

</div>

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

        return `

<table class="table table-hover table-bordered">

    <thead class="table-light">

        <tr>

            <th>#</th>

            <th>Repair Task</th>

            <th>Status</th>

        </tr>

    </thead>

    <tbody>

        ${tasks.map((task,index)=>`

<tr>

<td>${index+1}</td>

<td>${task.description}</td>

<td>

<span class="badge bg-warning">

${task.status}

</span>

</td>

</tr>

`).join("")}

    </tbody>

</table>

`;

    },

    async refresh() {

        await WorkflowService.loadRepairTasks();

        document.getElementById(
            "repairTaskContainer"
        ).innerHTML = this.renderTasks();

    },

    bindEvents() {

        this.refresh();

        document
            .getElementById("startRepairBtn")
            ?.addEventListener(
                "click",
                async ()=>{

                    try {

                        await WorkflowService.startRepair();

                        alert("Repair started successfully.");

                        await this.refresh();

                    } catch (e) {

                        alert(e.message);

                    }

                });

        document
            .getElementById("completeRepairBtn")
            ?.addEventListener(
                "click",
                async ()=>{

                    await WorkflowService.completeRepair();

                    alert(
                        "Repair completed successfully."
                    );

                    Workflow.nextStep();

                });

    }

};