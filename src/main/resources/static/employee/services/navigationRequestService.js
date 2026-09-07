/*
==========================================================
 GarageOS Navigation Request Service
 Manager
==========================================================
*/

window.NavigationRequestService = {

    async getGarageRequests(garageId) {

        return Api.get(
            `/navigation/requests/garage/${garageId}`
        );

    },


    async getRequest(requestId) {

        return Api.get(
            `/navigation/requests/${requestId}`
        );

    },


    async assignDriver(
        navigationRequestId,
        driverId
    ) {

        return Api.post(
            "/navigation/trips",
            {
                navigationRequestId:
                    navigationRequestId,

                driverId:
                    driverId
            }
        );

    }

};