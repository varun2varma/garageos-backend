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

    }

};