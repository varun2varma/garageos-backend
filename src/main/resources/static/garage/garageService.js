window.GarageService = {

    async createGarage(request) {

        try {

            const response =
                await Api.post(

                    "/garages",

                    request

                );

            return response;

        } catch (e) {

            console.error(

                "Create Garage Failed",

                e

            );

            throw e;

        }

    },

    async getGarage() {

        try {

            return await Api.get(

                "/garages/me"

            );

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async updateGarage(id, request) {

        try {

            return await Api.put(

                `/garages/${id}`,

                request

            );

        } catch (e) {

            console.error(e);

            throw e;

        }

    }

};