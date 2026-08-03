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

        const customerFound = !!customer?.id;
        const vehicleFound = !!vehicle?.id;

        document.getElementById("searchResult").innerHTML = `

    <div class="card shadow-sm border-success">

        <div class="card-body">

            <div class="row">

                <div class="col-md-6">

                    <h5>

                        Customer

                    </h5>

                    ${
                        customerFound
                        ?

                        `
                        <table class="table table-borderless">

                            <tr>
                                <td>Name</td>
                                <td>${customer.firstName} ${customer.lastName ?? ""}</td>
                            </tr>

                            <tr>
                                <td>Mobile</td>
                                <td>${customer.mobileNumber}</td>
                            </tr>

                        </table>
                        `

                        :

                        `
                        <div class="alert alert-warning">

                            Customer not found.

                            <div class="mt-3">

                                <button
                                    id="registerCustomerBtn"
                                    class="btn btn-outline-primary btn-sm">

                                    + Register Customer

                                </button>

                            </div>

                        </div>
                        `
                    }

                </div>

                <div class="col-md-6">

                    <h5>

                        Vehicle

                    </h5>

                    ${
                        vehicleFound

                        ?

                        `
                        <table class="table table-borderless">

                            <tr>
                                <td>Registration</td>
                                <td>${vehicle.registrationNumber}</td>
                            </tr>

                            <tr>
                                <td>Vehicle</td>
                                <td>${vehicle.brand} ${vehicle.model}</td>
                            </tr>

                        </table>
                        `

                        :

                        `
                        <div class="alert alert-warning">

                            Vehicle not found.

                            <div class="mt-3">

                                <button
                                    id="registerVehicleBtn"
                                    class="btn btn-outline-primary btn-sm">

                                    + Register Vehicle

                                </button>

                            </div>

                        </div>
                        `
                    }

                </div>

            </div>

            <div class="text-end mt-4">

                <button
                    id="continueBtn"
                    class="btn btn-success"
                    ${customerFound && vehicleFound ? "" : "disabled"}>

                    Continue →

                </button>

            </div>

        </div>

    </div>

    `;

        document
            .getElementById("continueBtn")
            .addEventListener("click", () => {

                Workflow.nextStep();

            });


        document
            .getElementById("registerCustomerBtn")
            ?.addEventListener(
                "click",
                () => this.openCustomerDrawer()
            );

        document
            .getElementById("registerVehicleBtn")
            ?.addEventListener(
                "click",
                () => this.openVehicleDrawer()
            );

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

        const mobile =
            document
                .getElementById("mobileNumber")
                .value
                .trim();

        const registration =
            document
                .getElementById("registrationNumber")
                .value
                .trim()
                .toUpperCase();

        if (!mobile && !registration) {

            alert("Enter Mobile Number or Registration Number.");

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

        WorkflowHelper.state.customer = null;
        WorkflowHelper.state.customerId = null;
        WorkflowHelper.state.vehicle = null;
        WorkflowHelper.state.vehicleId = null;

        if (mobile) {

            try {

                customer =
                    await CustomerService.findByMobile(mobile);

                WorkflowHelper.state.customer = customer;
                WorkflowHelper.state.customerId = customer.id;

            }

            catch (e) {

                console.log("Customer not found.");

                WorkflowHelper.state.customer = {

                    firstName: "",
                    lastName: "",
                    mobileNumber: mobile,
                    email: "",
                    address: ""

                };

            }

        }

        if (registration) {

            try {

                vehicle =
                    await VehicleService.searchByRegistrationNumber(
                        registration
                    );

                WorkflowHelper.state.vehicle = vehicle;
                WorkflowHelper.state.vehicleId = vehicle.id;

            }

            catch (e) {

                console.log("Vehicle not found.");

                WorkflowHelper.state.vehicle = {

                    registrationNumber: registration

                };

            }

        }

        this.renderResult(
            WorkflowHelper.state.customer,
            WorkflowHelper.state.vehicle
        );

    },


    openCustomerDrawer() {

        SidePanel.open(

            "Register Customer",

            CustomerStep.render()

        );

        CustomerStep.bindEvents();

    },

    openVehicleDrawer() {

        SidePanel.open(

            "Register Vehicle",

            VehicleStep.render()

        );

        VehicleStep.bindEvents();

    }

};