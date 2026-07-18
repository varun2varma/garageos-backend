window.EstimateSummaryStep = {

    render() {

        const customer = WorkflowHelper.state.customer;

        const vehicle = WorkflowHelper.state.vehicle;

        const job = WorkflowHelper.state.job;

        const estimate = WorkflowHelper.state.estimate;

        const inspections =
            WorkflowHelper.state.inspections || [];

        const items =
            WorkflowHelper.state.estimateItems || [];

        const totals =
            this.calculateTotals();

        let complaintCards = "";

        inspections.forEach((inspection, index) => {

            complaintCards +=
                this.renderComplaintCard(
                    inspection,
                    index + 1,
                    items
                );

        });

        return `

<div class="container-fluid">

<div class="card shadow">

<div class="card-header bg-success text-white">

<h3 class="mb-0">

Estimate Summary

</h3>

</div>

<div class="card-body">

<div class="row">

<div class="col-md-3">

<label class="fw-bold">

Customer

</label>

<p>

${customer.name}

</p>

</div>

<div class="col-md-3">

<label class="fw-bold">

Vehicle

</label>

<p>

${vehicle.registrationNumber}

</p>

</div>

<div class="col-md-3">

<label class="fw-bold">

Job Card

</label>

<p>

${job.jobCardNumber}

</p>

</div>

<div class="col-md-3">

<label class="fw-bold">

Estimate

</label>

<p>

${estimate.estimateNumber}

</p>

</div>

</div>

<hr>

${complaintCards}

<hr>

<div class="card border-success">

<div class="card-header bg-success text-white">

Estimate Totals

</div>

<div class="card-body">

<div class="row text-center">

<div class="col-md-3">

<h6>

Parts

</h6>

<h4>

₹ ${totals.parts.toFixed(2)}

</h4>

</div>

<div class="col-md-3">

<h6>

Labour

</h6>

<h4>

₹ ${totals.labour.toFixed(2)}

</h4>

</div>

<div class="col-md-3">

<h6>

Subtotal

</h6>

<h4>

₹ ${totals.subTotal.toFixed(2)}

</h4>

</div>

<div class="col-md-3">

<h6 class="text-success">

Grand Total

</h6>

<h3 class="text-success">

₹ ${totals.grandTotal.toFixed(2)}

</h3>

</div>

</div>

</div>

</div>

<hr>

<div class="d-flex justify-content-between">

<button

id="summaryBackBtn"

class="btn btn-secondary"

>

← Back

</button>

<button

id="generateInvoiceBtn"

class="btn btn-success"

>

Generate Invoice →

</button>

</div>

</div>

</div>

</div>

`;

    },

    renderComplaintCard(
        inspection,
        number,
        items
    ) {

        const complaintItems =
            items.filter(

                item =>
                    Number(item.complaintId) ===
                    Number(inspection.complaintId)

            );

        let rows = "";

        let total = 0;

        complaintItems.forEach(item => {

            total += Number(item.totalPrice);

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

</tr>

`;

        });

        return `

<div class="card mb-4">

<div class="card-header">

Complaint ${number}

</div>

<div class="card-body">

<p>

<strong>Complaint:</strong>

${inspection.complaint}

</p>

<p>

<strong>Inspection:</strong>

${inspection.inspectionNotes}

</p>

<p>

<strong>Recommendation:</strong>

<span class="text-success">

${inspection.recommendedWork}

</span>

</p>

<table class="table table-bordered">

<thead>

<tr>

<th>Type</th>

<th>Description</th>

<th>Qty</th>

<th>Price</th>

<th>Total</th>

</tr>

</thead>

<tbody>

${rows}

</tbody>

</table>

<div class="text-end">

<h5>

Complaint Total :

<span class="text-success">

₹ ${total.toFixed(2)}

</span>

</h5>

</div>

</div>

</div>

`;

    },

    calculateTotals() {

        const items =
            WorkflowHelper.state.estimateItems || [];

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

        const subTotal =
            parts + labour;

        const gst = 0;

        const discount = 0;

        const grandTotal =
            subTotal + gst - discount;

        return {

            parts,

            labour,

            subTotal,

            gst,

            discount,

            grandTotal

        };

    },

    bindEvents() {

        document
            .getElementById("summaryBackBtn")
            ?.addEventListener(

                "click",

                () => Workflow.previousStep()

            );

        document
            .getElementById("generateInvoiceBtn")
            ?.addEventListener(

                "click",

                () => this.generateInvoice()

            );

    },

    async generateInvoice() {

        try {

            const response =
                await InvoiceService.createInvoice({

                    estimateId:
                        WorkflowHelper.state.estimateId

                });

            WorkflowHelper.state.invoice =
                response.data;

            WorkflowHelper.state.invoiceId =
                response.data.id;

            console.log("Invoice", response.data);

            Workflow.nextStep();

        }

        catch (e) {

            console.error(e);

            alert("Unable to generate invoice.");

        }

    }

};