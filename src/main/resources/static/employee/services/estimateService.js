window.EstimateService = {

    async createEstimate(request) {

        return await Api.post(

            "/estimates",

            request

        );

    },

    async addEstimateItem(
        estimateId,
        request
    ) {

        return await Api.post(

            `/estimates/${estimateId}/items`,

            request

        );

    },

    async finishEstimate(estimateId, payload) {

        return await Api.put(
            `/estimates/${estimateId}`,
            payload
        );

    },

    async approveEstimate(jobCardNumber) {

        return await Api.post(
            `/workflow/${jobCardNumber}/estimate/approve`
        );

    },
    async updateEstimate(id, request) {

        return await Api.put(
            `/estimates/${id}`,
            request
        );

    },

//    async getEstimate(id) {
//        return await Api.get(`/estimates/${id}`);
//    }
//
//    async rejectEstimate(id) {
//        return await Api.put(`/estimates/${id}/reject`);
//    }
};