window.CustomerVehicle = {

    vehicles: [],

    async init() {

        document
            .getElementById("refreshVehiclesButton")
            ?.addEventListener(
                "click",
                () => this.loadVehicles()
            );

        await this.loadVehicles();

    },

    async loadVehicles() {

        try {

            CustomerApp.showLoading();

            this.vehicles =
                await CustomerPortalService.getVehicles();

            this.render(this.vehicles);

        } catch (e) {

            console.error(e);

            this.render([]);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(vehicles) {

        const grid =
            document.getElementById(
                "vehicleGrid"
            );

        if (!grid) {

            return;

        }

        if (!vehicles.length) {


            document.getElementById(
                "totalVehicleCount"
            ).textContent =
                vehicles.length;

            const petrolDiesel =
                vehicles.filter(v =>

                    v.fuelType === "PETROL"
                    ||
                    v.fuelType === "DIESEL"

                ).length;

            const electricHybrid =
                vehicles.filter(v =>

                    v.fuelType === "ELECTRIC"
                    ||
                    v.fuelType === "HYBRID"

                ).length;

            document.getElementById(
                "petrolVehicleCount"
            ).textContent =
                petrolDiesel;

            document.getElementById(
                "electricVehicleCount"
            ).textContent =
                electricHybrid;

            grid.innerHTML = `

                <div class="col-12">

                    <div class="customer-card empty-state text-center p-5">

                        <i class="bi bi-car-front display-3 text-secondary"></i>

                        <h4 class="mt-3">

                            No Vehicles Found

                        </h4>

                        <p class="text-muted">

                            You haven't registered any vehicles yet.

                        </p>

                    </div>

                </div>

            `;

            return;

        }

        grid.innerHTML = vehicles
            .map(vehicle => this.buildCard(vehicle))
            .join("");

    },

    buildCard(vehicle) {

        return `

        <div class="col-xl-4 col-lg-6">

            <div class="customer-card vehicle-card h-100">

                <div class="vehicle-banner">

                    <i class="bi bi-car-front-fill"></i>

                </div>

                <div class="vehicle-content">

                    <div class="text-center">

                        <h4>

                            ${vehicle.registrationNumber}

                        </h4>

                        <div class="text-muted">

                            ${vehicle.brand}

                            ${vehicle.model}

                        </div>

                        <span class="badge bg-primary mt-2">

                            ${vehicle.variant ?? "-"}

                        </span>

                    </div>

                    <hr>

                    ${this.row(
                        "Manufacturing Year",
                        vehicle.manufacturingYear
                    )}

                    ${this.row(
                        "Fuel Type",
                        this.getFuel(vehicle.fuelType)
                    )}

                    ${this.row(
                        "Transmission",
                        this.getTransmission(vehicle.transmission)
                    )}

                    ${this.row(
                        "Color",
                        vehicle.color
                    )}

                </div>

            </div>

        </div>

        `;

    },

    row(label, value) {

        return `

            <div class="d-flex justify-content-between py-2 border-bottom">

                <span class="text-muted">

                    ${label}

                </span>

                <strong>

                    ${value ?? "-"}

                </strong>

            </div>

        `;

    },

    getFuel(fuel) {

        if (!fuel) {

            return "-";

        }

        switch (fuel) {

            case "PETROL":

                return "⛽ Petrol";

            case "DIESEL":

                return "🛢 Diesel";

            case "CNG":

                return "🔥 CNG";

            case "ELECTRIC":

                return "⚡ Electric";

            case "HYBRID":

                return "🔋 Hybrid";

            default:

                return fuel;

        }

    },

    getTransmission(type) {

        if (!type) {

            return "-";

        }

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

                return type;

        }

    }

};