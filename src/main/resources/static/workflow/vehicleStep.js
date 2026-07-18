window.VehicleStep = {

    render() {

        const vehicle = WorkflowHelper.state.vehicle || {};

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Vehicle Information

        </h4>

    </div>

    <div class="card-body">

        <div class="row">

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Registration Number *

                </label>

                <input
                    id="registrationNumber"
                    class="form-control"
                    value="${vehicle.registrationNumber || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Brand *

                </label>

                <input
                    id="brand"
                    class="form-control"
                    value="${vehicle.brand || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Model *

                </label>

                <input
                    id="model"
                    class="form-control"
                    value="${vehicle.model || ''}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Fuel Type

                </label>

                <select
                    id="fuelType"
                    class="form-select">

                    <option value="">Select Fuel Type</option>

                    <option value="PETROL"
                        ${vehicle.fuelType === "PETROL" ? "selected" : ""}>
                        PETROL
                    </option>

                    <option value="DIESEL"
                        ${vehicle.fuelType === "DIESEL" ? "selected" : ""}>
                        DIESEL
                    </option>

                    <option value="CNG"
                        ${vehicle.fuelType === "CNG" ? "selected" : ""}>
                        CNG
                    </option>

                    <option value="EV"
                        ${vehicle.fuelType === "EV" ? "selected" : ""}>
                        EV
                    </option>

                </select>

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Odometer

                </label>

                <input
                    id="odometer"
                    type="number"
                    class="form-control"
                    value="${vehicle.odometer || ''}">

            </div>

        </div>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                ← Previous

            </button>

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

        return {

            customerId:
                WorkflowHelper.state.customerId,

            registrationNumber:
                document.getElementById("registrationNumber").value.trim(),

            brand:
                document.getElementById("brand").value.trim(),

            model:
                document.getElementById("model").value.trim(),

            fuelType:
                document.getElementById("fuelType").value,

            odometer:
                Number(document.getElementById("odometer").value)

        };

    },

    validate(request) {

        if (!request.registrationNumber) {

            alert("Registration Number is mandatory.");

            return false;

        }

        if (!request.brand) {

            alert("Brand is mandatory.");

            return false;

        }

        if (!request.model) {

            alert("Model is mandatory.");

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

            let vehicle;

            if (WorkflowHelper.state.vehicleId) {

                const response =
                    await VehicleService.updateVehicle(

                        WorkflowHelper.state.vehicleId,

                        request

                    );

                vehicle = response.data;

            }

            else {

                try {

                    const response =
                        await VehicleService.searchByRegistrationNumber(

                            request.registrationNumber

                        );

                    vehicle = response.data;

                    console.log("Existing vehicle found.");

                }

                catch (e) {

                    if (e.status === 404 || e.response?.status === 404) {

                        console.log("Vehicle not found. Creating new vehicle.");

                        const response =
                            await VehicleService.createVehicle(request);

                        vehicle = response.data;

                    } else {

                        throw e;

                    }

                }

            }

            WorkflowHelper.state.vehicle = vehicle;
            WorkflowHelper.state.vehicleId = vehicle.id;

            console.log("Vehicle Saved", vehicle);

            return true;

        }

        catch (e) {

            console.error(e);

            alert(e.message || "Unable to save vehicle.");

            return false;

        }

    }

};