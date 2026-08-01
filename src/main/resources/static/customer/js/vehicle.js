window.CustomerVehicle = {

    vehicles: [],

    jobCards: [],

    estimates: [],

    invoices: [],

    selectedVehicle: null,

    async init() {

        document

            .getElementById(
                "refreshVehiclesButton"
            )

            ?.addEventListener(

                "click",

                () => this.load()

            );

        document

            .getElementById(
                "vehicleSearch"
            )

            ?.addEventListener(

                "keyup",

                (event) =>

                    this.filterVehicles(
                        event.target.value
                    )

            );

        await this.load();

    },

    async load() {

        try {

            console.log("STEP 1");

            CustomerApp.showLoading();

            const vehicles =
                await CustomerPortalService.getVehicles();

            console.log("Vehicles", vehicles);

            const jobCards =
                await CustomerPortalService.getJobCards();

            console.log("JobCards", jobCards);

            const estimates =
                await CustomerPortalService.getEstimates();

            console.log("Estimates", estimates);

            const invoices =
                await CustomerPortalService.getInvoices();

            console.log("Invoices", invoices);

            this.vehicles = vehicles;

            console.log("STEP 2");

            this.jobCards = jobCards;

            this.estimates = estimates;

            this.invoices = invoices;

            console.log("STEP 3");

            this.loadStatistics();

            console.log("STEP 4");

            this.renderVehicleTable(this.vehicles);

            console.log("STEP 5");

        }

        catch(e){

            console.error("LOAD ERROR", e);

        }

        finally{

            CustomerApp.hideLoading();

        }

    },

        loadStatistics() {

            document.getElementById(

                "totalVehicleCount"

            ).textContent =

                this.vehicles.length;

            document.getElementById(

                "vehicleListCount"

            ).textContent =

                this.vehicles.length;

            document.getElementById(

                "petrolVehicleCount"

            ).textContent =

                this.vehicles.filter(vehicle =>

                    vehicle.fuelType === "PETROL"

                    ||

                    vehicle.fuelType === "DIESEL"

                ).length;

            document.getElementById(

                "electricVehicleCount"

            ).textContent =

                this.vehicles.filter(vehicle =>

                    vehicle.fuelType === "ELECTRIC"

                    ||

                    vehicle.fuelType === "HYBRID"

                ).length;

            document.getElementById(

                "activeRepairCount"

            ).textContent =

                this.jobCards.filter(job =>

                    job.status !== "DELIVERED"

                ).length;

        },

            renderVehicleTable(vehicles) {

                const body =

                    document.getElementById(

                        "vehicleTableBody"

                    );

                if (!vehicles.length) {

                    body.innerHTML = `

        <tr>

        <td colspan="4"

        class="text-center py-5">

        No Vehicles Found

        </td>

        </tr>

        `;

                    return;

                }

                body.innerHTML =

                    vehicles.map(

                        vehicle => `

        <tr>

        <td>

        <strong>

        ${vehicle.registrationNumber}

        </strong>

        </td>

        <td>

        ${vehicle.brand}

        </td>

        <td>

        ${vehicle.model}

        </td>

        <td class="text-end">

        <button

        class="btn btn-sm btn-primary"

        onclick="CustomerVehicle.selectVehicle(${vehicle.id})">

        View

        </button>

        </td>

        </tr>

        `

                    ).join("");

        },

            filterVehicles(searchText) {

                const keyword =

                    searchText
                        .toLowerCase()
                        .trim();

                const filtered =

                    this.vehicles.filter(vehicle =>

                        vehicle.registrationNumber
                            .toLowerCase()
                            .includes(keyword)

                        ||

                        vehicle.brand
                            .toLowerCase()
                            .includes(keyword)

                        ||

                        vehicle.model
                            .toLowerCase()
                            .includes(keyword)

                    );

                this.renderVehicleTable(filtered);

            },

            selectVehicle(vehicleId) {

                this.selectedVehicle =

                    this.vehicles.find(

                        vehicle =>

                            vehicle.id === vehicleId

                    );

                if (!this.selectedVehicle) {

                    return;

                }

                const jobCards =

                    this.jobCards.filter(

                        job =>

                            job.registrationNumber ===
                            this.selectedVehicle.registrationNumber

                    );

                this.renderVehicleDetails(

                    this.selectedVehicle,

                    jobCards

                );

            },

            renderVehicleDetails(vehicle, jobCards) {

                document.getElementById(

                    "vehicleRegistration"

                ).textContent =

                    vehicle.registrationNumber;

                document.getElementById(

                    "vehicleTitle"

                ).textContent =

                    `${vehicle.brand} ${vehicle.model} ${vehicle.variant ?? ""}`;

                document.getElementById(

                    "detailBrand"

                ).textContent =

                    vehicle.brand;

                document.getElementById(

                    "detailModel"

                ).textContent =

                    vehicle.model;

                document.getElementById(

                    "detailVariant"

                ).textContent =

                    vehicle.variant ?? "-";

                document.getElementById(

                    "detailYear"

                ).textContent =

                    vehicle.manufacturingYear ?? "-";

                document.getElementById(

                    "detailFuel"

                ).textContent =

                    this.getFuel(

                        vehicle.fuelType

                    );

                document.getElementById(

                    "detailTransmission"

                ).textContent =

                    this.getTransmission(

                        vehicle.transmission

                    );

                document.getElementById(

                    "detailColor"

                ).textContent =

                    vehicle.color ?? "-";


                        document.getElementById(

                            "jobCardCount"

                        ).textContent =

                            jobCards.length;

                        document.getElementById(

                            "repairCount"

                        ).textContent =

                            jobCards.length;

                        document.getElementById(

                            "estimateCount"

                        ).textContent =

                            this.estimates.filter(

                                estimate =>

                                    jobCards.some(

                                        job =>

                                            job.jobCardNumber ===
                                            estimate.jobCardNumber

                                    )

                            ).length;

                        document.getElementById(

                            "invoiceCount"

                        ).textContent =

                            this.invoices.length;

                        this.renderJobCards(

                            jobCards

                        );

                    },


                    renderJobCards(jobCards) {

                        const container =

                            document.getElementById(

                                "jobCardHistory"

                            );

                        if (!jobCards.length) {

                            container.innerHTML = `

                                <div class="alert alert-light text-center mb-0">

                                    No service history available for this vehicle.

                                </div>

                            `;

                            return;

                        }

                        container.innerHTML =

                            jobCards.map(job => `

                                <div class="card mb-3 border-0 shadow-sm">

                                    <div class="card-body">

                                        <div class="d-flex justify-content-between align-items-center">

                                            <div>

                                                <h6 class="mb-1">

                                                    ${job.jobCardNumber}

                                                </h6>

                                                <small class="text-muted">

                                                    Service Date :

                                                    ${job.serviceDate ?? "-"}

                                                </small>

                                            </div>

                                            <span class="badge bg-primary">

                                                ${this.formatStatus(job.status)}

                                            </span>

                                        </div>

                                        <hr>

                                        <div class="row text-center">

                                            <div class="col">

                                                <a href="#"

                                                   onclick="CustomerRouter.navigate('repair')">

                                                    View Repair

                                                </a>

                                            </div>

                                            <div class="col">

                                                <a href="#"

                                                   onclick="CustomerRouter.navigate('estimates')">

                                                    View Estimate

                                                </a>

                                            </div>

                                            <div class="col">

                                                <a href="#"

                                                   onclick="CustomerRouter.navigate('invoices')">

                                                    View Invoice

                                                </a>

                                            </div>

                                        </div>

                                    </div>

                                </div>

                            `).join("");

                    },

        getFuel(fuel) {

            switch (fuel) {

                case "PETROL":

                    return "Petrol";

                case "DIESEL":

                    return "Diesel";

                case "CNG":

                    return "CNG";

                case "ELECTRIC":

                    return "Electric";

                case "HYBRID":

                    return "Hybrid";

                default:

                    return "-";

            }

        },


            getTransmission(type) {

                switch (type) {

                    case "MANUAL":

                        return "Manual";

                    case "AUTOMATIC":

                        return "Automatic";

                    case "AMT":

                        return "AMT";

                    case "CVT":

                        return "CVT";

                    case "DCT":

                        return "DCT";

                    default:

                        return "-";

                }

            },


                formatStatus(status) {

                    if (!status) {

                        return "-";

                    }

                    return status

                        .replaceAll("_", " ")

                        .toLowerCase()

                        .replace(
                            /\b\w/g,
                            c => c.toUpperCase()
                        );

    }

};