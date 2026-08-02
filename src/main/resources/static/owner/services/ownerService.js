window.OwnerService={

    async getPendingEmployees(){

        return await Api.get(

            "/garage-memberships/pending"

        );

    },

    async getEmployees(){

        return await Api.get(

            "/garage-memberships/employees"

        );

    },

    async approveEmployee(

        membershipId,

        request

    ){

        return await Api.put(

            `/garage-memberships/${membershipId}/approve`,

            request

        );

    },

    async rejectEmployee(

        membershipId,

        remarks

    ){

        return await Api.put(

            `/garage-memberships/${membershipId}/reject?remarks=${encodeURIComponent(remarks)}`,

            {}

        );

    },

    async removeEmployee(

        membershipId

    ){

        return await Api.delete(

            `/garage-memberships/${membershipId}`

        );

    },

    async updateEmployeeRole(

        employeeId,

        roleId

    ){

        return await Api.put(

            `/garage-memberships/employees/${employeeId}/role`,

            {

                roleId

            }

        );

    },

    async getSummary() {
        return await Api.get("/dashboard/summary");
    },

    async getRecentJobs() {
        return await Api.get("/dashboard/recent-jobs");
    }

};