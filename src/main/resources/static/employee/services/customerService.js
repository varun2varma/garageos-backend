window.CustomerService = {

    async createCustomer(customer) {

        return await Api.post("/customers", customer);

    },

    async updateCustomer(id, customer) {

        return await Api.put("/customers/" + id, customer);

    },

    async findByMobile(mobileNumber) {

        console.log("Searching Mobile:", mobileNumber);

        return await Api.get(
            "/customers/search?mobileNumber=" + encodeURIComponent(mobileNumber)
        );
    },

    async getAll(page = 0, size = 10) {

        return await Api.get(
            `/customers?page=${page}&size=${size}`
        );

    },

    async getById(id) {

            return await Api.get(
                `/customers/${id}`
            );

        },

        async delete(id) {

            return await Api.delete(
                `/customers/${id}`
            );

        },

};