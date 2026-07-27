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

    },

    async getAll(page = 0, size = 10) {

        return await Api.get(
            `/vehicles?page=${page}&size=${size}`
        );

    },

    async getById(id) {

            return await Api.get(
                `/vehicles/${id}`
            );

        },

        async delete(id) {

            return await Api.delete(
                `/vehicles/${id}`
            );

        },

};