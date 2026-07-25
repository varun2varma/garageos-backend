window.DashboardService = {

    async getSummary() {
        return await Api.get("/dashboard/summary");
    },

    async getRecentJobs() {
        return await Api.get("/dashboard/recent-jobs");
    }

};