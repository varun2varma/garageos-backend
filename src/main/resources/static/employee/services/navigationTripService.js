/*
==========================================================
 GarageOS Navigation Trip Service
==========================================================
*/

window.NavigationTripService = {

    async getMyTrips() {

        const driverId =
            EmployeeSession.id();

        return Api.get(
            `/navigation/trips/driver/${driverId}`
        );

    },


    async getTrip(tripId) {

        return Api.get(
            `/navigation/trips/${tripId}`
        );

    },


    async acceptTrip(tripId) {

        const driverId =
            EmployeeSession.id();

        return Api.post(
            `/navigation/trips/${tripId}/accept?driverId=${driverId}`
        );

    },


    async startTrip(tripId) {

        const driverId =
            EmployeeSession.id();

        return Api.post(
            `/navigation/trips/${tripId}/start?driverId=${driverId}`
        );

    },


    async arriveTrip(tripId) {

        const driverId =
            EmployeeSession.id();

        return Api.post(
            `/navigation/trips/${tripId}/arrive?driverId=${driverId}`
        );

    },


    async continueTrip(tripId) {

        const driverId =
            EmployeeSession.id();

        return Api.post(
            `/navigation/trips/${tripId}/continue?driverId=${driverId}`
        );

    },


    async completeTrip(tripId) {

        const driverId =
            EmployeeSession.id();

        return Api.post(
            `/navigation/trips/${tripId}/complete?driverId=${driverId}`
        );

    }

};