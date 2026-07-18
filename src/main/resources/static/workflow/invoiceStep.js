window.InvoiceStep = {

    render() {

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Generate Invoice

        </h4>

    </div>

    <div class="card-body">

        <div class="alert alert-info">

            <strong>Estimate ID :</strong>

            <span id="estimateIdLabel">

                ${AppState.workflow.estimateId ?? "-"}

            </span>

        </div>

        <div class="mb-4">

            <label class="form-label">

                Remarks

            </label>

            <textarea
                id="invoiceRemarks"
                rows="4"
                class="form-control"
                placeholder="Optional remarks"></textarea>

        </div>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                ← Previous

            </button>

            <button
                id="generateInvoiceBtn"
                class="btn btn-success">

                Generate Invoice

            </button>

        </div>

    </div>

</div>

`;

    },

    bindEvents() {

        // Reserved for future use

    },

    validate() {

        if (!AppState.workflow.estimateId) {

            alert("Estimate is not available.");

            return false;

        }

        return true;

    },

    collectData() {

        return {

            estimateId:
                AppState.workflow.estimateId,

            remarks:
                document
                    .getElementById("invoiceRemarks")
                    .value
                    .trim()

        };

    },

    async save() {

        if (!this.validate()) {

            return false;

        }

        try {

            const request =
                this.collectData();

            const response =
                await InvoiceService.createInvoice(request);

            AppState.workflow.invoice =
                response;

            AppState.workflow.invoiceId =
                response.id;

            console.log("Invoice Generated", response);

            return true;

        }

        catch (e) {

            alert(e.message);

            return false;

        }

    }

};