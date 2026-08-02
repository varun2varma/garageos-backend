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

    },

     async joinGarage(request) {

         try {

             return await Api.post(

                 "/garage-memberships/join",

                 request

             );

         } catch (e) {

             console.error(

                 "Join Garage Failed",

                 e

             );

             throw e;

         }

     },

     async getPendingMemberships() {

         try {

             return await Api.get(

                 "/garage-memberships/pending"

             );

         } catch (e) {

             console.error(e);

             throw e;

         }

     },

     async approveMembership(
         membershipId,
         request
     ) {

         try {

             return await Api.put(

                 `/garage-memberships/${membershipId}/approve`,

                 request

             );

         } catch (e) {

             console.error(e);

             throw e;

         }

     },

     async rejectMembership(
         membershipId,
         remarks
     ) {

         try {

             return await Api.put(

                 `/garage-memberships/${membershipId}/reject?remarks=${encodeURIComponent(remarks)}`,

                 {}

             );

         } catch (e) {

             console.error(e);

             throw e;

         }

     },

     async removeMembership(
         membershipId
     ) {

         try {

             return await Api.delete(

                 `/garage-memberships/${membershipId}`

             );

         } catch (e) {

             console.error(e);

             throw e;

         }

     }

};