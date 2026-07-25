window.CustomerStep = {

    render() {

        const customer = WorkflowHelper.state.customer || {};

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Customer Information

        </h4>

    </div>

    <div class="card-body">

        <div class="row">

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    First Name *

                </label>

                <input
                    id="customerName"
                    class="form-control"
                    value="${customer.firstName || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Mobile Number *

                </label>

                <input
                    id="customerMobile"
                    class="form-control"
                    maxlength="10"
                    inputmode="numeric"
                    pattern="[0-9]*"
                    value="${customer.mobileNumber || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Email

                </label>

                <input
                    id="customerEmail"
                    class="form-control"
                    value="${customer.email || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Address

                </label>

                <input
                    id="customerAddress"
                    class="form-control"
                    value="${customer.address || ''}">

            </div>

        </div>

        <div class="text-end">

            <button
                id="nextBtn"
                class="btn btn-primary">

                Next →

            </button>

        </div>

    </div>

</div>

`;

    },

    bindEvents() {

        document
            .getElementById("nextBtn")
            ?.addEventListener("click", async () => {

                const success = await this.save();

                if (success) {

                    Workflow.nextStep();

                }

            });

    },

    collectData() {

        const request = {

            firstName: document.getElementById("customerName").value.trim(),
            lastName: WorkflowHelper.state.customer?.lastName || "",
            mobileNumber: document.getElementById("customerMobile").value.trim(),
            email: document.getElementById("customerEmail").value.trim(),
            address: document.getElementById("customerAddress").value.trim(),
            city: WorkflowHelper.state.customer?.city || "",
            state: WorkflowHelper.state.customer?.state || "",
            pincode: WorkflowHelper.state.customer?.pincode || "",
            remarks: WorkflowHelper.state.customer?.remarks || ""

        };

        console.log("Customer Request:", request);

        return request;
    },

    validate(request) {

        if (!request.firstName) {

            alert("First Name is mandatory.");

            return false;

        }

        if (!request.mobileNumber) {

            alert("Mobile Number is mandatory.");

            return false;

        }

        if (!/^[6-9]\d{9}$/.test(request.mobileNumber)) {
            alert("Please enter a valid 10-digit mobile number.");
            return false;
        }

        return true;

    },

    async save() {

        const request = this.collectData();

        if (!this.validate(request)) {

            return false;

        }

        try {

            let response;

            // Existing customer already loaded in workflow
            if (WorkflowHelper.state.customerId) {

                response = await CustomerService.updateCustomer(
                    WorkflowHelper.state.customerId,
                    request
                );

            } else {

                try {

                    response = await CustomerService.findByMobile(request.mobileNumber);
                    console.log("Existing Customer:", response);
                    console.log("Customer API Response:", response);
                    console.log("Customer ID:", response?.id);
                    WorkflowHelper.state.customer = response;
                    WorkflowHelper.state.customerId = response.id;

                } catch (e) {

                    const error = JSON.parse(e.message);

                    if (error.message?.includes("Customer not found")) {

                        response = await CustomerService.createCustomer(request);
                        console.log("Customer Response:", response);
                        console.log("Customer API Response:", response);
                        console.log("Customer ID:", response?.id);
                        WorkflowHelper.state.customer = response;
                        WorkflowHelper.state.customerId = response.id;

                    } else {

                        throw e;

                    }

                }

            }


            return true;

        } catch (e) {

            console.error(e);
            alert(e.message || "Failed to save customer.");
            return false;

        }

    }

};