window.EstimateItemStep = {

    currentComplaintIndex: 0,

    getCurrentInspection() {

        return WorkflowHelper.state.inspections[

            this.currentComplaintIndex

        ];

    },

    render() {

        const inspections =
            WorkflowHelper.state.inspections || [];

        if (inspections.length === 0) {

            return `

<div class="alert alert-warning">

    No inspection records found.

</div>

`;

        }

        const inspection =
            inspections[this.currentComplaintIndex];

        if (!inspection) {

            this.currentComplaintIndex = 0;

            if (inspections.length === 0) {

                return `
        <div class="alert alert-warning">
        No inspection records found.
        </div>
        `;
            }

            return this.render();

        }

        return `

<div class="container-fluid">

<div class="card shadow">

<div class="card-header d-flex justify-content-between align-items-center">

<div>

<h3 class="mb-0">

Estimate Preparation

</h3>

<small class="text-muted">

Complaint ${this.currentComplaintIndex + 1}
of
${inspections.length}

</small>

</div>

<div>

<span class="badge bg-primary">

${inspection.status}

</span>

</div>

</div>

<div class="card-body">

${this.renderComplaintCard(inspection)}

<hr>

${this.renderPartsSection()}

<hr>

${this.renderLabourSection()}

<hr>

<div id="estimateItemsContainer">

    ${this.renderCurrentItems()}
    ${this.renderTotals()}

</div>

<hr>

<div class="d-flex justify-content-between">

<button

id="previousComplaintBtn"

class="btn btn-outline-secondary"

>

← Previous Complaint

</button>

<button

id="nextComplaintBtn"

class="btn btn-success"

>

${this.nextButtonText()}

</button>

</div>

</div>

</div>

</div>

`;

    },

    renderComplaintCard(inspection) {

        return `

<div class="card border-primary">

<div class="card-header bg-primary text-white">

Inspection Details

</div>

<div class="card-body">

<div class="row">

<div class="col-md-4">

<label class="fw-bold">

Customer Complaint

</label>

<p>

${inspection.complaint}

</p>

</div>

<div class="col-md-4">

<label class="fw-bold">

Inspection Findings

</label>

<p>

${inspection.inspectionNotes}

</p>

</div>

<div class="col-md-4">

<label class="fw-bold">

Recommended Work

</label>

<p class="text-success fw-bold">

${inspection.recommendedWork}

</p>

</div>

</div>

</div>

</div>

`;

    },

    renderPartsSection() {

        return `

<h5>

Parts

</h5>

<div class="row g-2">

<div class="col-md-5">

<input

id="partDescription"

class="form-control"

placeholder="Part Description"

>

</div>

<div class="col-md-2">

<input

id="partQty"

type="number"

value="1"

class="form-control"

>

</div>

<div class="col-md-3">

<input

id="partPrice"

type="number"

class="form-control"

placeholder="Unit Price"

>

</div>

<div class="col-md-2 d-grid">

<button

id="addPartBtn"

class="btn btn-primary"

>

+ Part

</button>

</div>

</div>

`;

    },

    renderLabourSection() {

        return `

<h5 class="mt-4">

Labour

</h5>

<div class="row g-2">

<div class="col-md-8">

<input

id="labourDescription"

class="form-control"

placeholder="Labour Description"

>

</div>

<div class="col-md-2">

<input

id="labourPrice"

type="number"

class="form-control"

placeholder="Amount"

>

</div>

<div class="col-md-2 d-grid">

<button

id="addLabourBtn"

class="btn btn-success"

>

+ Labour

</button>

</div>

</div>

`;

    },

    renderCurrentItems() {

        const inspection = this.getCurrentInspection();

        const complaintId = inspection.complaintId;

        const items =
            WorkflowHelper.state.estimateItems.filter(

                item => Number(item.complaintId) === Number(complaintId)

            );

        if (items.length === 0) {

            return `

    <div class="alert alert-light text-center">

    No estimate items added for this complaint.

    </div>

    `;

        }

        let rows = "";

        items.forEach(item => {

            rows += `

    <tr>

    <td>

    ${item.itemType}

    </td>

    <td>

    ${item.description}

    </td>

    <td>

    ${item.quantity}

    </td>

    <td>

    ₹ ${item.unitPrice}

    </td>

    <td>

    ₹ ${item.totalPrice}

    </td>

    <td>

    <button

    class="btn btn-sm btn-danger"

    onclick="EstimateItemStep.removeEstimateItem(${item.id})"

    >

    Delete

    </button>

    </td>

    </tr>

    `;

        });

        return `

    <table class="table table-bordered table-hover">

    <thead>

    <tr>

    <th>Type</th>

    <th>Description</th>

    <th>Qty</th>

    <th>Price</th>

    <th>Total</th>

    <th></th>

    </tr>

    </thead>

    <tbody>

    ${rows}

    </tbody>

    </table>

    `;

    },

    bindEvents() {

        document
            .getElementById("previousComplaintBtn")
            ?.addEventListener(
                "click",
                () => this.previousComplaint()
            );

        document
            .getElementById("nextComplaintBtn")
            ?.addEventListener(
                "click",
                () => this.nextComplaint()
            );

        document
            .getElementById("addPartBtn")
            ?.addEventListener(
                "click",
                () => this.addPart()
            );

        document
            .getElementById("addLabourBtn")
            ?.addEventListener(
                "click",
                () => this.addLabour()
            );

    },

    previousComplaint() {

        if (this.currentComplaintIndex > 0) {

            this.currentComplaintIndex--;

            this.refresh();

        }

    },

    async nextComplaint() {

        const inspections =
            WorkflowHelper.state.inspections || [];

        if (

            this.currentComplaintIndex <

            inspections.length - 1

        ) {

            this.currentComplaintIndex++;

            this.refresh();

            return;

        }

        try {

                const estimateId = WorkflowHelper.state.estimate.id;

                await WorkflowService.finishEstimate(estimateId);

                Workflow.nextStep();

            } catch (e) {
                console.error(e);
                alert("Unable to finish estimate.");
            }
//        this.showEstimateSummary();
//        Workflow.nextStep();
    },

    refresh() {

        document.getElementById("workflowContent").innerHTML =
            this.render();

        this.bindEvents();

    },

    async addPart() {

        try {

            const inspection = this.getCurrentInspection();

            const description =
                document.getElementById(
                    "partDescription"
                ).value.trim();

            const quantity =
                Number(
                    document.getElementById(
                        "partQty"
                    ).value
                );

            const unitPrice =
                Number(
                    document.getElementById(
                        "partPrice"
                    ).value
                );

            if (!description) {

                alert("Please enter part description.");

                return;

            }

            const request = {

                complaintId:
                    inspection.complaintId,

                description,

                quantity,

                unitPrice,

                itemType: "PART"

            };

            const item =
                await WorkflowService.addEstimateItem(
                    request
                );

            console.log("Part Added", item);

            await WorkflowService.loadEstimateItems();

            this.clearPartForm();

            this.refresh();

        } catch (e) {

            console.error(e);

            alert("Unable to add part.");

        }

    },

    clearPartForm() {

        document.getElementById(
            "partDescription"
        ).value = "";

        document.getElementById(
            "partQty"
        ).value = 1;

        document.getElementById(
            "partPrice"
        ).value = "";

    },

    async addLabour() {

        try {

            const inspection = this.getCurrentInspection();

            const description =
                document.getElementById(
                    "labourDescription"
                ).value.trim();

            const unitPrice =
                Number(
                    document.getElementById(
                        "labourPrice"
                    ).value
                );

            if (!description) {

                alert("Please enter labour description.");

                return;

            }

            const request = {

                complaintId:
                    inspection.complaintId,

                description,

                quantity: 1,

                unitPrice,

                itemType: "LABOUR"

            };

            const item =
                await WorkflowService.addEstimateItem(
                    request
                );

            console.log("Labour Added", item);

            this.clearLabourForm();

            await WorkflowService.loadEstimateItems();

            this.refresh();

        } catch (e) {

            console.error(e);

            alert("Unable to add labour.");

        }

    },

    clearLabourForm() {

        document.getElementById(
            "labourDescription"
        ).value = "";

        document.getElementById(
            "labourPrice"
        ).value = "";

    },

    async removeEstimateItem(id) {

        if (!confirm("Delete this estimate item?")) {

            return;

        }

        try {

            await WorkflowService.deleteEstimateItem(id);

            this.refresh();

        } catch (e) {

            console.error(e);

            alert("Unable to delete estimate item.");

        }

    },

    calculateComplaintTotals() {

        const inspection = this.getCurrentInspection();

        const complaintId =
            inspection.complaintId;

        const items =
            WorkflowHelper.state.estimateItems.filter(

                x => Number(x.complaintId) === Number(complaintId)

            );

        let parts = 0;

        let labour = 0;

        items.forEach(item => {

            if (item.itemType === "PART") {

                parts += Number(item.totalPrice);

            }

            if (item.itemType === "LABOUR") {

                labour += Number(item.totalPrice);

            }

        });

        return {

            parts,

            labour,

            total: parts + labour

        };

    },

    renderTotals() {

        const totals =
            this.calculateComplaintTotals();

        return `

    <div class="card mt-4 border-success">

    <div class="card-header bg-success text-white">

    Complaint Summary

    </div>

    <div class="card-body">

    <div class="row">

    <div class="col-md-4">

    <h6>

    Parts Total

    </h6>

    <h4>

    ₹ ${totals.parts}

    </h4>

    </div>

    <div class="col-md-4">

    <h6>

    Labour Total

    </h6>

    <h4>

    ₹ ${totals.labour}

    </h4>

    </div>

    <div class="col-md-4">

    <h6>

    Complaint Total

    </h6>

    <h3 class="text-success">

    ₹ ${totals.total}

    </h3>

    </div>

    </div>

    </div>

    </div>

    `;

    },

    nextButtonText() {

        const total =
            WorkflowHelper.state.inspections.length;

        if (

            this.currentComplaintIndex === total - 1

        ) {

            return "Finish Estimate →";

        }

        return "Next Complaint →";

    },



};