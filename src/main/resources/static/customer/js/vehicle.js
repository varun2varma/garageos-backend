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

            if (!VehicleService.getMyVehicles) {

                this.render([]);

                return;

            }

            this.vehicles =
                await VehicleService.getMyVehicles();

            this.render(this.vehicles);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(vehicles) {

        const grid =
            document.getElementById(
                "vehicleGrid"
            );

        if (!vehicles.length) {

            grid.innerHTML = `

                <div class="col-12">

                    <div class="customer-card empty-vehicle">

                        <i class="bi bi-car-front"></i>

                        <h4>

                            No Vehicles Found

                        </h4>

                        <p>

                            You haven't registered any vehicles yet.

                        </p>

                    </div>

                </div>

            `;

            return;

        }

        grid.innerHTML =
            vehicles.map(vehicle => `

                <div class="col-lg-4 col-md-6">

                    <div class="vehicle-card">

                        <div class="vehicle-header">

                            <div class="vehicle-registration">

                                ${vehicle.registrationNumber}

                            </div>

                            <div>

                                ${vehicle.make}
                                ${vehicle.model}

                            </div>

                        </div>

                        <div class="vehicle-body">

                            <div class="vehicle-row">

                                <span class="vehicle-label">

                                    Fuel

                                </span>

                                <span class="vehicle-value">

                                    ${vehicle.fuelType || "-"}

                                </span>

                            </div>

                            <div class="vehicle-row">

                                <span class="vehicle-label">

                                    Transmission

                                </span>

                                <span class="vehicle-value">

                                    ${vehicle.transmission || "-"}

                                </span>

                            </div>

                            <div class="vehicle-row">

                                <span class="vehicle-label">

                                    Year

                                </span>

                                <span class="vehicle-value">

                                    ${vehicle.manufacturingYear || "-"}

                                </span>

                            </div>

                            <div class="vehicle-row">

                                <span class="vehicle-label">

                                    Odometer

                                </span>

                                <span class="vehicle-value">

                                    ${vehicle.odometerReading || 0} km

                                </span>

                            </div>

                        </div>

                    </div>

                </div>

            `).join("");

    }

};