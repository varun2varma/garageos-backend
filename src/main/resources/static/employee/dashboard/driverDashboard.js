window.DriverDashboard = {

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