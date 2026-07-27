window.JobCardService = {

    async createJob(request) {

        return await Api.post(
            "/workflow/job",
            request
        );

    },

    async updateJob(id, request) {

        return await Api.put(
            "/job-cards/" + id,
            request
        );

    },

    async getAll(page = 0, size = 10) {

        return await Api.get(
            `/jobcards?page=${page}&size=${size}`
        );

    },

};