window.JobAssignmentService = {

    /*
    -----------------------------------------
    Manager
    -----------------------------------------
    */

    async getByJobCard(jobCardId) {

        return await Api.get(
            `/job-assignments/job-card/${jobCardId}`
        );

    },

    async assign(request) {

        return await Api.post(
            "/job-assignments",
            request
        );

    },

    async reassign(id, request) {

        return await Api.put(
            `/job-assignments/${id}/reassign`,
            request
        );

    },

    /*
    -----------------------------------------
    Technician
    -----------------------------------------
    */

    async myAssignments() {

        return await Api.get(
            "/job-assignments/my"
        );

    },

    async accept(id) {

        return await Api.put(
            `/job-assignments/${id}/accept`
        );

    },

    async start(id, request = {}) {

        return await Api.put(
            `/job-assignments/${id}/start`,
            request
        );

    },

    async complete(id, request = {}) {

        return await Api.put(
            `/job-assignments/${id}/complete`,
            request
        );

    }

};