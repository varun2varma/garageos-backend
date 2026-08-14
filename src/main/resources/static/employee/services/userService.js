window.UserService = {

    async getTechnicians() {

        return await Api.get(
            "/users/technicians"
        );

    },

    async getRepairEmployees() {

        const response =
            await Api.get("/users/repair-employees");

        return response.data || response;

    }

};