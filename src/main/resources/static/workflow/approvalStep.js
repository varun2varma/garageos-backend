window.ApprovalStep = {

    render() {

        const estimate = WorkflowHelper.state.estimate || {};
        const items = WorkflowHelper.state.estimateItems || [];

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">
            Customer Approval
        </h4>

        <small class="text-muted">
            Review the estimate before customer approval.
        </small>

    </div>

    <div class="card-body">

        <div class="alert alert-warning">

            Waiting for customer approval.

        </div>

        <table class="table table-bordered">

            <thead>

                <tr>

                    <th>Description</th>

                    <th class="text-end">Qty</th>

                    <th class="text-end">Price</th>

                    <th class="text-end">Total</th>

                </tr>

            </thead>

            <tbody>

                ${this.renderItems(items)}

            </tbody>

        </table>

        <div class="text-end">

            <h5>

                Subtotal :
                ₹ ${estimate.subtotal ?? 0}

            </h5>

            <h6>

                GST :
                ₹ ${estimate.gst ?? 0}

            </h6>

            <h6>

                Discount :
                ₹ ${estimate.discount ?? 0}

            </h6>

            <h4 class="text-success">

                Grand Total :
                ₹ ${estimate.grandTotal ?? 0}

            </h4>

        </div>

        <hr>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                Previous

            </button>

            <button
                id="approveEstimateBtn"
                class="btn btn-success">

                Approve Estimate

            </button>

        </div>

    </div>

</div>

`;

    },

    renderItems(items){

        if(items.length===0){

            return `

<tr>

<td colspan="4" class="text-center">

No Estimate Items

</td>

</tr>

`;

        }

        return items.map(item=>`

<tr>

<td>${item.description}</td>

<td class="text-end">${item.quantity}</td>

<td class="text-end">₹ ${item.unitPrice}</td>

<td class="text-end">₹ ${item.totalPrice}</td>

</tr>

`).join("");

    },

    bindEvents(){

        document
            .getElementById("approveEstimateBtn")
            ?.addEventListener(
                "click",
                async ()=>{

                    await WorkflowService.approveEstimate(

                        WorkflowHelper.state.jobCardNumber

                    );

                    alert(
                        "Estimate Approved Successfully."
                    );

                    Workflow.nextStep();

                });

    }

};