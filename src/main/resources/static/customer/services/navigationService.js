/*
==========================================================
 GarageOS Customer Navigation Service
==========================================================
*/

window.CustomerNavigationService = {

    async createRequest(
        customerId,
        request
    ) {

        return Api.post(
            `/navigation/requests?customerId=${customerId}`,
            request
        );

    },


    async getMyRequests(
        customerId
    ) {

        return Api.get(
            `/navigation/requests/customer/${customerId}`
        );

    },


    async getRequest(
        requestId
    ) {

        return Api.get(
            `/navigation/requests/${requestId}`
        );

    },


    async getTrip(
        tripId
    ) {

        return Api.get(
            `/navigation/trips/${tripId}`
        );

    },

    async getGarages() {

        return Api.get(
            "/garages"
        );

    },

};