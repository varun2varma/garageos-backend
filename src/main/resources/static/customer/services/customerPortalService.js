window.CustomerPortalService = {

    async getProfile() {
        return await Api.get("/customer/profile");
    },

    async getDashboard() {
        return await Api.get("/customer/dashboard");
    },

    async getVehicles() {
        return await Api.get("/customer/vehicles");
    },

    async getJobCards() {
        return await Api.get("/customer/jobcards");
    },

    async getEstimates() {
        return await Api.get("/customer/estimates");
    },

    async getInvoices() {
        return await Api.get("/customer/invoices");
    },

    async getRepairTracking(jobCardNumber) {
        return await Api.get(
            `/customer/repair-tracking/${jobCardNumber}`
        );
    }

};