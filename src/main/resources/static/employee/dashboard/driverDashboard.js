window.DriverDashboard = {

    trips: [],

    async loadTrips() {

        try {

            const trips =
                await NavigationTripService
                    .getMyTrips();

            this.trips =
                trips ?? [];

            this.renderTrips();

            this.updateStatistics();

        } catch (error) {

            console.error(
                "Unable to load driver trips:",
                error
            );

            const container =
                document.getElementById(
                    "driverTrips"
                );

            if (container) {

                container.innerHTML = `

                    <div class="alert alert-danger">

                        Unable to load trips.

                    </div>

                `;

            }

        }

    },


    renderTrips() {

        const container =
            document.getElementById(
                "driverTrips"
            );

        if (!container) {
            return;
        }


        if (!this.trips.length) {

            container.innerHTML = `

                <div class="text-center text-muted py-5">

                    <div class="fs-1 mb-3">
                        🚗
                    </div>

                    <h5>
                        No trips assigned
                    </h5>

                    <p class="mb-0">
                        You don't have any active trips.
                    </p>

                </div>

            `;

            return;
        }


        container.innerHTML =
            this.trips
                .map(
                    trip =>
                        this.renderTripCard(trip)
                )
                .join("");


        this.bindTripEvents();

    },

    renderTripCard(trip) {

        return `

            <div
                class="card border-0 shadow-sm mb-3"
            >

                <div class="card-body">

                    <div
                        class="d-flex
                               justify-content-between
                               align-items-start"
                    >

                        <div>

                            <h5 class="fw-bold mb-1">

                                🚗 Trip #${trip.id}

                            </h5>

                            <div class="text-muted">

                                Vehicle #${trip.vehicleId}

                            </div>

                        </div>

                        <span
                            class="${this.getTripStatusBadge(
                                trip.status
                            )}"
                        >

                            ${this.formatTripStatus(
                                trip.status
                            )}

                        </span>

                    </div>


                    <hr>


                    <div class="row">

                        <div class="col-md-5">

                            <div class="text-muted small">
                                From
                            </div>

                            <div class="fw-semibold">

                                ${trip.sourceAddress ?? "-"}

                            </div>

                        </div>


                        <div
                            class="col-md-2
                                   text-center
                                   align-self-center"
                        >

                            🚗

                        </div>


                        <div class="col-md-5">

                            <div class="text-muted small">
                                To
                            </div>

                            <div class="fw-semibold">

                                ${trip.destinationAddress ?? "-"}

                            </div>

                        </div>

                    </div>


                    <div class="mt-4">

                        ${this.renderTripAction(trip)}

                    </div>

                </div>

            </div>

        `;

    },

    renderTripAction(trip) {

        switch (trip.status) {

            case "ASSIGNED":

                return `

                    <button
                        class="btn btn-primary
                               accept-trip-btn"
                        data-trip-id="${trip.id}"
                    >

                        Accept Trip

                    </button>

                `;


            case "ACCEPTED":

                return `

                    <button
                        class="btn btn-success
                               start-trip-btn"
                        data-trip-id="${trip.id}"
                    >

                        🚗 Start Trip

                    </button>

                `;


            case "IN_PROGRESS":

                return `

                    <button
                        class="btn btn-primary
                               arrive-trip-btn"
                        data-trip-id="${trip.id}"
                    >

                        I Have Arrived

                    </button>

                `;


            default:

                return "";

        }

    },


    render() {

        return `

<div class="container-fluid">

    <!-- Header -->

    <div class="mb-4">

        <h2 class="fw-bold">

            Ready for today's trips 🚗

        </h2>

        <p class="text-muted">

            Manage your assigned trips.

        </p>

    </div>


    <!-- Statistics -->

    <div class="row g-3 mb-4">

        <div class="col-md-3">

            <div class="card shadow-sm border-0">

                <div class="card-body">

                    <div class="text-muted">

                        Pending Acceptance

                    </div>

                    <h2 id="driverPendingTrips">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0">

                <div class="card-body">

                    <div class="text-muted">

                        In Progress

                    </div>

                    <h2 id="driverActiveTrips">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0">

                <div class="card-body">

                    <div class="text-muted">

                        Completed

                    </div>

                    <h2 id="driverCompletedTrips">

                        0

                    </h2>

                </div>

            </div>

        </div>


        <div class="col-md-3">

            <div class="card shadow-sm border-0">

                <div class="card-body">

                    <div class="text-muted">

                        Ready to Start

                    </div>

                    <h2 id="driverReadyTrips">

                        0

                    </h2>

                </div>

            </div>

        </div>

    </div>


    <!-- Map -->

    <div class="card shadow-sm border-0 mb-4">

        <div class="card-header bg-white">

            <h4 class="mb-0">

                Live Trip Map 🚗

            </h4>

        </div>

        <div class="card-body p-0">

            <div

                id="driverMap"

                style="

                    height: 450px;

                    width: 100%;

                    border-radius: 0 0 8px 8px;

                "

            ></div>

        </div>

    </div>


    <!-- Trips -->

    <div class="card shadow-sm border-0">

        <div class="card-header bg-white">

            <h4 class="mb-0">

                Today's Assigned Trips

            </h4>

        </div>

        <div

            id="driverTrips"

            class="card-body"

        >

            <div class="text-center text-muted py-5">

                No trips assigned

            </div>

        </div>

    </div>

</div>

        `;

    },


    bindEvents() {

        this.initializeMap();

        this.loadTrips();

    },

    bindTripEvents() {

        document
            .querySelectorAll(
                ".accept-trip-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () =>
                        this.acceptTrip(
                            Number(
                                button.dataset.tripId
                            )
                        )
                );

            });


        document
            .querySelectorAll(
                ".start-trip-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () =>
                        this.startTrip(
                            Number(
                                button.dataset.tripId
                            )
                        )
                );

            });


        document
            .querySelectorAll(
                ".arrive-trip-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () =>
                        this.arriveTrip(
                            Number(
                                button.dataset.tripId
                            )
                        )
                );

            });

    },


    async acceptTrip(tripId) {

        try {

            await NavigationTripService
                .acceptTrip(tripId);

            await this.loadTrips();

        } catch (error) {

            console.error(
                "Unable to accept trip:",
                error
            );

            alert(
                "Unable to accept trip."
            );

        }

    },


    async startTrip(tripId) {

        try {

            await NavigationTripService
                .startTrip(tripId);

            await this.loadTrips();

            /*
             * GPS WebSocket will be started
             * after the trip becomes IN_PROGRESS.
             */

        } catch (error) {

            console.error(
                "Unable to start trip:",
                error
            );

            alert(
                "Unable to start trip."
            );

        }

    },

    async arriveTrip(tripId) {

        try {

            await NavigationTripService
                .arriveTrip(tripId);

            await this.loadTrips();

        } catch (error) {

            console.error(
                "Unable to update arrival:",
                error
            );

            alert(
                "Unable to update arrival."
            );

        }

    },


    updateStatistics() {

        const trips =
            this.trips ?? [];


        const pending =
            trips.filter(
                trip =>
                    trip.status === "ASSIGNED"
            ).length;


        const active =
            trips.filter(
                trip =>
                    trip.status === "IN_PROGRESS"
            ).length;


        const ready =
            trips.filter(
                trip =>
                    trip.status === "ACCEPTED"
            ).length;


        this.setStatistic(
            "driverPendingTrips",
            pending
        );

        this.setStatistic(
            "driverActiveTrips",
            active
        );

        this.setStatistic(
            "driverReadyTrips",
            ready
        );

    },


    setStatistic(id, value) {

        const element =
            document.getElementById(id);

        if (element) {

            element.textContent =
                value;

        }

    },

    getTripStatusBadge(status) {

        switch (status) {

            case "ASSIGNED":
                return "badge bg-warning text-dark";

            case "ACCEPTED":
                return "badge bg-info text-dark";

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


    formatTripStatus(status) {

        return (status ?? "")

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


    initializeMap() {

        const mapElement = document.getElementById(

            "driverMap"

        );

        if (!mapElement) {

            return;

        }


        // Default location

        const defaultLocation = [

            17.3850,

            78.4867

        ];


        const map = L.map(

            "driverMap"

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

        ).addTo(map);


        // Car icon

        const carIcon = L.divIcon({

            className: "driver-car-marker",

            html: `

                <div style="

                    font-size: 32px;

                    transform: translate(-50%, -50%);

                ">

                    🚗

                </div>

            `,

            iconSize: [40, 40],

            iconAnchor: [20, 20]

        });


        let driverMarker = null;


        // Get driver's current location

        if (navigator.geolocation) {

            navigator.geolocation.watchPosition(

                position => {

                    const latitude =

                        position.coords.latitude;

                    const longitude =

                        position.coords.longitude;


                    const location = [

                        latitude,

                        longitude

                    ];


                    if (!driverMarker) {

                        driverMarker =

                            L.marker(

                                location,

                                {

                                    icon: carIcon

                                }

                            )

                            .addTo(map);


                        map.setView(

                            location,

                            16

                        );

                    } else {

                        driverMarker.setLatLng(

                            location

                        );

                    }

                },

                error => {

                    console.error(

                        "Unable to get driver location:",

                        error

                    );

                },

                {

                    enableHighAccuracy: true,

                    maximumAge: 5000,

                    timeout: 10000

                }

            );

        } else {

            console.error(

                "Geolocation is not supported."

            );

        }

    }

};