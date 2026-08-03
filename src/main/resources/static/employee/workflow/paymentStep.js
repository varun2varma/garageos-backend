window.PaymentStep = {

    render() {

        const invoice = WorkflowHelper.state.invoice;

        if (invoice?.paymentStatus === "PENDING") {

            return `

    <div class="card shadow-sm">

        <div class="card-header">

            <h4>

                Waiting For Customer Payment

            </h4>

        </div>

        <div class="card-body text-center">

            <div class="mb-4">

                <i class="bi bi-hourglass-split text-warning"
                   style="font-size:60px;"></i>

            </div>

            <h5 class="text-warning">

                Payment Pending

            </h5>

            <p class="text-muted">

                Invoice has been shared with the customer.

                <br>

                Waiting for the customer to complete payment.

            </p>

            <hr>

            <div class="d-flex justify-content-between">

                <button
                        id="refreshPaymentBtn"
                        class="btn btn-primary">

                        Refresh Payment Status

                    </button>

            </div>

        </div>

    </div>

    `;

        }

        return this.renderPaymentReceived();

    },

    renderPaymentReceived() {

        const invoice = WorkflowHelper.state.invoice;

        return `

    <div class="card shadow-sm">

        <div class="card-header">

            <h4>

                Payment Received

            </h4>

        </div>

        <div class="card-body">

            <div class="alert alert-success">

                Customer payment received successfully.

            </div>

            <div class="mb-3">

                <strong>Payment Mode :</strong>

                ${invoice.paymentMode}

            </div>

            <div class="mb-3">

                <strong>Amount :</strong>

                ₹ ${invoice.grandTotal}

            </div>

            <div class="text-end">

                <button
                    id="deliveryBtn"
                    class="btn btn-success">

                    Proceed To Delivery →

                </button>

            </div>

        </div>

    </div>

    `;

    },

    bindEvents() {

        document
            .getElementById("paymentBtn")
            ?.addEventListener(
                "click",
                async () => {

                    try {

                        await WorkflowService.receivePayment();

                        alert("Payment Received.");

                        Workflow.nextStep();

                    } catch (e) {

                        alert(e.message);

                    }

                });

        document
            .getElementById("refreshPaymentBtn")
            ?.addEventListener(
                "click",
                async () => {

                    try {

                        const invoice =
                            await WorkflowService.refreshInvoice();

                        if (
                            invoice.paymentStatus === "PAID"
                        ) {

                            await WorkflowService.refreshWorkflowStatus();

                            Workflow.nextStep();

                        }
                        else {

                            alert(
                                "Customer has not completed payment yet."
                            );

                        }

                    } catch (e) {

                        console.error(e);

                    }

                });

    }

};