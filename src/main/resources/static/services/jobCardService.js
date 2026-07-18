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

    }

};