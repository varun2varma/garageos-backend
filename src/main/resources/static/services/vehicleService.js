window.VehicleService = {

    async createVehicle(request) {

        return await Api.post(
            "/vehicles",
            request
        );

    },

    async updateVehicle(id, request) {

        return await Api.put(
            "/vehicles/" + id,
            request
        );

    },

    async searchByRegistrationNumber(registrationNumber) {

        return await Api.get(
            "/vehicles/search?registrationNumber=" +
            encodeURIComponent(registrationNumber)
        );

    }

};