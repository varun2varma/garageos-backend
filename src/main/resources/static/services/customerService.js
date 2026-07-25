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
    }

};