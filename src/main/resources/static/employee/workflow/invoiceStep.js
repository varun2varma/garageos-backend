window.InvoiceStep = {

    render() {

        if (!WorkflowHelper.state.invoice) {
            return this.renderGenerateInvoice();
        }

        return this.renderInvoice();

    },

    renderGenerateInvoice() {

        const estimate = WorkflowHelper.state.estimate;
        const customer = WorkflowHelper.state.customer;
        const vehicle = WorkflowHelper.state.vehicle;
        const items = WorkflowHelper.state.estimateItems || [];

        let rows = "";

        items.forEach(item => {

            rows += `

    <tr>

        <td>${item.itemType}</td>

        <td>${item.description}</td>

        <td>${item.quantity}</td>

        <td>₹ ${item.unitPrice}</td>

        <td>₹ ${item.totalPrice}</td>

    </tr>

    `;

        });

        return `

    <div class="card shadow">

        <div class="card-header bg-primary text-white">

            <h3 class="mb-0">

                Review Estimate Before Invoice

            </h3>

        </div>

        <div class="card-body">

            <div class="row mb-4">

                <div class="col-md-3">

                    <label class="fw-bold">

                        Customer

                    </label>

                    <p>${customer.firstName} ${customer.lastName}</p>

                </div>

                <div class="col-md-3">

                    <label class="fw-bold">

                        Vehicle

                    </label>

                    <p>${vehicle.registrationNumber}</p>

                </div>

                <div class="col-md-3">

                    <label class="fw-bold">

                        Estimate

                    </label>

                    <p>${estimate.estimateNumber}</p>

                </div>

                <div class="col-md-3">

                    <label class="fw-bold">

                        Status

                    </label>

                    <p>

                        <span class="badge bg-success">

                            ${estimate.status}

                        </span>

                    </p>

                </div>

            </div>

            <table class="table table-bordered">

                <thead>

                    <tr>

                        <th>Type</th>

                        <th>Description</th>

                        <th>Qty</th>

                        <th>Unit Price</th>

                        <th>Total</th>

                    </tr>

                </thead>

                <tbody>

                    ${rows}

                </tbody>

            </table>

            <div class="row mt-4">

                <div class="col-md-3">

                    <h6>Subtotal</h6>

                    <h4>₹ ${estimate.subtotal}</h4>

                </div>

                <div class="col-md-3">

                    <h6>GST</h6>

                    <h4>₹ ${estimate.gst}</h4>

                </div>

                <div class="col-md-3">

                    <h6>Discount</h6>

                    <h4>₹ ${estimate.discount}</h4>

                </div>

                <div class="col-md-3">

                    <h6 class="text-success">

                        Grand Total

                    </h6>

                    <h3 class="text-success">

                        ₹ ${estimate.grandTotal}

                    </h3>

                </div>

            </div>

            <hr>

            <div class="d-flex justify-content-between align-items-center">

                <button
                    id="printInvoiceBtn"
                    class="btn btn-outline-primary">

                    🖨 Print Invoice

                </button>

                <div class="text-end">

                    <span class="badge bg-warning fs-6 mb-2">
                        Waiting For Customer Payment
                    </span>

                    <br>

                    <small class="text-muted">
                        Invoice has been generated successfully.
                        Customer will complete the payment from the Customer Portal.
                    </small>

                </div>

            </div>

        </div>

    </div>

    `;

    },

    renderInvoice() {

        const invoice = WorkflowHelper.state.invoice;
        const customer = WorkflowHelper.state.customer;
        const vehicle = WorkflowHelper.state.vehicle;
        const items = WorkflowHelper.state.estimateItems || [];

        let rows = "";

        items.forEach(item => {

            rows += `

<tr>

<td>${item.itemType}</td>

<td>${item.description}</td>

<td>${item.quantity}</td>

<td>₹ ${item.unitPrice}</td>

<td>₹ ${item.totalPrice}</td>

</tr>

`;

        });

        return `

<div class="card shadow">

<div class="card-header bg-success text-white">

<h3>

Tax Invoice

</h3>

</div>

<div class="card-body">

<div class="row">

<div class="col-md-3">

<label class="fw-bold">

Invoice No

</label>

<p>

${invoice.invoiceNumber}

</p>

</div>

<div class="col-md-3">

<label class="fw-bold">

Customer

</label>

<p>
${customer.firstName} ${customer.lastName}
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

Payment Status

</label>

<p>

<span class="badge bg-warning">

${invoice.paymentStatus}

</span>

</p>

</div>

</div>

<hr>

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

<hr>

<div class="row">

<div class="col-md-3">

<h6>Subtotal</h6>

<h4>

₹ ${invoice.subtotal}

</h4>

</div>

<div class="col-md-3">

<h6>GST</h6>

<h4>

₹ ${invoice.gst}

</h4>

</div>

<div class="col-md-3">

<h6>Discount</h6>

<h4>

₹ ${invoice.discount}

</h4>

</div>

<div class="col-md-3">

<h6 class="text-success">

Grand Total

</h6>

<h3 class="text-success">

₹ ${invoice.grandTotal}

</h3>

</div>

</div>

<hr>

<div class="d-flex justify-content-between">

<button
id="printInvoiceBtn"
class="btn btn-outline-primary">

🖨 Print Invoice

</button>

<button
id="paymentBtn"
class="btn btn-success">

Proceed To Payment →

</button>

</div>

</div>

</div>

`;

    },

    bindEvents() {

        if (!WorkflowHelper.state.invoice) {

            document
                .getElementById("editEstimateBtn")
                ?.addEventListener(
                    "click",
                    () => Workflow.goToStep(5)
                );

            document
                .getElementById("generateInvoiceBtn")
                ?.addEventListener(
                    "click",
                    () => this.generateInvoice()
                );

            return;

        }

        document
            .getElementById("printInvoiceBtn")
            ?.addEventListener(
                "click",
                () => window.print()
            );

        document
            .getElementById("printInvoiceBtn")
            ?.addEventListener(
                "click",
                () => window.print()
            );

    },

    async generateInvoice() {

        try {

            const response =
                await WorkflowService.generateWorkflowInvoice();

            WorkflowHelper.state.invoice =
                response;

            WorkflowHelper.state.invoiceId =
                response.id;

            await Workflow.renderStep();

        } catch (e) {

            console.error(e);

            alert(e.message);

        }

    }

};