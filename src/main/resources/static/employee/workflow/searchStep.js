window.SearchStep = {

    render() {

        return `

<div class="card shadow-sm">

    <div class="card-header bg-white">

        <h4>

            Start New Service

        </h4>

        <small class="text-muted">

            Search using Customer Mobile Number or Vehicle Registration Number.

        </small>

    </div>

    <div class="card-body">

        <div class="row">

            <div class="col-md-6">

                <label class="form-label">

                    Mobile Number

                </label>

                <input

                    id="mobileNumber"

                    class="form-control"

                    placeholder="9876543210">

            </div>

            <div class="col-md-6">

                <label class="form-label">

                    Registration Number

                </label>

                <input

                    id="registrationNumber"

                    class="form-control"

                    placeholder="AP09AB1234">

            </div>

        </div>

        <div class="mt-4">

            <button

                id="searchBtn"

                class="btn btn-primary">

                <i class="bi bi-search"></i>

                Search

            </button>

        </div>

        <hr>

        <div id="searchResult">

            <div class="text-center text-muted py-5">

                Search a customer to continue.

            </div>

        </div>

    </div>

</div>

`;

    },

    renderResult(customer, vehicle) {

        document.getElementById("searchResult").innerHTML = `

    <div class="card border-success shadow-sm">

        <div class="card-body">

            <div class="row">

                <div class="col-md-6">

                    <h5>

                        Customer

                    </h5>

                    <table class="table table-borderless mb-0">

                        <tr>

                            <td>Name</td>

                            <td>

                                ${customer?.firstName ?? "-"}
                                ${customer?.lastName ?? ""}

                            </td>

                        </tr>

                        <tr>

                            <td>Mobile</td>

                            <td>

                                ${customer?.mobileNumber ?? customer?.mobile ?? "-"}

                            </td>

                        </tr>

                    </table>

                </div>

                <div class="col-md-6">

                    <h5>

                        Vehicle

                    </h5>

                    <table class="table table-borderless mb-0">

                        <tr>

                            <td>Registration</td>

                            <td>

                                ${vehicle?.registrationNumber ?? "-"}

                            </td>

                        </tr>

                        <tr>

                            <td>Vehicle</td>

                            <td>

                                ${vehicle?.brand ?? ""}
                                ${vehicle?.model ?? ""}

                            </td>

                        </tr>

                    </table>

                </div>

            </div>

            <div class="text-end mt-3">

                <button

                    id="continueBtn"

                    class="btn btn-success">

                    Continue →

                </button>

            </div>

        </div>

    </div>

    `;

        document
            .getElementById("continueBtn")
            .addEventListener("click", () => {

                WorkflowHelper.state.customer = customer;

                WorkflowHelper.state.vehicle = vehicle;

                Workflow.nextStep();

            });

    },

    bindEvents() {

        document

            .getElementById("searchBtn")

            .addEventListener(

                "click",

                () => this.search()

            );

    },

    async search() {

        try {

            const mobile =
                document
                    .getElementById("mobileNumber")
                    .value
                    .trim();

            const registration =
                document
                    .getElementById("registrationNumber")
                    .value
                    .trim();

            if (!mobile && !registration) {

                alert(
                    "Enter Mobile Number or Registration Number."
                );

                return;

            }

            document.getElementById("searchResult").innerHTML = `

                <div class="text-center py-5">

                    <div class="spinner-border text-primary"></div>

                    <p class="mt-3">

                        Searching...

                    </p>

                </div>

            `;

            let customer = null;

            let vehicle = null;

            if (mobile) {

                customer =
                    await CustomerService.findByMobile(mobile);

            }

            if (registration) {

                vehicle =
                    await VehicleService.searchByRegistrationNumber(
                        registration
                    );

            }

            this.renderResult(customer, vehicle);

        }

        catch (error) {

            console.error(error);

            document.getElementById("searchResult").innerHTML = `

                <div class="alert alert-danger">

                    Customer / Vehicle not found.

                </div>

            `;

        }

    }

};