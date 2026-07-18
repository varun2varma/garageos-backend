window.EstimateItemService = {

    async addItem(estimateId, request) {

        return await Api.post(

            `/estimates/${estimateId}/items`,

            request

        );

    },

    async getItems(estimateId) {

        return await Api.get(

            `/estimates/${estimateId}/items`

        );

    },

    async updateItem(id, request) {

        return await Api.put(

            `/estimate-items/${id}`,

            request

        );

    },

    async deleteItem(id) {

        return Api.delete(

            `/estimate-items/${id}`

        );

    }

};