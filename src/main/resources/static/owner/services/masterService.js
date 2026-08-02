window.MasterService = {

    async getEmployeeRoles() {

        return await Api.get(

            "/master/employee-roles"

        );

    }

};