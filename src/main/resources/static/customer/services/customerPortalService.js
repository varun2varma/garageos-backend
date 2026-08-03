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

    async getEstimate(estimateId) {
        return await Api.get(`/customer/estimates/${estimateId}`);
    },

    async approveEstimate(estimateId) {
        return await Api.put(`/estimates/${estimateId}/approve`);
    },

    async rejectEstimate(estimateId) {
        return await Api.put(`/estimates/${estimateId}/reject`);
    },

    async getInvoices() {
        return await Api.get("/customer/invoices");
    },

    async getRepairTracking(jobCardNumber) {
        return await Api.get(
            `/customer/repair-tracking/${jobCardNumber}`
        );
    },

    async payInvoice(jobCardNumber) {
        return await Api.post(`/workflow/${jobCardNumber}/payment`);
    }

};