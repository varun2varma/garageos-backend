window.PaymentStep = {

    render() {

        return `

<div class="card shadow-sm">

<div class="card-header">

<h4>

Receive Payment

</h4>

</div>

<div class="card-body">

<div class="mb-3">

<label>

Payment Mode

</label>

<select
id="paymentMode"
class="form-select">

<option value="CASH">Cash</option>

<option value="UPI">UPI</option>

<option value="CARD">Card</option>

</select>

</div>

<div class="d-flex justify-content-between">

<button
id="previousBtn"
class="btn btn-outline-secondary">

Previous

</button>

<button
id="paymentBtn"
class="btn btn-success">

Receive Payment

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

                        const paymentMode =
                            document.getElementById("paymentMode").value;

                        await WorkflowService.receivePayment(paymentMode);

                        alert("Payment Received.");

                        Workflow.nextStep();

                    } catch (e) {

                        alert(e.message);

                    }

                });

    }

};