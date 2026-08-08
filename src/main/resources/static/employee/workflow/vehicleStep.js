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
            
            <!-- VARIANT MOVED HERE -->

    <div class="col-md-6 mb-3">

        <label class="form-label">
            Variant
        </label>

        <select
            id="variant"
            class="form-select">

            <option value="">
                Select Variant
            </option>

        </select>

    </div>
          <!-- FUEL TYPE -->
          
            <div class="col-md-6 mb-3">

        <label class="form-label">
            Fuel Type
        </label>

        <select
            id="fuelType"
            class="form-select">

            <option value="">
                Select Fuel Type
            </option>

        </select>

    </div>
    
    
    <!-- TRANSMISSION -->

    <div class="col-md-6 mb-3">

        <label class="form-label">
            Transmission
        </label>

        <select
            id="transmission"
            class="form-select">

            <option value="">
                Select Transmission
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

        /*
        -----------------------------------------
        Brand → Model
        -----------------------------------------
        */

        document
            .getElementById("brand")
            ?.addEventListener("change", async (e) => {

                const brandId = e.target.value;

                this.resetVariants();
                this.resetFuelTypes();
                this.resetTransmissions();

                await this.loadModels(brandId);

            });


        /*
        -----------------------------------------
        Model → Variant
        -----------------------------------------
        */

        document
            .getElementById("model")
            ?.addEventListener("change", async (e) => {

                const modelId = e.target.value;

                this.resetFuelTypes();
                this.resetTransmissions();

                await this.loadVariants(modelId);

            });


        /*
        -----------------------------------------
        Variant → Fuel Type
        -----------------------------------------
        */

        document
            .getElementById("variant")
            ?.addEventListener("change", async (e) => {

                const modelId = document.getElementById("model")?.value;

                const variantId = e.target.value;

                this.resetFuelTypes();
                this.resetTransmissions();

                if (!variantId ||!modelId) {
                    return;
                }

                await this.loadFuelTypes(variantId,modelId);

            });


        /*
        -----------------------------------------
        Fuel Type → Transmission
        -----------------------------------------
        */

        document
            .getElementById("fuelType")
            ?.addEventListener("change", async (e) => {

                const fuelType = e.target.value;

                const modelId =
                    document.getElementById("model")?.value;
                const variantId =
                    document.getElementById("variant")?.value;

                this.resetTransmissions();

                if (!variantId || !fuelType ||!modelId) {
                    return;
                }

                await this.loadTransmissions(
                    modelId,
                    variantId,
                    fuelType
                );

            });


        /*
        -----------------------------------------
        Next
        -----------------------------------------
        */

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

       await this.loadBrands();

        // await Promise.all([
        //     this.loadBrands(),
        //     this.loadMetadata()
        // ]);

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

        if (!variantOption) {
            return;
        }

        variantSelect.value =
            variantOption.value;


        /*
        -----------------------------------------
        Load Fuel Types for Variant
        -----------------------------------------
        */

        await this.loadFuelTypes(
            document.getElementById("model")?.value,
            document.getElementById("variant")?.value
        );


        /*
        -----------------------------------------
        Select Existing Fuel Type
        -----------------------------------------
        */

        const fuelSelect =
            document.getElementById("fuelType");

        fuelSelect.value =
            vehicle.fuelType || "";


        /*
        -----------------------------------------
        Load Transmissions for Variant + Fuel
        -----------------------------------------
        */

        if (vehicle.fuelType) {

            await this.loadTransmissions(
                modelOption.value,
                variantOption.value,
                vehicle.fuelType
            );

        }


        /*
        -----------------------------------------
        Select Existing Transmission
        -----------------------------------------
        */

        const transmissionSelect =
            document.getElementById("transmission");

        transmissionSelect.value =
            vehicle.transmission || "";
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

    async loadFuelTypes(variantId,modelId) {

        const fuelSelect =
            document.getElementById("fuelType");

        fuelSelect.innerHTML =
            '<option value="">Select Fuel Type</option>';

        if (!variantId) {
            return;
        }

        try {

            const response =
                await VehicleMasterService.getFuelTypeDropdown(
                    modelId,
                    variantId
                );

            const fuelTypes =
                response.data || response;

            fuelTypes.forEach(fuel => {

                const value =
                    fuel.id || fuel;

                const name =
                    fuel.name ||
                    this.formatEnum(value);

                fuelSelect.innerHTML += `
                <option value="${value}">
                    ${name}
                </option>
            `;

            });

        }
        catch (e) {

            console.error(
                "Unable to load fuel types.",
                e
            );

        }

    },

    async loadTransmissions(
        modelId,
        variantId,
        fuelType
    ) {

        const transmissionSelect =
            document.getElementById("transmission");

        transmissionSelect.innerHTML =
            '<option value="">Select Transmission</option>';

        if (!variantId || !fuelType || !modelId) {
            return;
        }

        try {

            const response =
                await VehicleMasterService.getTransmissionDropdown(
                    modelId,
                    variantId,
                    fuelType
                );

            const transmissions =
                response.data || response;

            transmissions.forEach(type => {

                const value =
                    type.id || type;

                const name =
                    type.name ||
                    this.formatEnum(value);

                transmissionSelect.innerHTML += `
                <option value="${value}">
                    ${name}
                </option>
            `;

            });

        }
        catch (e) {

            console.error(
                "Unable to load transmissions.",
                e
            );

        }

    },

    resetVariants() {

        const select =
            document.getElementById("variant");

        if (!select) {
            return;
        }

        select.innerHTML =
            '<option value="">Select Variant</option>';

    },

    resetFuelTypes() {

        const select =
            document.getElementById("fuelType");

        if (!select) {
            return;
        }

        select.innerHTML =
            '<option value="">Select Fuel Type</option>';

    },

    resetTransmissions() {

        const select =
            document.getElementById("transmission");

        if (!select) {
            return;
        }

        select.innerHTML =
            '<option value="">Select Transmission</option>';

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
