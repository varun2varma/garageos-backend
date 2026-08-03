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

                <select
                    id="brand"
                    class="form-select">

                    <option value="">Select Brand</option>

                </select>

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">
                    Model *
                </label>

                <select
                    id="model"
                    class="form-select">

                    <option value="">Select Model</option>

                </select>

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

            <div class="col-md-6 mb-3">

                <label class="form-label">
                    Transmission
                </label>

                <select
                    id="transmission"
                    class="form-select">

                    <option value="">Select Transmission</option>

                    <option value="MANUAL"
                        ${vehicle.transmission === "MANUAL" ? "selected" : ""}>
                        MANUAL
                    </option>

                    <option value="AUTOMATIC"
                        ${vehicle.transmission === "AUTOMATIC" ? "selected" : ""}>
                        AUTOMATIC
                    </option>

                </select>

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">
                    Variant
                </label>

                <select
                    id="variant"
                    class="form-select">

                    <option value=""> Select Variant</option>

                </select>

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">
                    Manufacturing Year
                </label>

                <input
                    id="manufacturingYear"
                    type="number"
                    min="1990"
                    max="9999"
                    class="form-control"
                    value="${vehicle.manufacturingYear || ''}">

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

        this.initializeDropdowns();

        document
            .getElementById("brand")
            ?.addEventListener("change", async (e) => {

                await this.loadModels(e.target.value);

                document.getElementById("variant").innerHTML =
                    '<option value="">Select Variant</option>';

            });

        document
            .getElementById("model")
            ?.addEventListener("change", async (e) => {

                await this.loadVariants(e.target.value);

            });

        document
            .getElementById("nextBtn")
            ?.addEventListener("click", async () => {

                const vehicle = await this.save();

                if (!vehicle) {
                    return;
                }

                SidePanel.close();

                SearchStep.renderResult(
                    WorkflowHelper.state.customer,
                    WorkflowHelper.state.vehicle
                );

            });

    },

    async initializeDropdowns() {

//        await this.loadBrands();

        await Promise.all([
            this.loadBrands(),
            this.loadMetadata()
        ]);

        const vehicle = WorkflowHelper.state.vehicle;

        if (!vehicle) {

            return;

        }

        const brandSelect =
            document.getElementById("brand");

        const brandOption =
            [...brandSelect.options]
                .find(option => option.text === vehicle.brand);

        if (!brandOption) {

            return;

        }

        brandSelect.value = brandOption.value;

        await this.loadModels(brandOption.value);

        const modelSelect =
            document.getElementById("model");

        const modelOption =
            [...modelSelect.options]
                .find(option => option.text === vehicle.model);

        if (!modelOption) {

            return;

        }

        modelSelect.value = modelOption.value;

        await this.loadVariants(modelOption.value);

        const variantSelect =
            document.getElementById("variant");

        const variantOption =
            [...variantSelect.options]
                .find(option => option.text === vehicle.variant);

        if (variantOption) {

            variantSelect.value = variantOption.value;

        }

    },

        async loadBrands() {

            try {

                const brands =
                    await VehicleMasterService.getBrands();

                const brandSelect =
                    document.getElementById("brand");

                brandSelect.innerHTML =
                    '<option value="">Select Brand</option>';

                brands.forEach(brand => {

                    brandSelect.innerHTML += `
                        <option value="${brand.id}">
                            ${brand.name}
                        </option>`;

                });

            }

            catch (e) {

                console.error("Unable to load brands.", e);

            }

        },

        async loadModels(brandId) {

            const modelSelect =
                document.getElementById("model");

            modelSelect.innerHTML =
                '<option value="">Select Model</option>';

            if (!brandId) {

                return;

            }

            try {

                const models =
                    await VehicleMasterService.getModels(brandId);

                models.forEach(model => {

                    modelSelect.innerHTML += `
                        <option value="${model.id}">
                            ${model.name}
                        </option>`;

                });

            }

            catch (e) {

                console.error("Unable to load models.", e);

            }

        },

        async loadVariants(modelId) {

            const variantSelect =
                document.getElementById("variant");

            variantSelect.innerHTML =
                '<option value="">Select Variant</option>';

            if (!modelId) {

                return;

            }

            try {

                const variants =
                    await VehicleMasterService.getVariants(modelId);

                variants.forEach(variant => {

                    variantSelect.innerHTML += `
                        <option value="${variant.id}">
                            ${variant.name}
                        </option>`;

                });

            }

            catch (e) {

                console.error("Unable to load variants.", e);

            }

        },

        collectData() {

            const brandSelect =
                document.getElementById("brand");

            const modelSelect =
                document.getElementById("model");

            const variantSelect =
                document.getElementById("variant");

            return {

                customerId: WorkflowHelper.state.customerId,

                registrationNumber:
                    document.getElementById("registrationNumber").value.trim(),

                brand:
                    brandSelect.options[
                        brandSelect.selectedIndex
                    ]?.text || "",

                model:
                    modelSelect.options[
                        modelSelect.selectedIndex
                    ]?.text || "",

                variant:
                    variantSelect.options[
                        variantSelect.selectedIndex
                    ]?.text || "",

                fuelType:
                    document.getElementById("fuelType").value,

                transmission:
                    document.getElementById("transmission").value,

                manufacturingYear:
                    Number(document.getElementById("manufacturingYear").value),

                odometer:
                    Number(document.getElementById("odometer").value)

            };

        },

        validate(request) {

            if (!request.registrationNumber) {

                alert("Registration Number is mandatory.");

                return false;

            }

//            const patterns = [
//                /^[A-Z]{2}(0[1-9]|[1-9][0-9])[A-Z]{1,3}(000[1-9]|00[1-9]\d|0[1-9]\d{2}|[1-9]\d{3})$/,      // State
//                /^\d{2}BH\d{4}[A-Z]{2}$/,                  // Bharat Series
//                /^CD\d{1,3}[A-Z]\d{1,4}$/,                 // Diplomatic
//                /^CC\d{1,3}\d{1,4}$/,                      // Consular
//                /^TR\d{1,6}$/,                            // Temporary
//                /^TEMP\d{1,6}$/,                          // Temporary
//                /^[A-Z]{2}\d{6}[A-Z]?$/                   // Military
//            ];
//
//            const regNo = request.registrationNumber
//                .trim()
//                .replace(/[-\s]/g, "")
//                .toUpperCase();
//
//            if (!statePlateRegex.test(regNo)) {
//                alert("Please enter a valid Indian vehicle registration number.");
//                return false;
//            }

            if (!request.brand) {

                alert("Brand is mandatory.");

                return false;

            }

            if (!request.model) {

                alert("Model is mandatory.");

                return false;

            }

            if (!request.transmission) {

                alert("Transmission is mandatory.");

                return false;

            }

            if (!request.manufacturingYear) {

                alert("Manufacturing Year is mandatory.");

                return false;

            }

            const currentYear = new Date().getFullYear();

            if (
                !/^\d{4}$/.test(request.manufacturingYear) ||
                request.manufacturingYear < 1990 ||
                request.manufacturingYear > currentYear
            ) {
                alert(`Please enter a valid manufacturing year.`);
                return false;
            }

            return true;

        },

        async loadMetadata() {

            const response = await VehicleMasterService.getMetadata();

            if (!response.success) {
                return;
            }

            const metadata = response.data;

            this.populateFuelTypes(metadata.fuelTypes);
            this.populateTransmissionTypes(metadata.transmissionTypes);
        //    this.populateBodyTypes(metadata.bodyTypes);

        },

        populateFuelTypes(fuelTypes) {

            const vehicle = WorkflowHelper.state.vehicle || {};

            const select = document.getElementById("fuelType");

            select.innerHTML =
                '<option value="">Select Fuel Type</option>';

            fuelTypes.forEach(fuel => {

                select.innerHTML += `
                    <option value="${fuel}"
                        ${vehicle.fuelType === fuel ? "selected" : ""}>
                        ${this.formatEnum(fuel)}
                    </option>
                `;

            });

        },

        populateBodyTypes(bodyTypes) {

            const select = document.getElementById("bodyType");

            select.innerHTML = '<option value="">Select Body Type</option>';

            bodyTypes.forEach(type => {

                select.innerHTML += `
                    <option value="${type}">
                        ${formatEnum(type)}
                    </option>
                `;
            });
        },

        populateTransmissionTypes(transmissionTypes) {

            const vehicle = WorkflowHelper.state.vehicle || {};

            const select =
                document.getElementById("transmission");

            select.innerHTML =
                '<option value="">Select Transmission</option>';

            transmissionTypes.forEach(type => {

                select.innerHTML += `
                    <option value="${type}"
                        ${vehicle.transmission === type ? "selected" : ""}>
                        ${this.formatEnum(type)}
                    </option>
                `;

            });

        },

        formatEnum(value) {
            return value
                .toLowerCase()
                .split("_")
                .map(word => word.charAt(0).toUpperCase() + word.slice(1))
                .join(" ");

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

                            vehicle = response;

                        }

                        else {

                            try {

                                const response =
                                    await VehicleService.searchByRegistrationNumber(

                                        request.registrationNumber

                                    );

                                vehicle = response;

                                console.log("Existing vehicle found.");

                            }

                            catch (e) {

                                if (e.status === 404 || e.response?.status === 404) {

                                    console.log("Vehicle not found. Creating new vehicle.");

                                    console.log("Vehicle Request", request);

                                    console.log("Workflow State", WorkflowHelper.state);

                                    const response =
                                        await VehicleService.createVehicle(request);

                                    vehicle = response;

                                }

                                else {

                                    throw e;

                                }

                            }

                        }

                        WorkflowHelper.state.vehicle = vehicle;

                        WorkflowHelper.state.vehicleId = vehicle.id;

                        console.log(WorkflowHelper);

                        const recommendations =
                            await InspectionFindingService.loadRecommendations({

                                brand: WorkflowHelper.state.vehicle.brand,

                                model: WorkflowHelper.state.vehicle.model,

                                variant: WorkflowHelper.state.vehicle.variant,

                                fuelType: WorkflowHelper.state.vehicle.fuelType,

                                transmission: WorkflowHelper.state.vehicle.transmission,

                                manufacturingYear: WorkflowHelper.state.vehicle.manufacturingYear,

                                odometer: request.odometer

                            });

                        WorkflowHelper.state.recommendedInspectionItems =
                            recommendations;

                        console.log("Vehicle Saved", vehicle);

                        return vehicle;

        }

        catch (e) {

                        console.error(e);

                        alert(e.message || "Unable to save vehicle.");

                        return false;

        }

    }

};
