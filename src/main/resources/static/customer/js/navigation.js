window.CustomerNavigation = {

    selectedVehicle: null,

    requests: [],

    pickupMap: null,

    pickupMarker: null,

    pickupLocation: {

        address: "",

        latitude: null,

        longitude: null

    },

    async init() {

        this.bindEvents();

        this.loadSelectedVehicle();

        await this.loadGarages();

        this.initPickupMap();

        await this.loadRequests();

        this.updateRequestTypeUI();

    },


    bindEvents() {

        document
            .getElementById(
                "navigationRequestForm"
            )
            ?.addEventListener(
                "submit",
                event => {

                    event.preventDefault();

                    this.submitRequest();

                }
            );


        document
            .querySelectorAll(
                'input[name="navigationRequestType"]'
            )
            .forEach(radio => {

                radio.addEventListener(
                    "change",
                    () => this.updateRequestTypeUI()
                );

            });


        document
            .getElementById(
                "refreshNavigationRequests"
            )
            ?.addEventListener(
                "click",
                () => this.loadRequests()
            );

    },


    loadSelectedVehicle() {

        const vehicleId =
            Number(
                sessionStorage.getItem(
                    "navigationVehicleId"
                )
            );


        if (!vehicleId) {

            this.renderNoVehicle();

            return;

        }


        /*
        ==================================================
         CustomerVehicle may not be loaded because the
         router loads only navigation.js.
         So load vehicles through the existing service.
        ==================================================
        */

        CustomerPortalService
            .getVehicles()
            .then(
                vehicles => {

                    this.selectedVehicle =
                        (vehicles ?? []).find(
                            vehicle =>
                                vehicle.id ===
                                vehicleId
                        );


                    if (
                        !this.selectedVehicle
                    ) {

                        this.renderNoVehicle();

                        return;

                    }


                    this.renderSelectedVehicle();

                }
            )
            .catch(
                error => {

                    console.error(
                        "Unable to load selected vehicle:",
                        error
                    );

                    this.renderNoVehicle();

                }
            );

    },


    renderSelectedVehicle() {

        const container =
            document.getElementById(
                "navigationSelectedVehicle"
            );


        if (!container) {
            return;
        }


        const vehicle =
            this.selectedVehicle;


        container.innerHTML = `

            <div class="d-flex align-items-center">

                <div
                    class="rounded-circle
                           bg-primary
                           text-white
                           d-flex
                           align-items-center
                           justify-content-center
                           me-3"
                    style="
                        width: 50px;
                        height: 50px;
                    ">

                    <i class="bi bi-car-front-fill"></i>

                </div>


                <div>

                    <div class="fw-bold">

                        ${vehicle.brand}
                        ${vehicle.model}

                    </div>


                    <div class="text-muted">

                        ${
                            vehicle.variant
                            ?? ""
                        }

                    </div>


                    <div class="fw-semibold mt-1">

                        ${vehicle.registrationNumber}

                    </div>

                </div>

            </div>

        `;

    },


    renderNoVehicle() {

        const container =
            document.getElementById(
                "navigationSelectedVehicle"
            );


        if (!container) {
            return;
        }


        container.innerHTML = `

            <div
                class="alert
                       alert-warning
                       mb-0">

                <i
                    class="bi bi-exclamation-triangle me-2">
                </i>

                Please select a vehicle from
                <strong>My Vehicles</strong>
                before requesting pickup or delivery.

            </div>

        `;


        const form =
            document.getElementById(
                "navigationRequestForm"
            );


        if (form) {

            form
                .querySelector(
                    'button[type="submit"]'
                )
                ?.setAttribute(
                    "disabled",
                    "disabled"
                );

        }

    },


    updateRequestTypeUI() {

        const selected =
            document.querySelector(
                'input[name="navigationRequestType"]:checked'
            );


        const type =
            selected?.value;


        const pickup =
            document.getElementById(
                "pickupAddressSection"
            );


        const delivery =
            document.getElementById(
                "deliveryAddressSection"
            );


        if (type === "PICKUP") {

            pickup?.classList.remove(
                "d-none"
            );

            delivery?.classList.add(
                "d-none"
            );

            return;

        }


        if (type === "DELIVERY") {

            pickup?.classList.add(
                "d-none"
            );

            delivery?.classList.remove(
                "d-none"
            );

        }

    },

    initPickupMap() {

        const mapElement =
            document.getElementById(
                "pickupMap"
            );

        if (!mapElement) {

            console.warn(
                "Pickup map element not found."
            );

            return;

        }

        if (this.pickupMap) {

            return;

        }

        /*
        ==================================================
         Default location - Hyderabad
        ==================================================
        */

        const defaultLocation = [

            17.4485,
            78.3908

        ];


        this.pickupMap =
            L.map(
                "pickupMap"
            ).setView(
                defaultLocation,
                13
            );


        L.tileLayer(
            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            {

                attribution:
                    '&copy; OpenStreetMap contributors'

            }
        ).addTo(
            this.pickupMap
        );


        /*
        ==================================================
         Map click
        ==================================================
        */

        this.pickupMap.on(
            "click",
            event => {

                this.selectPickupLocation(
                    event.latlng.lat,
                    event.latlng.lng
                );

            }
        );


        /*
        ==================================================
         Try driver's/customer browser location
         as initial map center
        ==================================================
        */

        if (navigator.geolocation) {

            navigator.geolocation.getCurrentPosition(

                position => {

                    const latitude =
                        position.coords.latitude;

                    const longitude =
                        position.coords.longitude;


                    this.pickupMap.setView(

                        [
                            latitude,
                            longitude
                        ],

                        15

                    );

                },

                error => {

                    console.log(
                        "Unable to get current location.",
                        error
                    );

                }

            );

        }

    },

    selectPickupLocation(
        latitude,
        longitude
    ) {

        latitude =
            Number(
                latitude.toFixed(7)
            );

        longitude =
            Number(
                longitude.toFixed(7)
            );


        /*
        ==================================================
         Remove old marker
        ==================================================
        */

        if (this.pickupMarker) {

            this.pickupMap.removeLayer(
                this.pickupMarker
            );

        }


        /*
        ==================================================
         Create marker
        ==================================================
        */

        this.pickupMarker =
            L.marker(
                [
                    latitude,
                    longitude
                ]
            )
            .addTo(
                this.pickupMap
            );


        /*
        ==================================================
         Store selected location
        ==================================================
        */

        this.pickupLocation = {

            address: "",

            latitude,

            longitude

        };


        /*
        ==================================================
         Store in hidden fields
        ==================================================
        */

        const latitudeInput =
            document.getElementById(
                "pickupLatitude"
            );

        const longitudeInput =
            document.getElementById(
                "pickupLongitude"
            );

        if (latitudeInput) {

            latitudeInput.value =
                latitude;

        }

        if (longitudeInput) {

            longitudeInput.value =
                longitude;

        }


        /*
        ==================================================
         Show coordinates immediately
        ==================================================
        */

        this.renderPickupLocationInfo();


        /*
        ==================================================
         Resolve human-readable address
        ==================================================
        */

        this.reverseGeocodePickupLocation(

            latitude,

            longitude

        );

    },


    getCustomerId() {

        const user =
            Auth.getUser?.() ?? {};


        return user.customerId
            ?? user.id;

    },


    buildRequestPayload() {

        const garageId =
            Number(
                document.getElementById(
                    "navigationGarage"
                )?.value
            );


        const requestType =
            document.querySelector(
                'input[name="navigationRequestType"]:checked'
            )?.value;


        const pickupAddress =
            document.getElementById(
                "navigationPickupAddress"
            )?.value
            ?.trim();


        const deliveryAddress =
            document.getElementById(
                "navigationDeliveryAddress"
            )?.value
            ?.trim();


        const scheduledAt =
            document.getElementById(
                "navigationScheduledAt"
            )?.value;


        return {

            vehicleId:
                this.selectedVehicle?.id,

            garageId,

            requestType,

            pickupAddress,

            deliveryAddress,

            scheduledAt

        };

    },


    async submitRequest() {

        if (!this.selectedVehicle) {

            alert(
                "Please select a vehicle from My Vehicles."
            );

            return;

        }


        const customerId =
            this.getCustomerId();


        if (!customerId) {

            alert(
                "Unable to identify customer."
            );

            return;

        }


        const payload =
            this.buildRequestPayload();


        if (!payload.garageId) {

            alert(
                "Please select a garage."
            );

            return;

        }


        if (!payload.requestType) {

            alert(
                "Please select Pickup or Delivery."
            );

            return;

        }


        if (!payload.scheduledAt) {

            alert(
                "Please select a preferred time."
            );

            return;

        }


        if (
            payload.requestType === "PICKUP"
            &&
            !payload.pickupAddress
        ) {

            alert(
                "Please enter the pickup address."
            );

            return;

        }


        if (
            payload.requestType === "DELIVERY"
            &&
            !payload.deliveryAddress
        ) {

            alert(
                "Please enter the delivery address."
            );

            return;

        }


        const button =
            document
                .getElementById(
                    "navigationRequestForm"
                )
                ?.querySelector(
                    'button[type="submit"]'
                );


        try {

            if (button) {

                button.disabled = true;

                button.innerHTML = `

                    <span
                        class="spinner-border
                               spinner-border-sm
                               me-2">
                    </span>

                    Submitting...

                `;

            }


            await CustomerNavigationService
                .createRequest(
                    customerId,
                    payload
                );


            alert(
                "Pickup / delivery request submitted successfully."
            );


            document
                .getElementById(
                    "navigationRequestForm"
                )
                ?.reset();


            this.updateRequestTypeUI();

            await this.loadRequests();

        } catch (error) {

            console.error(
                "Navigation request failed:",
                error
            );


            alert(
                error.message
                ||
                "Unable to submit pickup / delivery request."
            );

        } finally {

            if (button) {

                button.disabled = false;

                button.innerHTML = `

                    <i
                        class="bi bi-truck me-2">
                    </i>

                    Submit Request

                `;

            }

        }

    },


    async loadRequests() {

        const customerId =
            this.getCustomerId();


        if (!customerId) {
            return;
        }


        try {

            this.requests =
                await CustomerNavigationService
                    .getMyRequests(
                        customerId
                    );


            this.renderRequests();

        } catch (error) {

            console.error(
                "Unable to load navigation requests:",
                error
            );


            const container =
                document.getElementById(
                    "customerNavigationRequests"
                );


            if (container) {

                container.innerHTML = `

                    <div
                        class="alert alert-danger">

                        Unable to load
                        pickup and delivery requests.

                    </div>

                `;

            }

        }

    },


    renderRequests() {

        const container =
            document.getElementById(
                "customerNavigationRequests"
            );


        if (!container) {
            return;
        }


        if (!this.requests.length) {

            container.innerHTML = `

                <div
                    class="text-center
                           text-muted
                           py-5">

                    <i
                        class="bi bi-truck
                               display-5">
                    </i>

                    <p class="mt-3 mb-0">

                        No pickup or delivery
                        requests yet.

                    </p>

                </div>

            `;

            return;

        }


        container.innerHTML =
            this.requests
                .map(
                    request =>
                        this.renderRequest(
                            request
                        )
                )
                .join("");

    },


    renderRequest(request) {

        const isPickup =
            request.requestType === "PICKUP";


        return `

            <div
                class="border
                       rounded
                       p-3
                       mb-3">

                <div
                    class="d-flex
                           justify-content-between
                           align-items-start">

                    <div>

                        <div class="fw-bold">

                            ${
                                isPickup
                                    ? "🚗 Pickup"
                                    : "🚙 Delivery"
                            }

                        </div>


                        <small class="text-muted">

                            Request #${request.id}

                        </small>

                    </div>


                    <span
                        class="${this.getStatusBadge(
                            request.status
                        )}">

                        ${this.formatStatus(
                            request.status
                        )}

                    </span>

                </div>


                <div class="mt-3">

                    <div class="small text-muted">

                        Vehicle

                    </div>

                    <div class="fw-semibold">

                        ${
                            request.vehicleRegistrationNumber
                            ??
                            request.vehicleId
                            ??
                            "-"
                        }

                    </div>

                </div>


                ${
                    request.scheduledAt
                        ? `

                            <div class="mt-2">

                                <div
                                    class="small text-muted">

                                    Scheduled

                                </div>

                                <div>

                                    ${this.formatDate(
                                        request.scheduledAt
                                    )}

                                </div>

                            </div>

                        `
                        : ""
                }


                ${
                    request.tripId
                        ? `

                            <button
                                type="button"
                                class="btn
                                       btn-sm
                                       btn-outline-primary
                                       mt-3"
                                onclick="
                                    CustomerNavigation.trackTrip(
                                        ${request.tripId}
                                    )
                                ">

                                <i
                                    class="bi bi-geo-alt me-1">
                                </i>

                                Track Vehicle

                            </button>

                        `
                        : ""
                }

            </div>

        `;

    },


    async trackTrip(tripId) {

        sessionStorage.setItem(
            "navigationTripId",
            String(tripId)
        );


        /*
         * Live map will be plugged into this
         * method next.
         */

        console.log(
            "Tracking navigation trip:",
            tripId
        );


        CustomerRouter.navigate(
            "navigation"
        );

    },


    getStatusBadge(status) {

        switch (status) {

            case "REQUESTED":
                return "badge bg-warning text-dark";

            case "ASSIGNED":
                return "badge bg-info text-dark";

            case "ACCEPTED":
                return "badge bg-primary";

            case "IN_PROGRESS":
                return "badge bg-primary";

            case "COMPLETED":
                return "badge bg-success";

            case "CANCELLED":
                return "badge bg-danger";

            default:
                return "badge bg-secondary";

        }

    },


    formatStatus(status) {

        return (
            status ?? ""
        )
            .replaceAll(
                "_",
                " "
            )
            .toLowerCase()
            .replace(
                /\b\w/g,
                c => c.toUpperCase()
            );

    },


    formatDate(value) {

        if (!value) {
            return "-";
        }


        return new Date(
            value
        ).toLocaleString();

    },

    async loadGarages() {

        const select =
            document.getElementById(
                "navigationGarage"
            );


        if (!select) {

            return;

        }


        try {

            select.innerHTML = `

                <option value="">

                    Loading garages...

                </option>

            `;


            const response =
                await CustomerNavigationService
                    .getGarages();


            console.log(
                "Garages response:",
                response
            );


            /*
            ==================================================
             Handle ApiResponse
            ==================================================
            */

            const garages =
                response?.data
                ?? response
                ?? [];


            if (!garages.length) {

                select.innerHTML = `

                    <option value="">

                        No garages available

                    </option>

                `;

                return;

            }


            select.innerHTML = `

                <option value="">

                    Select Garage

                </option>

                ${
                    garages
                        .map(
                            garage => `

                                <option
                                    value="${garage.id}"
                                >

                                    ${
                                        garage.name
                                        ?? garage.garageName
                                        ?? `Garage #${garage.id}`
                                    }

                                    ${
                                        garage.code
                                            ? ` (${garage.code})`
                                            : ""
                                    }

                                </option>

                            `
                        )
                        .join("")
                }

            `;

        } catch (error) {

            console.error(
                "Unable to load garages:",
                error
            );


            select.innerHTML = `

                <option value="">

                    Unable to load garages

                </option>

            `;

        }

    },

};