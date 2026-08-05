window.UserService = {

    async getTechnicians() {

        return await Api.get(
            "/users/technicians"
        );

    }

};