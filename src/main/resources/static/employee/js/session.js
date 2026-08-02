/*
==========================================================
 GarageOS Employee Session
==========================================================
*/

window.EmployeeSession = {

    user() {

        return Auth.getUser() ?? {};

    },

    id() {

        return this.user().id;

    },

    username() {

        return this.user().username;

    },

    firstName() {

        return this.user().firstName ?? "";

    },

    lastName() {

        return this.user().lastName ?? "";

    },

    fullName() {

        return (

            this.firstName() +

            " " +

            this.lastName()

        ).trim();

    },

    garageId() {

        return this.user().garageId;

    },

    garageCode() {

        return this.user().garageCode;

    },

    garageName() {

        return this.user().garageName;

    },

    employeeCode() {

        return this.user().employeeCode;

    },

    roles() {

        return this.user().roles ?? [];

    }

};